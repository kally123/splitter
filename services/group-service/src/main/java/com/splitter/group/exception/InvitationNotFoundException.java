package com.splitter.group.exception;

/**
 * Exception thrown when an invitation is not found.
 */
public class InvitationNotFoundException extends RuntimeException {

    public InvitationNotFoundException(String tokenOrId) {
        super(String.format("Invitation not found: %s", tokenOrId));
    }
}
