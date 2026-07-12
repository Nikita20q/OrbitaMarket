package com.orbitamarket.orders.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orbitamarket.orders.domain.dto.OrderRequest;
import com.orbitamarket.orders.domain.dto.OrderResponse;
import com.orbitamarket.orders.domain.entity.Order;
import com.orbitamarket.orders.domain.entity.OrderOutbox;
import com.orbitamarket.orders.domain.enums.OrderStatus;
import com.orbitamarket.orders.domain.enums.OutboxStatus;
import com.orbitamarket.orders.domain.event.OrderPaymentRequested;
import com.orbitamarket.orders.repository.OrderOutboxRepository;
import com.orbitamarket.orders.repository.OrderRepository;
import com.orbitamarket.orders.validation.PayloadValidatorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderOutboxRepository orderOutboxRepository;
    private final ObjectMapper objectMapper;
    private final PayloadValidatorFactory validatorFactory;

    @Transactional
    public OrderResponse createOrder(UUID userId, OrderRequest orderRequest) {
        var validator = validatorFactory.getValidator(orderRequest.getProductType());
        validator.validate(orderRequest.getPayload());

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(orderRequest.getPayload());
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize payload: " + e.getMessage());
        }

        Order order = Order.builder()
                .userId(userId)
                .productType(orderRequest.getProductType())
                .orderStatus(OrderStatus.PAYMENT_PENDING)
                .price(orderRequest.getPrice())
                .payload(payloadJson)
                .build();

        order = orderRepository.save(order);

        UUID eventId = UUID.randomUUID();
        OrderPaymentRequested event = OrderPaymentRequested.builder()
                .eventId(eventId)
                .orderId(order.getId())
                .userId(userId)
                .amount(order.getPrice())
                .occurredAt(LocalDateTime.now())
                .build();

        String eventJson;
        try {
            eventJson = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize event", e);
        }

        OrderOutbox outbox = OrderOutbox.builder()
                .eventId(eventId)
                .eventType("ORDER_PAYMENT_REQUESTED")
                .aggregateId(order.getId())
                .payload(eventJson)
                .status(OutboxStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        log.info("Order {} created and event {} saved to outbox", order.getId(), eventId);
        orderOutboxRepository.save(outbox);

        return OrderResponse.builder()
                .orderId(order.getId())
                .orderStatus(order.getOrderStatus())
                .productType(order.getProductType())
                .price(order.getPrice())
                .createdAt(order.getCreatedAt())
                .build();
    }
}