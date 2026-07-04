package com.orbitamarket.payments.repository;

import com.orbitamarket.payments.domain.entity.PaymentInbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PaymentInboxRepository extends JpaRepository<PaymentInbox, UUID> {
}
