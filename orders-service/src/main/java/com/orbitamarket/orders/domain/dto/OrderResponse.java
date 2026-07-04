package com.orbitamarket.orders.domain.dto;

import com.orbitamarket.orders.domain.enums.OrderStatus;
import com.orbitamarket.orders.domain.enums.ProductType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class OrderResponse {
    private UUID orderId;
    private OrderStatus orderStatus;
    private ProductType productType;
    private BigDecimal price;
    private LocalDateTime createdAt;
}
