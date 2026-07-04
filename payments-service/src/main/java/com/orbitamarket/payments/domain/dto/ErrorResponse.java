package com.orbitamarket.payments.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private String errorCode;
    private String errorMessage;
    private LocalDateTime timestamp;
    private String errorDetail;

    public static ErrorResponse of(String errorCode, String errorMessage) {
        return new ErrorResponse(errorCode, errorMessage, LocalDateTime.now(), null);
    }
}