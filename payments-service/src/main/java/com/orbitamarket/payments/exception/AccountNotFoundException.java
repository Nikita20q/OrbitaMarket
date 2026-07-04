package com.orbitamarket.payments.exception;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String userId) {
        super("Account with id " + userId + " not found");
    }
}
