//package com.example.payment;
//
//import com.example.payment.exception.InsufficientBalanceException;
//import com.example.payment.kafka.PaymentEventPublisher;
//import com.example.payment.ledger.InvalidAccountException;
//import com.example.payment.ledger.LedgerClient;
//import com.example.payment.ledger.LedgerException;
//import com.example.payment.model.Payment;
//import com.example.payment.repository.InMemoryPaymentRepository;
//import com.example.payment.service.CreatePaymentResult;
//import com.example.payment.service.PaymentService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import java.math.BigDecimal;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//class PaymentServiceTest {
//
//    private RecordingRepository repository;
//    private LedgerClient ledgerClient;
//    private PaymentEventPublisher eventPublisher;
//    private PaymentService service;
//
//    private final UUID payer = UUID.randomUUID();
//    private final UUID payee = UUID.randomUUID();
//    private final BigDecimal amount = new BigDecimal("150.00");
//
//    @BeforeEach
//    void setUp() {
//        repository = new RecordingRepository();
//        ledgerClient = mock(LedgerClient.class);
//        eventPublisher = mock(PaymentEventPublisher.class);
//        service = new PaymentService(repository, ledgerClient, eventPublisher);
//    }
//
//    // ── Happy path ───────────────────────────────────────────────────────────
//
//    @Test
//    void createPayment_completedWhenSufficientBalance() {
//        givenBalance(new BigDecimal("500.00"));
//        givenTransferSucceeds();
//
//        CreatePaymentResult result = service.createPayment(payer, payee, amount, "AED", "rent", null);
//
//        assertThat(result.isReplay()).isFalse();
//        assertThat(result.payment().getStatus()).isEqualTo(Payment.Status.COMPLETED);
//        assertThat(repository.findById(result.payment().getId()))
//                .isPresent().get()
//                .extracting(Payment::getStatus).isEqualTo(Payment.Status.COMPLETED);
//        verify(eventPublisher, times(1)).publishPaymentCompleted(result.payment());
//    }
//
//    @Test
//    void createPayment_firstSaveIsPending() {
//        givenBalance(new BigDecimal("500.00"));
//        givenTransferSucceeds();
//
//        service.createPayment(payer, payee, amount, "AED", "rent", null);
//
//        assertThat(repository.savedStatuses().get(0)).isEqualTo(Payment.Status.PENDING);
//    }
//
//    // ── Idempotency ──────────────────────────────────────────────────────────
//
//    @Test
//    void createPayment_returnsReplayOnDuplicateIdempotencyKey() {
//        givenBalance(new BigDecimal("500.00"));
//        givenTransferSucceeds();
//        String key = "idem-key-" + UUID.randomUUID();
//
//        CreatePaymentResult first  = service.createPayment(payer, payee, amount, "AED", "rent", key);
//        CreatePaymentResult second = service.createPayment(payer, payee, amount, "AED", "rent", key);
//
//        assertThat(first.isReplay()).isFalse();
//        assertThat(second.isReplay()).isTrue();
//        assertThat(second.payment().getId()).isEqualTo(first.payment().getId());
//
//        // Ledger called exactly once — no double charge
//        verify(ledgerClient, times(1)).getBalance(payer);
//        verify(ledgerClient, times(1)).executeTransfer(any(), any(), any(), any());
//        verify(eventPublisher, times(1)).publishPaymentCompleted(any());
//    }
//
//    @Test
//    void createPayment_nullIdempotencyKey_alwaysCreatesNew() {
//        givenBalance(new BigDecimal("500.00"));
//        givenTransferSucceeds();
//
//        CreatePaymentResult r1 = service.createPayment(payer, payee, amount, "AED", "rent", null);
//        CreatePaymentResult r2 = service.createPayment(payer, payee, amount, "AED", "rent", null);
//
//        assertThat(r1.payment().getId()).isNotEqualTo(r2.payment().getId());
//    }
//
//    // ── Insufficient balance ─────────────────────────────────────────────────
//
//    @Test
//    void createPayment_throwsInsufficientBalanceWhenFundsLow() {
//        givenBalance(new BigDecimal("50.00"));
//
//        assertThatThrownBy(() ->
//                service.createPayment(payer, payee, amount, "AED", "rent", null))
//                .isInstanceOf(InsufficientBalanceException.class)
//                .hasMessageContaining("Insufficient balance");
//    }
//
//    @Test
//    void createPayment_persistsFailedStatusOnInsufficientBalance() {
//        givenBalance(new BigDecimal("10.00"));
//
//        assertThatThrownBy(() ->
//                service.createPayment(payer, payee, amount, "AED", "rent", null))
//                .isInstanceOf(InsufficientBalanceException.class);
//
//        assertThat(repository.savedStatuses()).last().isEqualTo(Payment.Status.FAILED);
//        verify(ledgerClient, never()).executeTransfer(any(), any(), any(), any());
//        verifyNoInteractions(eventPublisher);
//    }
//
//    // ── Ledger errors ────────────────────────────────────────────────────────
//
//    @Test
//    void createPayment_marksFailed_onLedgerException() {
//        when(ledgerClient.getBalance(payer)).thenThrow(new LedgerException("Ledger down"));
//
//        assertThatThrownBy(() ->
//                service.createPayment(payer, payee, amount, "AED", "rent", null))
//                .isInstanceOf(LedgerException.class);
//
//        assertThat(repository.savedStatuses()).last().isEqualTo(Payment.Status.FAILED);
//        verifyNoInteractions(eventPublisher);
//    }
//
//    @Test
//    void createPayment_marksFailed_onInvalidAccountDuringTransfer() {
//        givenBalance(new BigDecimal("999.00"));
//        when(ledgerClient.executeTransfer(payer, payee, amount, "AED"))
//                .thenThrow(new InvalidAccountException("Invalid recipient"));
//
//        assertThatThrownBy(() ->
//                service.createPayment(payer, payee, amount, "AED", "rent", null))
//                .isInstanceOf(InvalidAccountException.class);
//
//        assertThat(repository.savedStatuses()).last().isEqualTo(Payment.Status.FAILED);
//        verifyNoInteractions(eventPublisher);
//    }
//
//    // ── Helpers ──────────────────────────────────────────────────────────────
//
////    private void givenBalance(BigDecimal balance) {
////        when(ledgerClient.getBalance(payer))
////                .thenReturn(new LedgerClient.BalanceResponse(payer, balance, "AED"));
////    }
//
////    private void givenTransferSucceeds() {
////        when(ledgerClient.executeTransfer(payer, payee, amount, "AED"))
////                .thenReturn(new LedgerClient.TransferResponse("txn-1", "SUCCESS"));
////    }
//
//    /**
//     * Extends {@link InMemoryPaymentRepository} to record every status at save time,
//     * so tests can assert the full PENDING → COMPLETED/FAILED transition sequence.
//     */
//    static class RecordingRepository extends InMemoryPaymentRepository {
//        private final List<Payment.Status> statuses = new ArrayList<>();
//
//        @Override
//        public Payment save(Payment p) {
//            statuses.add(p.getStatus());
//            return super.save(p);
//        }
//
//        @Override
//        public Optional<Payment> findById(UUID id) {
//            return super.findById(id);
//        }
//
//        @Override
//        public Optional<Payment> findByIdempotencyKey(String key) {
//            return super.findByIdempotencyKey(key);
//        }
//
//        List<Payment.Status> savedStatuses() {
//            return statuses;
//        }
//    }
//}
