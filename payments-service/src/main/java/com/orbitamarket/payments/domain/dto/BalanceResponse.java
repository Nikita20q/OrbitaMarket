package com.orbitamarket.payments.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class BalanceResponse {
    private String userId;
    private BigDecimal balance;
    private String currency;
}
