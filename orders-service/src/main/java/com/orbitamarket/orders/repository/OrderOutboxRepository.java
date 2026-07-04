package com.orbitamarket.orders.repository;

import com.orbitamarket.orders.domain.entity.OrderOutbox;
import com.orbitamarket.orders.domain.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderOutboxRepository extends JpaRepository<OrderOutbox, UUID> {
    List<OrderOutbox> findByStatus(OutboxStatus status);
}
