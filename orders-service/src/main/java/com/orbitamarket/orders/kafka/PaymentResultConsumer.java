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

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentResultConsumer {
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-payment-completed", groupId = "orders-service")
    public void handlePaymentCompleted(String message) {
        try {
            log.info("Received payment completed message: {}", message);

            OrderPaymentCompleted event = objectMapper.readValue(message, OrderPaymentCompleted.class);

            log.info("Parsed event: orderId={}, newBalance={}", event.getOrderId(), event.getNewBalance());

            Optional<Order> orderOptional = orderRepository.findById(event.getOrderId());

            if (orderOptional.isPresent()) {
                Order order = orderOptional.get();
                order.setOrderStatus(OrderStatus.PAID);
                orderRepository.save(order);
                log.info("Order {} status updated to PAID", event.getOrderId());
            } else {
                log.warn("Order {} not found", event.getOrderId());
            }
        } catch (Exception e) {
            log.error("Error processing payment completed message: {}", message, e);
        }
    }

    @KafkaListener(topics = "order-payment-failed", groupId = "orders-service")
    public void handlePaymentFailed(String message) {
        try {
            log.info("Received payment failed message: {}", message);

            OrderPaymentFailed event = objectMapper.readValue(message, OrderPaymentFailed.class);

            log.info("Parsed event: orderId={}, reason={}", event.getOrderId(), event.getReason());

            Optional<Order> orderOptional = orderRepository.findById(event.getOrderId());

            if (orderOptional.isPresent()) {
                Order order = orderOptional.get();
                order.setOrderStatus(OrderStatus.PAYMENT_FAILED);
                order.setFailureReason(event.getReason());
                orderRepository.save(order);
                log.info("Order {} status updated to PAYMENT_FAILED: {}",
                        event.getOrderId(), event.getReason());
            } else {
                log.warn("Order {} not found", event.getOrderId());
            }
        } catch (Exception e) {
            log.error("Error processing payment failed message: {}", message, e);
        }
    }
}