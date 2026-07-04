package com.orbitamarket.payments.domain.event;

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
public class OrderPaymentCompleted {
    private UUID eventId;
    private UUID orderId;
    private String userId;
    private BigDecimal amount;
    private BigDecimal newBalance;
    private LocalDateTime occurredAt;
}
