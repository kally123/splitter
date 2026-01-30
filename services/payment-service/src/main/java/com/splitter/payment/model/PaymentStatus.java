package com.splitter.payment.model;

public enum PaymentStatus {
    PENDING,
    PROCESSING,
    REQUIRES_ACTION,
    COMPLETED,
    FAILED,
    CANCELLED,
    REFUNDED
}
