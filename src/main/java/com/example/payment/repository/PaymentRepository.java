package com.example.payment.repository;

import com.example.payment.model.Payment;

import java.util.Optional;
import java.util.UUID;

/**
 * Abstraction over payment persistence.
 * Current: in-memory. Future: jOOQ + PostgreSQL — implement this interface and register the bean.
 */
public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(UUID id);

    /**
     * Used for idempotency: find a previously accepted request by its caller-supplied key.
     * For the in-memory impl this is a linear scan; a DB impl adds a unique index on the column.
     */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
