package com.example.payment.repository;

import com.example.payment.model.Payment;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory repository — no external dependencies needed for local development or unit tests.
 * Replace with a jOOQ/PostgreSQL implementation when a real DB is available.
 * The DB impl should add a unique index on the idempotency_key column.
 */
@Repository
public class InMemoryPaymentRepository implements PaymentRepository {

    private final Map<UUID, Payment> byId = new ConcurrentHashMap<>();
    /** Secondary index: idempotency key → payment id */
    private final Map<String, UUID> byIdempotencyKey = new ConcurrentHashMap<>();

    @Override
    public Payment save(Payment payment) {
        byId.put(payment.getId(), payment);
        if (payment.getIdempotencyKey() != null) {
            byIdempotencyKey.put(payment.getIdempotencyKey(), payment.getId());
        }
        return payment;
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        return Optional.ofNullable(byIdempotencyKey.get(idempotencyKey))
                .flatMap(this::findById);
    }
}
