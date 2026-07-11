package com.orbitamarket.orders.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orbitamarket.orders.domain.entity.Order;
import com.orbitamarket.orders.domain.enums.OrderStatus;
import com.orbitamarket.orders.domain.event.OrderPaymentCompleted;
import com.orbitamarket.orders.domain.event.OrderPaymentFailed;
import com.orbitamarket.orders.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentResultConsumer {
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-payment-completed", groupId = "orders-service")
    @Transactional
    public void handlePaymentCompleted(String message) {
        try {
            log.info("Received payment completed message: {}", message);

            OrderPaymentCompleted event = objectMapper.readValue(message, OrderPaymentCompleted.class);

            Optional<Order> orderOptional = orderRepository.findById(event.getOrderId());

            if (orderOptional.isEmpty()) {
                log.warn("Order {} not found", event.getOrderId());
                return;
            }

            Order order = orderOptional.get();

            if (order.getOrderStatus() == OrderStatus.PAID) {
                log.info("Order {} already PAID, skipping duplicate", event.getOrderId());
                return;
            }

            if (order.getOrderStatus() != OrderStatus.PAYMENT_PENDING) {
                log.warn("Order {} has status {}, cannot transition to PAID",
                        event.getOrderId(), order.getOrderStatus());
                return;
            }

            order.setOrderStatus(OrderStatus.PAID);
            orderRepository.save(order);

            log.info("Order {} status updated to PAID", event.getOrderId());
        } catch (Exception e) {
            log.error("Error processing payment completed message: {}", message, e);
        }
    }

    @KafkaListener(topics = "order-payment-failed", groupId = "orders-service")
    @Transactional
    public void handlePaymentFailed(String message) {
        try {
            log.info("Received payment failed message: {}", message);

            OrderPaymentFailed event = objectMapper.readValue(message, OrderPaymentFailed.class);

            Optional<Order> orderOptional = orderRepository.findById(event.getOrderId());

            if (orderOptional.isEmpty()) {
                log.warn("Order {} not found", event.getOrderId());
                return;
            }

            Order order = orderOptional.get();

            if (order.getOrderStatus() == OrderStatus.PAYMENT_FAILED) {
                log.info("Order {} already PAYMENT_FAILED, skipping duplicate", event.getOrderId());
                return;
            }

            if (order.getOrderStatus() != OrderStatus.PAYMENT_PENDING) {
                log.warn("Order {} has status {}, cannot transition to PAYMENT_FAILED",
                        event.getOrderId(), order.getOrderStatus());
                return;
            }

            order.setOrderStatus(OrderStatus.PAYMENT_FAILED);
            order.setFailureReason(event.getReason());
            orderRepository.save(order);

            log.info("Order {} status updated to PAYMENT_FAILED: {}",
                    event.getOrderId(), event.getReason());
        } catch (Exception e) {
            log.error("Error processing payment failed message: {}", message, e);
        }
    }
}