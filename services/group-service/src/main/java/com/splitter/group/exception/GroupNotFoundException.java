package com.splitter.group.exception;

import java.util.UUID;

/**
 * Exception thrown when a group is not found.
 */
public class GroupNotFoundException extends RuntimeException {

    public GroupNotFoundException(UUID groupId) {
        super(String.format("Group not found with ID: %s", groupId));
    }
}
