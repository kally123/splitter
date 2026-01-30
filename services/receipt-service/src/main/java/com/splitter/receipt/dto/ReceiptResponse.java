package com.splitter.receipt.dto;

import com.splitter.receipt.model.ParsedReceipt;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptResponse {
    private UUID id;
    private UUID userId;
    private UUID expenseId;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private String status;
    private ParsedReceipt parsedData;
    private String errorMessage;
    private Instant uploadedAt;
    private Instant processedAt;
}
