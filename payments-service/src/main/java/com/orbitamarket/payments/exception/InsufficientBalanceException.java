package com.orbitamarket.payments.exception;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException() {
        super("Insufficient balance");
    }

    public InsufficientBalanceException(String message) {
        super(message);
    }
}
