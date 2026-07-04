package com.orbitamarket.payments.service;

import com.orbitamarket.payments.domain.entity.Account;
import com.orbitamarket.payments.domain.entity.PaymentInbox;
import com.orbitamarket.payments.domain.event.OrderPaymentCompleted;
import com.orbitamarket.payments.domain.event.OrderPaymentFailed;
import com.orbitamarket.payments.domain.event.OrderPaymentRequested;
import com.orbitamarket.payments.kafka.PaymentEventProducer;
import com.orbitamarket.payments.repository.AccountRepository;
import com.orbitamarket.payments.repository.PaymentInboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final AccountRepository accountRepository;
    private final PaymentInboxRepository inboxRepository;
    private final PaymentEventProducer paymentEventProducer;

    @Transactional
    public void processPaymentRequest(OrderPaymentRequested event) {
        if (inboxRepository.existsById(event.getEventId())) {
            log.warn("Duplicate event detected, skipping: eventId={}, orderId={}",
                    event.getEventId(), event.getOrderId());
            return;
        }
        Account account = accountRepository.findByUserId(event.getUserId())
                .orElse(null);

        if (account == null) {
            log.warn("Account not found for user: {}", event.getUserId());
            OrderPaymentFailed failed = OrderPaymentFailed.builder()
                    .eventId(UUID.randomUUID())
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .reason("ACCOUNT_NOT_FOUND")
                    .amount(event.getAmount())
                    .occurredAt(LocalDateTime.now())
                    .build();
            paymentEventProducer.sendPaymentFailed(failed);

            saveToInbox(event, "ORDER_PAYMENT_REQUESTED");
            return;
        }

        if (account.getBalance().compareTo(event.getAmount()) < 0) {
            log.warn("Insufficient balance for user {}: {} < {}",
                    event.getUserId(), account.getBalance(), event.getAmount());
            OrderPaymentFailed failed = OrderPaymentFailed.builder()
                    .eventId(UUID.randomUUID())
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .reason("INSUFFICIENT_BALANCE")
                    .amount(event.getAmount())
                    .occurredAt(LocalDateTime.now())
                    .build();

            paymentEventProducer.sendPaymentFailed(failed);
            saveToInbox(event, "ORDER_PAYMENT_REQUESTED");
            return;
        }

        account.setBalance(account.getBalance().subtract(event.getAmount()));
        accountRepository.save(account);
        log.info("Payment successful for order {}: {} geocredits debited",
                event.getOrderId(), event.getAmount());

        OrderPaymentCompleted completed = OrderPaymentCompleted.builder()
                .eventId(UUID.randomUUID())
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .amount(event.getAmount())
                .newBalance(account.getBalance())
                .occurredAt(LocalDateTime.now())
                .build();
        paymentEventProducer.sendPaymentCompleted(completed);
        saveToInbox(event, "ORDER_PAYMENT_REQUESTED");
    }

    private void saveToInbox(OrderPaymentRequested event, String eventType) {
        PaymentInbox inbox = PaymentInbox.builder()
                .eventId(event.getEventId())
                .eventType(eventType)
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .amount(event.getAmount())
                .processedAt(LocalDateTime.now())
                .build();
        inboxRepository.save(inbox);
    }
}