package com.orbitamarket.orders.validation;

import com.orbitamarket.orders.domain.enums.ProductType;
import com.orbitamarket.orders.exception.UnknownProductTypeException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PayloadValidatorFactory {

    private final Map<ProductType, PayloadValidator> validators;

    public PayloadValidatorFactory(List<PayloadValidator> validatorList) {
        this.validators = validatorList.stream()
                .collect(Collectors.toMap(
                        PayloadValidator::getProductType,
                        Function.identity()
                ));
    }

    public PayloadValidator getValidator(ProductType productType) {
        PayloadValidator validator = validators.get(productType);

        if (validator == null) {
            throw new UnknownProductTypeException(
                    "Unsupported product type: " + productType +
                            ". Supported types: " + validators.keySet()
            );
        }

        return validator;
    }
}