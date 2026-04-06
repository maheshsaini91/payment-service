package com.example.payment.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull UUID payerAccountId,
        @NotNull UUID payeeAccountId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String currency,
        String description
) {}
