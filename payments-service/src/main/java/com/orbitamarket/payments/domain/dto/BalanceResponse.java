package com.orbitamarket.payments.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
public class BalanceResponse {
    private UUID userId;
    private BigDecimal balance;
    private String currency;
}
