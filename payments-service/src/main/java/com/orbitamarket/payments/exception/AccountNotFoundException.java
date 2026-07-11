package com.orbitamarket.payments.exception;

import java.util.UUID;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(UUID userId) {
        super("Account with id " + userId + " not found");
    }
}
