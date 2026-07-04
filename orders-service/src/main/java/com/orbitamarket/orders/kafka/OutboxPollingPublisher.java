package com.orbitamarket.orders.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orbitamarket.orders.domain.entity.OrderOutbox;
import com.orbitamarket.orders.domain.enums.OutboxStatus;
import com.orbitamarket.orders.domain.event.OrderPaymentRequested;
import com.orbitamarket.orders.repository.OrderOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPollingPublisher {
    private final OrderOutboxRepository outboxRepository;
    private final KafkaTemplate<String, OrderPaymentRequested> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    public void pollAndPublish() {
        List<OrderOutbox> orderOutboxes = outboxRepository.findByStatus(OutboxStatus.PENDING);

        if (orderOutboxes.isEmpty()) {
            return;
        }

        log.info("Found {} pending outbox records", orderOutboxes.size());

        for (OrderOutbox record : orderOutboxes) {
            processRecord(record);
        }
    }

    @Transactional
    public void  processRecord(OrderOutbox record) {
        try {
            OrderPaymentRequested event = objectMapper.readValue(record.getPayload(), OrderPaymentRequested.class);
            kafkaTemplate.send("order-payment-requested",
                    event.getOrderId().toString(),
            event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            markAsSent(record);
                            log.info("Outbox record {} sent to Kafka successfully", record.getEventId());
                        } else {
                            markAsFailed(record, ex.getMessage());
                            log.error("Failed to send outbox record {}: {}",
                                    record.getEventId(), ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            markAsFailed(record, e.getMessage());
            log.error("Failed to process outbox record {}: {}", record.getEventId(), e.getMessage());
        }
    }

    private void markAsSent(OrderOutbox record) {
        record.setStatus(OutboxStatus.SENT);
        record.setProcessedAt(LocalDateTime.now());
        outboxRepository.save(record);
    }

    private void markAsFailed(OrderOutbox record, String message) {
        record.setRetryCount(record.getRetryCount() + 1);
        record.setErrorMessage(message);

        if (record.getRetryCount() > 5) {
            record.setStatus(OutboxStatus.FAILED);
            log.error("Outbox record {} failed after {} retries",
                    record.getEventId(), record.getRetryCount());
        }

        outboxRepository.save(record);
    }
}
