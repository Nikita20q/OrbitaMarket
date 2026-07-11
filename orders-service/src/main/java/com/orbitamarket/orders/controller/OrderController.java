package com.orbitamarket.orders.controller;

import com.orbitamarket.orders.domain.dto.OrderRequest;
import com.orbitamarket.orders.domain.dto.OrderResponse;
import com.orbitamarket.orders.domain.entity.Order;
import com.orbitamarket.orders.exception.OrderNotFoundException;
import com.orbitamarket.orders.repository.OrderRepository;
import com.orbitamarket.orders.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders API", description = "Управление заказами")
public class OrderController {
    private final OrderService orderService;
    private final OrderRepository orderRepository;


    @PostMapping("/orders")
    @Operation(summary = "Создать заказ")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Заказ создан"),
            @ApiResponse(responseCode = "400", description = "Невалидные данные")
    })
    public ResponseEntity<OrderResponse> createOrder(
            @Parameter(description = "ID пользователя")
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody OrderRequest request
    ) {
        OrderResponse response = orderService.createOrder(userId, request);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/orders")
    @Operation(summary = "Получить список заказов пользователя")
    public ResponseEntity<List<OrderResponse>> getUserOrders(
        @Parameter(description = "ID пользователя")
        @RequestHeader("X-User-Id") UUID userId
    ) {
        List<OrderResponse> orders = orderRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Получить детали заказа")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        if (!order.getUserId().equals(userId)) {
            throw new OrderNotFoundException("Order not found: " + orderId);
        }

        return ResponseEntity.ok(mapToResponse(order));
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .orderStatus(order.getOrderStatus())
                .productType(order.getProductType())
                .price(order.getPrice())
                .createdAt(order.getCreatedAt())
                .failureReason(order.getFailureReason())
                .build();
    }
}
