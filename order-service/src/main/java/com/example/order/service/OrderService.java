package com.example.order.service;

import com.example.order.async.domain.AsyncMessage;
import com.example.order.async.domain.AsyncMessageId;
import com.example.order.async.service.AsyncMessageService;
import com.example.order.client.PaymentServiceClient;
import com.example.order.config.KafkaTopicsConfig;
import com.example.order.domain.Money;
import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;
import com.example.order.dto.DeliveryRequest;
import com.example.order.dto.PaymentCardDto;
import com.example.order.dto.PaymentRequest;
import com.example.order.dto.PaymentResponse;
import com.example.order.messaging.PaymentMessagePublisher;
import com.example.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private static final String PAYMENT_METHOD_CREDIT_CARD = "CREDIT_CARD";

    private final OrderRepository orderRepository;
    private final PaymentServiceClient paymentServiceClient;
    private final PaymentMessagePublisher paymentMessagePublisher;
    private final AsyncMessageService asyncMessageService;
    private final ObjectMapper objectMapper;

    @Transactional
    public Order createOrder(Order order) {
        try {
            log.info("Starting order creation process");

            order.setOrderNumber(generateOrderNumber());
            order.setOrderDate(LocalDateTime.now());
            order.setStatus(OrderStatus.CREATED);

            Money total = calculateTotalAmount(order);
            order.setTotalAmount(total);

            Order savedOrder = orderRepository.save(order);
            log.info("Order created successfully: {}", savedOrder.getOrderNumber());
            return savedOrder;

        } catch (Exception e) {
            log.error("Error creating order: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create order: " + e.getMessage(), e);
        }
    }

    /**
     * Обработка заказа с отправкой запроса оплаты через RabbitMQ.
     * Возвращает заказ в статусе PAYMENT_PENDING — результат оплаты придёт асинхронно.
     */
    @Transactional
    public Order processOrderWithPayment(UUID orderId, PaymentCardDto cardDetails, String idempotencyKey) {
        log.info("Starting payment processing for order ID: {}", orderId);

        Order order = getOrder(orderId);
        log.debug("Order retrieved: {}", order.getOrderNumber());

        order.setStatus(OrderStatus.PAYMENT_PENDING);
        order = orderRepository.save(order);
        log.info("Order {} status updated to PAYMENT_PENDING", order.getOrderNumber());

        PaymentRequest paymentRequest = buildPaymentRequest(order, cardDetails);
        // rabbit
        sendPaymentRequestWithStatusHandling(paymentRequest, order, idempotencyKey);
        // kafka
        sendDeliveryRequestWithStatusHandling(order);

        return order;
    }

    /**
     * Публикация запроса на оплату в RabbitMQ (заменяет синхронный Feign-вызов)
     */
    public void sendPaymentRequestWithStatusHandling(PaymentRequest paymentRequest, Order order, String idempotencyKey) {
        try {
            paymentMessagePublisher.sendPaymentRequest(paymentRequest, idempotencyKey);
        } catch (Exception e) {
            log.error("Failed to publish payment request for order {}: {}", order.getOrderNumber(), e.getMessage(), e);
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            throw new RuntimeException("Failed to send payment request: " + e.getMessage(), e);
        }
    }

    public void sendDeliveryRequestWithStatusHandling(Order order) {
        try {
            DeliveryRequest deliveryRequest = DeliveryRequest.builder()
                    .orderId(order.getId().toString())
                    .orderNumber(order.getOrderNumber())
                    .status(order.getStatus().name())
                    .build();

            var message = new AsyncMessage(
                    new AsyncMessageId(UUID.randomUUID().toString(), KafkaTopicsConfig.DELIVERY_REQUEST_TOPIC),
                    objectMapper.writeValueAsString(deliveryRequest),
                    AsyncMessage.Status.CREATED);
            log.info("try saveMessage {}", message);
            asyncMessageService.saveMessage(message);

        } catch (Exception e) {
            log.error("Failed to publish delivery request for order {}: {}", order.getOrderNumber(), e.getMessage(), e);
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            throw new RuntimeException("Failed to send delivery request: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Order getOrder(UUID id) {
        try {
            log.debug("Fetching order with ID: {}", id);
            return orderRepository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("Order not found with ID: {}", id);
                        return new RuntimeException("Order not found with ID: " + id);
                    });
        } catch (Exception e) {
            log.error("Error fetching order {}: {}", id, e.getMessage());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        try {
            log.debug("Fetching all orders");
            List<Order> orders = orderRepository.findAll();
            log.info("Retrieved {} orders", orders.size());
            return orders;
        } catch (Exception e) {
            log.error("Error fetching all orders: {}", e.getMessage());
            throw new RuntimeException("Failed to retrieve orders", e);
        }
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByCustomer(Long customerId) {
        try {
            log.debug("Fetching orders for customer ID: {}", customerId);
            List<Order> orders = orderRepository.findByCustomerId(customerId);
            log.info("Retrieved {} orders for customer {}", orders.size(), customerId);
            return orders;
        } catch (Exception e) {
            log.error("Error fetching orders for customer {}: {}", customerId, e.getMessage());
            throw new RuntimeException("Failed to retrieve orders for customer", e);
        }
    }

    @Transactional
    public Order updateOrderStatus(UUID id, OrderStatus status) {
        try {
            log.info("Updating order {} status to: {}", id, status);
            Order order = getOrder(id);
            OrderStatus oldStatus = order.getStatus();
            order.setStatus(status);
            Order updatedOrder = orderRepository.save(order);
            log.info("Order {} status updated from {} to {}",
                    order.getOrderNumber(), oldStatus, status);
            return updatedOrder;
        } catch (Exception e) {
            log.error("Error updating order {} status: {}", id, e.getMessage());
            throw new RuntimeException("Failed to update order status", e);
        }
    }

    @Transactional
    public void deleteOrder(UUID id) {
        try {
            log.info("Deleting order with ID: {}", id);
            Order order = getOrder(id);
            orderRepository.delete(order);
            log.info("Order {} deleted successfully", order.getOrderNumber());
        } catch (Exception e) {
            log.error("Error deleting order {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to delete order", e);
        }
    }

    @Transactional
    public Order updatePaymentInfo(UUID id, String paymentId) {
        try {
            log.info("Updating payment info for order {}: paymentId={}", id, paymentId);
            Order order = getOrder(id);
            order.setPaymentId(paymentId);
            order.setStatus(OrderStatus.PAID);
            Order updatedOrder = orderRepository.save(order);
            log.info("Payment info updated for order {}", order.getOrderNumber());
            return updatedOrder;
        } catch (Exception e) {
            log.error("Error updating payment info for order {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to update payment information", e);
        }
    }

    @Transactional
    public Order updateDeliveryInfo(UUID id, String deliveryId) {
        try {
            log.info("Updating delivery info for order {}: deliveryId={}", id, deliveryId);
            Order order = getOrder(id);
            order.setDeliveryId(deliveryId);
            order.setStatus(OrderStatus.SHIPPED);
            Order updatedOrder = orderRepository.save(order);
            log.info("Delivery info updated for order {}", order.getOrderNumber());
            return updatedOrder;
        } catch (Exception e) {
            log.error("Error updating delivery info for order {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to update delivery information", e);
        }
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentInfoForOrder(UUID orderId) {
        try {
            log.info("Fetching payment info for order: {}", orderId);
            Order order = getOrder(orderId);

            if (order.getPaymentId() == null) {
                log.warn("No payment found for order: {}", order.getOrderNumber());
                throw new RuntimeException("No payment found for order: " + order.getOrderNumber());
            }

            PaymentResponse payment = paymentServiceClient.getPayment(UUID.fromString(order.getPaymentId()));
            log.info("Payment info retrieved for order {}", order.getOrderNumber());
            return payment;

        } catch (FeignException e) {
            log.error("Error fetching payment info from payment service: Status={}, Message={}",
                    e.status(), e.getMessage());

            if (e.status() == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                throw new RuntimeException("Payment service is currently unavailable. Please try again later.", e);
            } else if (e.status() == HttpStatus.NOT_FOUND.value()) {
                throw new RuntimeException("Payment not found for this order.", e);
            } else {
                throw new RuntimeException("Failed to retrieve payment information: " + e.getMessage(), e);
            }

        } catch (Exception e) {
            log.error("Error getting payment info for order {}: {}", orderId, e.getMessage());
            throw e;
        }
    }

    private Money calculateTotalAmount(Order order) {
        try {
            Money total = order.getItems().stream()
                    .map(item -> {
                        BigDecimal itemTotal = item.getPrice().getAmount()
                                .multiply(BigDecimal.valueOf(item.getQuantity()));
                        return new Money(itemTotal, item.getPrice().getCurrency());
                    })
                    .reduce(new Money(BigDecimal.ZERO, Currency.getInstance("USD")),
                            (acc, money) -> {
                                try {
                                    return acc.add(money);
                                } catch (Exception e) {
                                    log.error("Error adding money amounts: {}", e.getMessage());
                                    throw new RuntimeException("Failed to calculate total amount", e);
                                }
                            });

            log.debug("Total amount calculated: {} {}", total.getAmount(), total.getCurrency());
            return total;
        } catch (Exception e) {
            log.error("Error calculating total amount: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate order total", e);
        }
    }

    private PaymentRequest buildPaymentRequest(Order order, PaymentCardDto cardDetails) {
        try {
            return PaymentRequest.builder()
                    .orderId(order.getId().toString())
                    .orderNumber(order.getOrderNumber())
                    .amount(order.getTotalAmount().getAmount())
                    .currency(order.getTotalAmount().getCurrency())
                    .paymentMethod(PAYMENT_METHOD_CREDIT_CARD)
                    .cardDetails(cardDetails)
                    .build();
        } catch (Exception e) {
            log.error("Error building payment request: {}", e.getMessage());
            throw new RuntimeException("Failed to build payment request", e);
        }
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
