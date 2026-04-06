package com.example.payment.ledger;

import com.example.payment.model.BalanceResponse;
import com.example.payment.model.TransferResponse;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Abstraction over the Ledger HTTP service.
 * Implementations: {@link HttpLedgerClient} (real), stub for tests.
 */
public interface LedgerClient {

    /**
     * Returns the available balance for the given account.
     *
     * @throws LedgerException if the account is not found or the service is unavailable
     */
    BalanceResponse getBalance(UUID accountId);

    /**
     * Executes a transfer between two accounts.
     *
     * @throws LedgerException       if the transfer cannot be executed
     * @throws InvalidAccountException if the recipient account is invalid
     */
    TransferResponse executeTransfer(UUID fromAccountId, UUID toAccountId,
                                     BigDecimal amount, String currency);

    // ── DTOs ────────────────────────────────────────────────────────────────
}
