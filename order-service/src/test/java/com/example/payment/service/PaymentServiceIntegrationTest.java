package com.example.payment.service;

import com.example.order.OrderServiceApplication;
import com.example.order.domain.Money;
import com.example.order.domain.Order;
import com.example.order.domain.OrderItem;
import com.example.order.domain.OrderStatus;
import com.example.order.domain.ShippingAddress;
import com.example.order.dto.PaymentCardDto;
import com.example.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author a.komolov
 * @date 2026-04-22
 */
@SpringBootTest(classes = OrderServiceApplication.class)
@ActiveProfiles("test")
class PaymentServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void testProcessOrderWithPayment_SendsMessageAndReturnsPendingOrder() {
        // Создаём заказ
        Order order = orderService.createOrder(
                new Order(null, null, 22L, null, null,
                        null,
                        new ShippingAddress("st", "mo", "123456", "USA"),
                        List.of(new OrderItem(null, 666L, 1,
                                new Money(BigDecimal.TEN, Currency.getInstance("USD")))),
                        null, null)
        );
        assertNotNull(order.getId());

        // Обрабатываем оплату — сообщение уйдёт в мок RabbitTemplate
        Order result = orderService.processOrderWithPayment(
                order.getId(),
                new PaymentCardDto("4111111111111111", "John Doe", "12/25", "123"),
                "test-idempotency-key"
        );

        assertEquals(OrderStatus.PAYMENT_PENDING, result.getStatus());
        assertNotNull(result.getOrderNumber());
    }
}
