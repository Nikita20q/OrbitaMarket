package com.orbitamarket.orders.exception;

import com.orbitamarket.orders.domain.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleOrderNotFoundException(OrderNotFoundException ex) {
        return ErrorResponse.of("ORDER_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(InvalidPayloadException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidPayloadException(InvalidPayloadException ex) {
        return ErrorResponse.of("INVALID_PAYLOAD", ex.getMessage());
    }

    @ExceptionHandler(InvalidPriceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidPriceException(InvalidPriceException ex) {
        return ErrorResponse.of("INVALID_PRICE", ex.getMessage());
    }

    @ExceptionHandler(UnknownProductTypeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUnknownProductTypeException(UnknownProductTypeException ex) {
        return ErrorResponse.of("UNKNOWN_PRODUCT_TYPE", ex.getMessage());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingRequestHeaderException(MissingRequestHeaderException ex) {
        return ErrorResponse.of("MISSING_USER_ID", "X-User-Id header is required");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleException(Exception ex) {
        return ErrorResponse.of("INTERNAL_ERROR", "Unexpected error: " + ex.getMessage());
    }
}