package com.orbitamarket.payments.kafka;

import com.orbitamarket.payments.domain.event.OrderPaymentRequested;
import com.orbitamarket.payments.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final PaymentService paymentService;

    @KafkaListener(topics = "order-payment-requested", groupId = "payments-service")
    public void handlePaymentRequest(OrderPaymentRequested event) {
        log.info("Received payment request for order {}: {}", event.getOrderId(), event);
        paymentService.processPaymentRequest(event);
    }
}