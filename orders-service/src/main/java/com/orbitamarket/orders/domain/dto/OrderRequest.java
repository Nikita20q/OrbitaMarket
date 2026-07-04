package com.orbitamarket.orders.domain.dto;

import com.orbitamarket.orders.domain.enums.ProductType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    private ProductType productType;
    private BigDecimal price;
    private Object payload;
}
