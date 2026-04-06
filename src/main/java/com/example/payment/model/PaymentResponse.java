package com.example.payment.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID payerAccountId,
        UUID payeeAccountId,
        BigDecimal amount,
        String currency,
        String description,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public PaymentResponse(Payment p) {
        this(p.getId(), p.getPayerAccountId(), p.getPayeeAccountId(),
                p.getAmount(), p.getCurrency(), p.getDescription(),
                p.getStatus().name(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
