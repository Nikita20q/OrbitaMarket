package com.orbitamarket.payments.service;

import com.orbitamarket.payments.domain.dto.AccountResponse;
import com.orbitamarket.payments.domain.dto.BalanceResponse;
import com.orbitamarket.payments.domain.dto.TopUpRequest;
import com.orbitamarket.payments.domain.entity.Account;
import com.orbitamarket.payments.exception.AccountNotFoundException;
import com.orbitamarket.payments.exception.InvalidAmountException;
import com.orbitamarket.payments.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {
    private final AccountRepository accountRepository;

    @Transactional
    public AccountResponse createAccount(UUID userId) {
        return accountRepository.findByUserId(userId)
                .map(account -> {
                    log.info("Account already exists for user: {}", userId);
                    return mapToResponse(account);
                })
                .orElseGet(() -> {
                    Account account = Account.builder()
                            .userId(userId)
                            .balance(BigDecimal.ZERO)
                            .build();
                    account = accountRepository.save(account);
                    log.info("Created new account for user: {}", userId);
                    return mapToResponse(account);
                });
    }

    @Transactional
    public AccountResponse topUp(UUID userId, TopUpRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero");
        }

        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException(userId));

        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        log.info("Account {} topped up by {}", userId, request.getAmount());
        return mapToResponse(account);
    }

    public BalanceResponse getBalance(UUID userId) {
        Account account = accountRepository.findByUserId(userId).orElseThrow(() -> new AccountNotFoundException(userId));

        return new BalanceResponse(account.getUserId(), account.getBalance(), "geocredits");
    }

    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .userId(account.getUserId())
                .balance(account.getBalance())
                .build();
    }
}
