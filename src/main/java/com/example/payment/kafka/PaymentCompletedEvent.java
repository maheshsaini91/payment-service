package com.example.payment.kafka;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published to {@code payment.events} after a successful transfer.
 */
public record PaymentCompletedEvent(
        UUID paymentId,
        UUID payerAccountId,
        UUID payeeAccountId,
        BigDecimal amount,
        String currency,
        Instant completedAt
) {}
