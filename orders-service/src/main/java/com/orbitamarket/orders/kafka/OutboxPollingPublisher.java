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
    private static final int MAX_FAST_RETRIES = 5;

    @Scheduled(fixedDelay = 1000)
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

    @Scheduled(fixedDelay = 1_200_000)
    public void longRetry() {
        List<OrderOutbox> records = outboxRepository.findByStatus(OutboxStatus.LONG_RETRY);

        if (records.isEmpty()) {
            return;
        }

        log.info("Long retry: found {} records for retry", records.size());

        for (OrderOutbox record : records) {
            processRecord(record);
        }
    }

    @Transactional
    public void processRecord(OrderOutbox record) {
        try {
            OrderPaymentRequested event = objectMapper.readValue(
                    record.getPayload(), OrderPaymentRequested.class);

            kafkaTemplate.send("order-payment-requested",
                    event.getOrderId().toString(), event).get();

            markAsSent(record);
            log.info("Outbox record {} sent to Kafka successfully", record.getEventId());

        } catch (Exception e) {
            markAsFailed(record, e.getMessage());
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
        record.setProcessedAt(LocalDateTime.now());

        if (record.getRetryCount() > MAX_FAST_RETRIES) {
            record.setStatus(OutboxStatus.LONG_RETRY);
            log.warn("Outbox record {} switched to long retry mode after {} fast retries",
                    record.getEventId(), record.getRetryCount());
        }

        outboxRepository.save(record);
    }
}
