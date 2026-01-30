package com.splitter.settlement.exception;

/**
 * Exception thrown when a user attempts an unauthorized settlement action.
 */
public class UnauthorizedSettlementActionException extends RuntimeException {

    public UnauthorizedSettlementActionException(String message) {
        super(message);
    }
}
