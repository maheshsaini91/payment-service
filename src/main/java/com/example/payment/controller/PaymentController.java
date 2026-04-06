package com.example.payment.controller;

import com.example.payment.model.CreatePaymentRequest;
import com.example.payment.model.Payment;
import com.example.payment.model.PaymentResponse;
import com.example.payment.service.CreatePaymentResult;
import com.example.payment.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * POST /api/payments
     *
     * Optional header: {@code Idempotency-Key: <uuid-or-string>}
     * A duplicate request with the same key returns the original payment (HTTP 200)
     * instead of creating a new one — safe to retry on network failures.
     *
     * New payment  → 201 Created  + Location header
     * Idempotent replay → 200 OK  (no side effects repeated)
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey) {

        CreatePaymentResult result = paymentService.createPayment(
                request.payerAccountId(),
                request.payeeAccountId(),
                request.amount(),
                request.currency(),
                request.description(),
                idempotencyKey
        );

        if (result.isReplay()) {
            return ResponseEntity.ok(new PaymentResponse(result.payment()));
        }
        return ResponseEntity
                .created(URI.create("/api/payments/" + result.payment().getId()))
                .body(new PaymentResponse(result.payment()));
    }

    /**
     * GET /api/payments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID id) {
        return paymentService.findById(id)
                .map(p -> ResponseEntity.ok(new PaymentResponse(p)))
                .orElse(ResponseEntity.notFound().build());
    }
}
