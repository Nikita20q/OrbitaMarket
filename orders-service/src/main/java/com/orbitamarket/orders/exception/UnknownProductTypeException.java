package com.orbitamarket.orders.exception;

public class UnknownProductTypeException extends RuntimeException {
    public UnknownProductTypeException(String message) {
        super(message);
    }
}