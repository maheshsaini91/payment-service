package com.example.payment;

import com.example.payment.controller.PaymentController;
import com.example.payment.exception.GlobalExceptionHandler;
import com.example.payment.exception.InsufficientBalanceException;
import com.example.payment.ledger.InvalidAccountException;
import com.example.payment.ledger.LedgerException;
import com.example.payment.model.Payment;
import com.example.payment.service.CreatePaymentResult;
import com.example.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean PaymentService paymentService;

    private static final UUID PAYER = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID PAYEE = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");

    // ── POST /api/payments ───────────────────────────────────────────────────

    @Test
    void post_returns201_onNewPayment() throws Exception {
        var payment = buildPayment(Payment.Status.COMPLETED);
        when(paymentService.createPayment(any(), any(), any(), any(), any(), any()))
                .thenReturn(CreatePaymentResult.fresh(payment));

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/payments/" + payment.getId()))
                .andExpect(jsonPath("$.id").value(payment.getId().toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void post_returns200_onIdempotentReplay() throws Exception {
        var payment = buildPayment(Payment.Status.COMPLETED);
        when(paymentService.createPayment(any(), any(), any(), any(), any(), eq("key-abc")))
                .thenReturn(CreatePaymentResult.replay(payment));

        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", "key-abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(payment.getId().toString()));
    }

    @Test
    void post_returns422_onInsufficientBalance() throws Exception {
        when(paymentService.createPayment(any(), any(), any(), any(), any(), any()))
                .thenThrow(new InsufficientBalanceException("Insufficient balance"));

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"));
    }

    @Test
    void post_returns422_onInvalidAccount() throws Exception {
        when(paymentService.createPayment(any(), any(), any(), any(), any(), any()))
                .thenThrow(new InvalidAccountException("Invalid account"));

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_ACCOUNT"));
    }

    @Test
    void post_returns502_onLedgerError() throws Exception {
        when(paymentService.createPayment(any(), any(), any(), any(), any(), any()))
                .thenThrow(new LedgerException("Ledger down"));

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("LEDGER_UNAVAILABLE"));
    }

    @Test
    void post_returns400_onMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void post_returns400_onNegativeAmount() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "payerAccountId", PAYER,
                                "payeeAccountId", PAYEE,
                                "amount", "-10.00",
                                "currency", "AED"
                        ))))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/payments/{id} ───────────────────────────────────────────────

    @Test
    void get_returns200_whenFound() throws Exception {
        var payment = buildPayment(Payment.Status.PENDING);
        when(paymentService.findById(payment.getId())).thenReturn(Optional.of(payment));

        mockMvc.perform(get("/api/payments/{id}", payment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(payment.getId().toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void get_returns404_whenNotFound() throws Exception {
        when(paymentService.findById(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/payments/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String validBody() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "payerAccountId", PAYER,
                "payeeAccountId", PAYEE,
                "amount", "150.00",
                "currency", "AED",
                "description", "Rent payment"
        ));
    }

    private Payment buildPayment(Payment.Status status) {
        var p = new Payment(PAYER, PAYEE, new BigDecimal("150.00"), "AED", "Rent", null);
        if (status != Payment.Status.PENDING) p.updateStatus(status);
        return p;
    }
}
