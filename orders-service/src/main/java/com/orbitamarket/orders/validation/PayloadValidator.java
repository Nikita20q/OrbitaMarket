package com.orbitamarket.orders.validation;
import com.orbitamarket.orders.domain.enums.ProductType;

public interface PayloadValidator {
    ProductType getProductType();
    void validate(Object payload);
}