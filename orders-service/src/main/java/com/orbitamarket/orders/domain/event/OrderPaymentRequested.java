package com.orbitamarket.orders.domain.event;

import com.orbitamarket.orders.domain.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaymentRequested {
    private UUID eventId;
    private UUID orderId;
    private String userId;
    private BigDecimal amount;
    private LocalDateTime occurredAt;
}
