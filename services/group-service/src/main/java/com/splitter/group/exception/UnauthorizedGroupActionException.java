package com.splitter.group.exception;

/**
 * Exception thrown when a user is not authorized to perform an action.
 */
public class UnauthorizedGroupActionException extends RuntimeException {

    public UnauthorizedGroupActionException(String message) {
        super(message);
    }
}
