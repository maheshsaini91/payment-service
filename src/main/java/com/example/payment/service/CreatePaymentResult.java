package com.example.payment.service;

import com.example.payment.model.Payment;

/**
 * Result of {@link PaymentService#createPayment}.
 *
 * Carries the payment together with a flag that tells the caller whether this
 * was a fresh creation or an idempotent replay of a previously processed request.
 * This avoids a second round-trip to the repository just to determine the HTTP
 * status code in the controller.
 */
public record CreatePaymentResult(Payment payment, boolean isReplay) {

    public static CreatePaymentResult fresh(Payment payment) {
        return new CreatePaymentResult(payment, false);
    }

    public static CreatePaymentResult replay(Payment payment) {
        return new CreatePaymentResult(payment, true);
    }
}
