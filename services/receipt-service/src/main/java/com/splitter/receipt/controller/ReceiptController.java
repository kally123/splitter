package com.splitter.receipt.controller;

import com.splitter.receipt.dto.LinkReceiptRequest;
import com.splitter.receipt.dto.ReceiptResponse;
import com.splitter.receipt.dto.ReceiptUploadResponse;
import com.splitter.receipt.model.ParsedReceipt;
import com.splitter.receipt.service.ReceiptService;
import com.splitter.receipt.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/receipts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Receipts", description = "Receipt scanning and OCR API")
public class ReceiptController {
    
    private final ReceiptService receiptService;
    private final StorageService storageService;
    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a receipt for OCR processing")
    public Mono<ReceiptUploadResponse> uploadReceipt(
            @RequestPart("file") FilePart file,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("User {} uploading receipt: {}", userId, file.filename());
        return receiptService.upload(file, userId);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get receipt by ID")
    public Mono<ReceiptResponse> getReceipt(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return receiptService.getReceipt(id, userId);
    }
    
    @GetMapping
    @Operation(summary = "Get all receipts for current user")
    public Flux<ReceiptResponse> getUserReceipts(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return receiptService.getUserReceipts(userId);
    }
    
    @GetMapping("/{id}/parsed")
    @Operation(summary = "Get parsed data for a receipt")
    public Mono<ParsedReceipt> getParsedData(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return receiptService.getParsedData(id, userId);
    }
    
    @GetMapping("/{id}/download-url")
    @Operation(summary = "Get a presigned URL to download the receipt image")
    public Mono<String> getDownloadUrl(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "15") int expirationMinutes,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return receiptService.getReceipt(id, userId)
            .flatMap(receipt -> storageService.getPresignedUrl(
                extractStoragePath(receipt), expirationMinutes));
    }
    
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a receipt")
    public Mono<Void> deleteReceipt(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("User {} deleting receipt: {}", userId, id);
        return receiptService.delete(id, userId);
    }
    
    private String extractStoragePath(ReceiptResponse receipt) {
        // The storage path is stored internally, we need to get it from the service
        // For now, returning a placeholder - in real implementation, this would be part of the response
        return receipt.getOriginalFilename();
    }
}
