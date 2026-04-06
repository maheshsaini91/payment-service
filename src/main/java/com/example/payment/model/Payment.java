package com.example.payment.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Payment {

    public enum Status {
        PENDING, COMPLETED, FAILED
    }

    private final UUID id;
    private final UUID payerAccountId;
    private final UUID payeeAccountId;
    private final BigDecimal amount;
    private final String currency;
    private final String description;
    /**
     * Caller-supplied idempotency key (e.g. from the Idempotency-Key header).
     * Null means the caller did not opt in to idempotency protection.
     */
    private final String idempotencyKey;
    private Status status;
    private final Instant createdAt;
    private Instant updatedAt;

    public Payment(UUID payerAccountId, UUID payeeAccountId,
                   BigDecimal amount, String currency, String description,
                   String idempotencyKey) {
        this.id = UUID.randomUUID();
        this.payerAccountId = payerAccountId;
        this.payeeAccountId = payeeAccountId;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.idempotencyKey = idempotencyKey;
        this.status = Status.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void updateStatus(Status status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getPayerAccountId() { return payerAccountId; }
    public UUID getPayeeAccountId() { return payeeAccountId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getDescription() { return description; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
