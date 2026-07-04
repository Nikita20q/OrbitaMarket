package com.orbitamarket.payments.kafka;

import com.orbitamarket.payments.domain.event.OrderPaymentCompleted;
import com.orbitamarket.payments.domain.event.OrderPaymentFailed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendPaymentCompleted(OrderPaymentCompleted event) {
        log.info("Sending payment completed for order {}: {}", event.getOrderId(), event);
        kafkaTemplate.send("order-payment-completed", event.getOrderId().toString(), event);
    }

    public void sendPaymentFailed(OrderPaymentFailed event) {
        log.info("Sending payment failed for order {}: {}", event.getOrderId(), event);
        kafkaTemplate.send("order-payment-failed", event.getOrderId().toString(), event);
    }
}