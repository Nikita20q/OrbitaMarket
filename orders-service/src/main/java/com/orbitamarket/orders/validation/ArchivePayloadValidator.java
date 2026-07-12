package com.orbitamarket.orders.validation;

import com.orbitamarket.orders.domain.dto.ArchivePayload;
import com.orbitamarket.orders.domain.enums.ProductType;
import com.orbitamarket.orders.exception.InvalidPayloadException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArchivePayloadValidator implements PayloadValidator {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Override
    public ProductType getProductType() {
        return ProductType.ARCHIVE;
    }

    @Override
    public void validate(Object payload) {
        try {
            ArchivePayload archivePayload = objectMapper.convertValue(payload, ArchivePayload.class);

            var violations = validator.validate(archivePayload);

            if (!violations.isEmpty()) {
                String message = violations.stream()
                        .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("Invalid payload");
                throw new InvalidPayloadException(message);
            }

        } catch (InvalidPayloadException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidPayloadException("Invalid payload structure: " + e.getMessage());
        }
    }
}