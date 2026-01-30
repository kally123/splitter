package com.splitter.receipt.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("receipts")
public class Receipt {
    
    @Id
    private UUID id;
    
    @Column("user_id")
    private UUID userId;
    
    @Column("expense_id")
    private UUID expenseId;
    
    @Column("original_filename")
    private String originalFilename;
    
    @Column("storage_path")
    private String storagePath;
    
    @Column("content_type")
    private String contentType;
    
    @Column("file_size")
    private Long fileSize;
    
    @Column("status")
    private ReceiptStatus status;
    
    @Column("raw_ocr_text")
    private String rawOcrText;
    
    @Column("parsed_data")
    private String parsedDataJson;
    
    @Column("error_message")
    private String errorMessage;
    
    @CreatedDate
    @Column("uploaded_at")
    private Instant uploadedAt;
    
    @Column("processed_at")
    private Instant processedAt;
}
