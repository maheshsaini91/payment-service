package com.example.payment.exception;

import com.example.payment.ledger.InvalidAccountException;
import com.example.payment.ledger.LedgerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 400 — bean-validation failures */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> handleValidation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(new ErrorBody("VALIDATION_ERROR", errors));
    }

    /** 422 — business rule: not enough funds */
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorBody> handleInsufficientBalance(InsufficientBalanceException ex) {
        return ResponseEntity.unprocessableEntity()
                .body(new ErrorBody("INSUFFICIENT_BALANCE", ex.getMessage()));
    }

    /** 422 — business rule: invalid account on ledger side */
    @ExceptionHandler(InvalidAccountException.class)
    public ResponseEntity<ErrorBody> handleInvalidAccount(InvalidAccountException ex) {
        return ResponseEntity.unprocessableEntity()
                .body(new ErrorBody("INVALID_ACCOUNT", ex.getMessage()));
    }

    /** 502 — ledger is reachable but returned an error or is down */
    @ExceptionHandler(LedgerException.class)
    public ResponseEntity<ErrorBody> handleLedger(LedgerException ex) {
        log.error("Ledger error", ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorBody("LEDGER_UNAVAILABLE", "Ledger service error"));
    }

    /** 500 — catch-all */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.internalServerError()
                .body(new ErrorBody("INTERNAL_ERROR", "An unexpected error occurred"));
    }

    public record ErrorBody(String code, String message) {}
}

