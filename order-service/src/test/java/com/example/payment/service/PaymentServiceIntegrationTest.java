package com.example.payment.service;

import com.example.order.OrderServiceApplication;
import com.example.order.domain.Money;
import com.example.order.domain.Order;
import com.example.order.domain.OrderItem;
import com.example.order.domain.OrderStatus;
import com.example.order.domain.ShippingAddress;
import com.example.order.dto.PaymentCardDto;
import com.example.order.dto.PaymentRequest;
import com.example.order.service.OrderService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

/**
 * @author a.komolov
 * @date 2026-04-22
 */
@SpringBootTest(classes = OrderServiceApplication.class)
@ActiveProfiles("test")
@EnableWireMock(
        @ConfigureWireMock(
                name = "payment-service-mock",
                port = 9999,
                filesUnderClasspath = "wiremock"
        )
)
class PaymentServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Test
    void testProcessOrderWithPayment_Success() {
        var response = orderService.sendPaymentRequestWithStatusHandling(
                new PaymentRequest("1sf", "4324", BigDecimal.TEN,
                        Currency.getInstance("USD"), "CREDIT_CARD",
                        new PaymentCardDto("4111111111111111", "John Doe", "12/25", "123")),
                new Order(UUID.randomUUID(), "4324", 22L, LocalDateTime.now(),
                        OrderStatus.CREATED, new Money(BigDecimal.TEN, Currency.getInstance("USD")),
                        new ShippingAddress("st", "mo", "123456", "USA"),
                        List.of(new OrderItem(UUID.randomUUID(), 666L, 1,
                                new Money(BigDecimal.TEN, Currency.getInstance("USD")))),
                        "4324", "88"), "test-key");
        Assertions.assertEquals("COMPLETED", response.getStatus());
    }
}