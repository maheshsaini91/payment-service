package com.example.payment.model;

import java.math.BigDecimal;
import java.util.UUID;

public record BalanceResponse(UUID accountId, BigDecimal availableBalance, String currency) {}

