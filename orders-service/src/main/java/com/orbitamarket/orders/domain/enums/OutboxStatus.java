package com.orbitamarket.orders.domain.enums;

public enum OutboxStatus {
    PENDING, // Ожидает отправки
    SENT, // Успешно отправлено
    FAILED // Ошибка отправки
}