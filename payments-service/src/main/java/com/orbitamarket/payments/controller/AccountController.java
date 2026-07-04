package com.orbitamarket.payments.controller;

import com.orbitamarket.payments.domain.dto.AccountResponse;
import com.orbitamarket.payments.domain.dto.BalanceResponse;
import com.orbitamarket.payments.domain.dto.TopUpRequest;
import com.orbitamarket.payments.domain.entity.Account;
import com.orbitamarket.payments.exception.AccountNotFoundException;
import com.orbitamarket.payments.exception.InvalidAmountException;
import com.orbitamarket.payments.repository.AccountRepository;
import com.orbitamarket.payments.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments API", description = "Управление счетами и балансом")
public class AccountController {
    private final AccountService accountService;

    @PostMapping("/accounts")
    @Operation(summary = "Создать счёт пользователя")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Счёт создан"),
            @ApiResponse(responseCode = "400", description = "Отсутствует X-User-Id")
    })
    public ResponseEntity<AccountResponse> createAccount(
            @Parameter(description = "ID пользователя")
            @RequestHeader("X-User-Id") String userId
    ) {
        AccountResponse response = accountService.createAccount(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/accounts/top-up")
    @Operation(summary = "Пополнить счёт")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Счёт успешно пополнен"),
            @ApiResponse(responseCode = "400", description = "Невалидная сумма или отсутствует X-User-Id"),
            @ApiResponse(responseCode = "404", description = "Счёт не найден")
    })
    public ResponseEntity<AccountResponse> topUp(
            @Parameter(description = "ID пользователя")
            @RequestHeader("X-User-Id") String userId,
            @RequestBody TopUpRequest request
    ) {

        AccountResponse response = accountService.topUp(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/accounts/balance")
    @Operation(summary = "Получить текущий баланс")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Баланс получен"),
            @ApiResponse(responseCode = "404", description = "Счёт не найден")
    })
    public ResponseEntity<BalanceResponse> getBalance(
            @Parameter(description = "ID пользователя")
            @RequestHeader("X-User-Id") String userId
    ) {
        BalanceResponse response = accountService.getBalance(userId);
        return ResponseEntity.ok(response);
    }
}
