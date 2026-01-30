package com.splitter.group.exception;

import java.util.UUID;

/**
 * Exception thrown when a user is not a member of a group.
 */
public class NotGroupMemberException extends RuntimeException {

    public NotGroupMemberException(UUID groupId, UUID userId) {
        super(String.format("User %s is not a member of group %s", userId, groupId));
    }
}
