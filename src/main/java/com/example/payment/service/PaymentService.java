package com.example.payment.service;

import com.example.payment.exception.InsufficientBalanceException;
import com.example.payment.kafka.PaymentEventPublisher;
import com.example.payment.ledger.LedgerClient;
import com.example.payment.model.Payment;
import com.example.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final LedgerClient ledgerClient;
    private final PaymentEventPublisher eventPublisher;

    public PaymentService(PaymentRepository paymentRepository,
                          LedgerClient ledgerClient,
                          PaymentEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.ledgerClient = ledgerClient;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Full payment flow:
     * 1. Idempotency check — short-circuit if key already seen
     * 2. Persist with PENDING status
     * 3. Check payer balance via Ledger
     * 4. Execute transfer via Ledger
     * 5. Persist final status (COMPLETED / FAILED)
     * 6. Publish PaymentCompletedEvent on success
     *
     * @param idempotencyKey optional caller-supplied deduplication key; null opts out
     * @return {@link CreatePaymentResult} with the payment and a replay flag
     */
    public CreatePaymentResult createPayment(UUID payerAccountId, UUID payeeAccountId,
                                             BigDecimal amount, String currency,
                                             String description, String idempotencyKey) {

        // Step 1 — idempotency guard
        if (idempotencyKey != null) {
            Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Idempotent replay key={} returning paymentId={}",
                        idempotencyKey, existing.get().getId());
                return CreatePaymentResult.replay(existing.get());
            }
        }

        // Step 2 — persist PENDING
        var payment = new Payment(payerAccountId, payeeAccountId,
                amount, currency, description, idempotencyKey);
        paymentRepository.save(payment);
        log.info("Payment created id={} status=PENDING", payment.getId());

        try {
            // Step 3 — balance check
            var balance = ledgerClient.getBalance(payerAccountId);
            if (balance.availableBalance().compareTo(amount) < 0) {
                payment.updateStatus(Payment.Status.FAILED);
                paymentRepository.save(payment);
                log.warn("Payment id={} FAILED: insufficient balance available={} required={}",
                        payment.getId(), balance.availableBalance(), amount);
                throw new InsufficientBalanceException(
                        "Insufficient balance: available=" + balance.availableBalance()
                        + " required=" + amount);
            }

            // Step 4 — execute transfer
            ledgerClient.executeTransfer(payerAccountId, payeeAccountId, amount, currency);

            // Step 5 — mark COMPLETED
            payment.updateStatus(Payment.Status.COMPLETED);
            paymentRepository.save(payment);
            log.info("Payment id={} COMPLETED", payment.getId());

            // Step 6 — publish event (async; publish failures are logged but not fatal)
            eventPublisher.publishPaymentCompleted(payment);

        } catch (InsufficientBalanceException e) {
            throw e; // already persisted as FAILED
        } catch (Exception e) {
            payment.updateStatus(Payment.Status.FAILED);
            paymentRepository.save(payment);
            log.error("Payment id={} FAILED due to unexpected error", payment.getId(), e);
            throw e;
        }

        return CreatePaymentResult.fresh(payment);
    }

    public Optional<Payment> findById(UUID id) {
        return paymentRepository.findById(id);
    }
}
