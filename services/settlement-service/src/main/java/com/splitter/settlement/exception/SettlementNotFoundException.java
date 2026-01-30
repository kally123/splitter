package com.splitter.settlement.exception;

import java.util.UUID;

/**
 * Exception thrown when a settlement is not found.
 */
public class SettlementNotFoundException extends RuntimeException {

    public SettlementNotFoundException(UUID settlementId) {
        super(String.format("Settlement not found with ID: %s", settlementId));
    }
}
