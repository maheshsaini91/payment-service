package com.example.payment.ledger;

public class InvalidAccountException extends LedgerException {
    public InvalidAccountException(String message) { super(message); }
}
