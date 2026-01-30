package com.splitter.receipt.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkReceiptRequest {
    
    @NotNull(message = "Expense ID is required")
    private UUID expenseId;
}
