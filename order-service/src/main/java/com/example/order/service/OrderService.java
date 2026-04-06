package com.example.order.service;

import com.example.order.client.PaymentServiceClient;
import com.example.order.domain.*;
import com.example.order.dto.PaymentCardDto;
import com.example.order.dto.PaymentRequest;
import com.example.order.dto.PaymentResponse;
import com.example.order.repository.OrderRepository;
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

    private final OrderRepository orderRepository;
    private final PaymentServiceClient paymentServiceClient;

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

    @Transactional
    public Order processOrderWithPayment(UUID orderId, PaymentCardDto cardDetails) {
        log.info("Starting payment processing for order ID: {}", orderId);
        Order order = null;

        try {
            // Получаем заказ
            order = getOrder(orderId);
            log.debug("Order retrieved: {}", order.getOrderNumber());

            // Проверяем статус заказа
            if (order.getStatus() != OrderStatus.CREATED) {
                String errorMsg = String.format("Order %s is not in CREATED state. Current state: %s",
                        order.getOrderNumber(), order.getStatus());
                log.warn(errorMsg);
                throw new IllegalStateException(errorMsg);
            }

            // Обновляем статус на PAYMENT_PENDING
            order.setStatus(OrderStatus.PAYMENT_PENDING);
            orderRepository.save(order);
            log.info("Order {} status updated to PAYMENT_PENDING", order.getOrderNumber());

            // Создаем запрос на оплату
            PaymentRequest paymentRequest = buildPaymentRequest(order, cardDetails);
            log.debug("Payment request created for order: {}", order.getOrderNumber());

            // Отправляем запрос в payment-service с обработкой статусов
            PaymentResponse paymentResponse = sendPaymentRequestWithStatusHandling(paymentRequest, order);

            // Обрабатываем успешный ответ
            return handleSuccessfulPaymentResponse(order, paymentResponse);

        } catch (FeignException e) {
            // Обработка ошибок Feign (включая 500)
            return handleFeignException(order, orderId, e);

        } catch (IllegalStateException e) {
            // Пробрасываем исключения статуса заказа
            throw e;

        } catch (Exception e) {
            // Обработка других неожиданных ошибок
            log.error("Unexpected error during payment processing for order {}: {}", orderId, e.getMessage(), e);
            if (order != null) {
                updateOrderToFailed(order);
            }
            throw new RuntimeException("Payment processing failed due to unexpected error: " + e.getMessage(), e);
        }
    }

    /**
     * Отправка запроса в payment-service с обработкой статусов ответа
     */
    private PaymentResponse sendPaymentRequestWithStatusHandling(PaymentRequest paymentRequest, Order order) {
        PaymentResponse response = null;

        try {
            log.info("Sending payment request to payment-service for order: {}", paymentRequest.getOrderNumber());

            // Вызываем Feign клиент
            response = paymentServiceClient.processPayment(paymentRequest);

            // Проверяем, что ответ не null
            if (response == null) {
                log.error("Received null response from payment service for order: {}", order.getOrderNumber());
                throw new RuntimeException("Payment service returned null response");
            }

            // Логируем полученный статус
            log.info("Payment service response received - Status: {}, PaymentId: {}, TransactionId: {}",
                    response.getStatus(), response.getPaymentId(), response.getTransactionId());

            // Обработка статуса 200 OK (успешный ответ от сервиса)
            return handlePaymentServiceResponse(response, order);

        } catch (FeignException e) {
            // Обработка ошибок HTTP (4xx, 5xx)
            log.error("Feign exception while calling payment-service: Status={}, Message={}",
                    e.status(), e.getMessage());
            throw e; // Пробрасываем для дальнейшей обработки

        } catch (Exception e) {
            // Обработка других ошибок (таймауты, ошибки сети)
            log.error("Unexpected error calling payment-service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to communicate with payment service: " + e.getMessage(), e);
        }
    }

    /**
     * Обработка успешного ответа от payment-service (HTTP 200)
     */
    private PaymentResponse handlePaymentServiceResponse(PaymentResponse response, Order order) {
        try {
            // Проверяем бизнес-статус платежа
            if ("COMPLETED".equals(response.getStatus())) {
                log.info("Payment successfully completed for order: {}", order.getOrderNumber());
                return response;

            } else if ("FAILED".equals(response.getStatus())) {
                String errorMsg = String.format("Payment failed for order %s: %s",
                        order.getOrderNumber(),
                        response.getErrorMessage() != null ? response.getErrorMessage() : "Unknown error");
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);

            } else if ("PENDING".equals(response.getStatus())) {
                log.warn("Payment is pending for order: {}", order.getOrderNumber());
                throw new RuntimeException("Payment is still pending. Please check status later.");

            } else {
                log.warn("Unknown payment status for order {}: {}", order.getOrderNumber(), response.getStatus());
                throw new RuntimeException("Unknown payment status: " + response.getStatus());
            }

        } catch (Exception e) {
            log.error("Error processing payment service response for order {}: {}", order.getOrderNumber(), e.getMessage());
            throw e;
        }
    }

    /**
     * Обработка успешного платежа и обновление заказа
     */
    private Order handleSuccessfulPaymentResponse(Order order, PaymentResponse paymentResponse) {
        try {
            if (paymentResponse == null) {
                throw new RuntimeException("Payment response is null");
            }

            if ("COMPLETED".equals(paymentResponse.getStatus())) {
                order.setPaymentId(paymentResponse.getPaymentId().toString());
                order.setStatus(OrderStatus.PAID);
                Order updatedOrder = orderRepository.save(order);
                log.info("Payment successful for order: {}. Payment ID: {}, Transaction ID: {}",
                        order.getOrderNumber(), paymentResponse.getPaymentId(), paymentResponse.getTransactionId());
                return updatedOrder;

            } else {
                // Если платеж не успешен, отменяем заказ
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                throw new RuntimeException("Payment failed with status: " + paymentResponse.getStatus());
            }

        } catch (Exception e) {
            log.error("Error handling successful payment response: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Обработка ошибок Feign клиента (включая HTTP 500)
     */
    private Order handleFeignException(Order order, UUID orderId, FeignException e) {
        int statusCode = e.status();
        String errorMessage = e.getMessage();

        log.error("Feign exception occurred - Status: {}, Order: {}, Message: {}",
                statusCode, orderId, errorMessage);

        try {
            // Получаем актуальный заказ, если он не был передан
            if (order == null) {
                order = getOrder(orderId);
            }

            // Обработка различных HTTP статусов
            if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                // HTTP 500 - Internal Server Error
                log.error("Payment service returned 500 Internal Server Error for order: {}", order.getOrderNumber());
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                throw new RuntimeException(
                        "Payment service encountered an internal error. Please try again later or contact support.",
                        e
                );

            } else if (statusCode == HttpStatus.SERVICE_UNAVAILABLE.value()) {
                // HTTP 503 - Service Unavailable
                log.error("Payment service is unavailable (503) for order: {}", order.getOrderNumber());
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                throw new RuntimeException(
                        "Payment service is temporarily unavailable. Please try again later.",
                        e
                );

            } else if (statusCode == HttpStatus.BAD_REQUEST.value()) {
                // HTTP 400 - Bad Request
                log.error("Invalid payment request (400) for order: {}", order.getOrderNumber());
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                throw new RuntimeException(
                        "Invalid payment request. Please check your payment details: " + extractErrorMessage(e),
                        e
                );

            } else if (statusCode == HttpStatus.NOT_FOUND.value()) {
                // HTTP 404 - Not Found
                log.error("Payment service endpoint not found (404) for order: {}", order.getOrderNumber());
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                throw new RuntimeException(
                        "Payment service configuration error. Please contact support.",
                        e
                );

            } else if (statusCode == HttpStatus.REQUEST_TIMEOUT.value()) {
                // HTTP 408 - Request Timeout
                log.error("Payment service request timeout (408) for order: {}", order.getOrderNumber());
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                throw new RuntimeException(
                        "Payment service request timeout. Please try again later.",
                        e
                );

            } else if (statusCode == HttpStatus.TOO_MANY_REQUESTS.value()) {
                // HTTP 429 - Too Many Requests
                log.error("Rate limit exceeded (429) for order: {}", order.getOrderNumber());
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                throw new RuntimeException(
                        "Too many payment requests. Please wait and try again later.",
                        e
                );

            } else if (statusCode >= 500) {
                // Любые другие 5xx ошибки
                log.error("Payment service server error ({}): {}", statusCode, errorMessage);
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                throw new RuntimeException(
                        "Payment service error (HTTP " + statusCode + "). Please try again later.",
                        e
                );

            } else if (statusCode >= 400) {
                // Клиентские ошибки 4xx
                log.error("Payment service client error ({}): {}", statusCode, errorMessage);
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                throw new RuntimeException(
                        "Payment request failed: " + extractErrorMessage(e),
                        e
                );

            } else {
                // Другие ошибки (таймауты, ошибки сети и т.д.)
                log.error("Payment service communication error: {}", errorMessage);
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                throw new RuntimeException(
                        "Failed to communicate with payment service: " + extractErrorMessage(e),
                        e
                );
            }

        } catch (Exception ex) {
            log.error("Error handling Feign exception for order {}: {}", orderId, ex.getMessage());
            throw new RuntimeException("Payment processing failed: " + errorMessage, e);
        }
    }

    /**
     * Обновление заказа в статус FAILED
     */
    private void updateOrderToFailed(Order order) {
        try {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            log.info("Order {} marked as CANCELLED due to payment failure", order.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to update order {} status to CANCELLED: {}", order.getOrderNumber(), e.getMessage());
        }
    }

    /**
     * Извлечение сообщения об ошибке из FeignException
     */
    private String extractErrorMessage(FeignException e) {
        try {
            if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                return e.getMessage();
            }
            if (e.contentUTF8() != null && !e.contentUTF8().isEmpty()) {
                return e.contentUTF8();
            }
            return "Unknown error";
        } catch (Exception ex) {
            return "Error extracting error message";
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
                    .paymentMethod("CREDIT_CARD")
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
