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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
            saveToInbox(event, "ORDER_PAYMENT_REQUESTED");

            OrderPaymentFailed failed = OrderPaymentFailed.builder()
                    .eventId(UUID.randomUUID())
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .reason("ACCOUNT_NOT_FOUND")
                    .amount(event.getAmount())
                    .occurredAt(LocalDateTime.now())
                    .build();

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            paymentEventProducer.sendPaymentFailed(failed);
                        }
                    }
            );
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

            saveToInbox(event, "ORDER_PAYMENT_REQUESTED");

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            paymentEventProducer.sendPaymentFailed(failed);
                        }
                    }
            );
            return;
        }

        account.setBalance(account.getBalance().subtract(event.getAmount()));
        accountRepository.save(account);

        saveToInbox(event, "ORDER_PAYMENT_REQUESTED");

        OrderPaymentCompleted completed = OrderPaymentCompleted.builder()
                .eventId(UUID.randomUUID())
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .amount(event.getAmount())
                .newBalance(account.getBalance())
                .occurredAt(LocalDateTime.now())
                .build();

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        paymentEventProducer.sendPaymentCompleted(completed);
                    }
                }
        );

        log.info("Payment successful for order {}", event.getOrderId());
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