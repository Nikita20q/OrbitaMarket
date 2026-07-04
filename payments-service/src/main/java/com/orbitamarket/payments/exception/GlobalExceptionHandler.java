package com.orbitamarket.payments.exception;

import com.orbitamarket.payments.domain.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleAccountNotFoundException(AccountNotFoundException ex) {
        return ErrorResponse.of("ACCOUNT_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInsufficientBalanceException(InsufficientBalanceException ex) {
        return ErrorResponse.of("INSUFFICIENT_BALANCE", ex.getMessage());
    }

    @ExceptionHandler(InvalidAmountException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidAmountException(InvalidAmountException ex) {
        return ErrorResponse.of("INVALID_AMOUNT", ex.getMessage());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingRequestHeaderException(MissingRequestHeaderException ex) {
        return ErrorResponse.of("MISSING_USER_ID", "X-User-Id header is required");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleException(Exception ex) {
        return ErrorResponse.of("INTERNAL_ERROR", "Unexpected error");
    }
}
