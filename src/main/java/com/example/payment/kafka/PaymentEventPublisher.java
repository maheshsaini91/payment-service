package com.example.payment.kafka;

import com.example.payment.model.Payment;

/**
 * Abstraction over event publishing.
 * Current implementation: Spring Kafka.
 * Can be swapped for a no-op stub in tests or replaced with a different broker.
 */
public interface PaymentEventPublisher {

    void publishPaymentCompleted(Payment payment);
}
