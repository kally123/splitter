package com.splitter.group.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request for inviting a user to a group.
 */
public record InviteRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    String email
) {}
