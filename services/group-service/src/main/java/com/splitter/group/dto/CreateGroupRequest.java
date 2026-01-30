package com.splitter.group.dto;

import com.splitter.group.model.Group;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request for creating a new group.
 */
public record CreateGroupRequest(
    @NotBlank(message = "Group name is required")
    @Size(min = 1, max = 100, message = "Group name must be between 1 and 100 characters")
    String name,
    
    @Size(max = 500, message = "Description must be at most 500 characters")
    String description,
    
    Group.GroupType type,
    
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter code")
    String defaultCurrency,
    
    String coverImageUrl,
    
    Boolean simplifyDebts
) {}
