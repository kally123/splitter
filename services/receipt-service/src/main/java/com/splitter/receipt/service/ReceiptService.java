package com.splitter.receipt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitter.receipt.dto.ReceiptResponse;
import com.splitter.receipt.dto.ReceiptUploadResponse;
import com.splitter.receipt.model.ParsedReceipt;
import com.splitter.receipt.model.Receipt;
import com.splitter.receipt.model.ReceiptStatus;
import com.splitter.receipt.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReceiptService {
    
    private final ReceiptRepository receiptRepository;
    private final StorageService storageService;
    private final OcrService ocrService;
    private final ObjectMapper objectMapper;
    
    public Mono<ReceiptUploadResponse> upload(FilePart file, UUID userId) {
        String originalFilename = file.filename();
        String contentType = file.headers().getContentType() != null ? 
            file.headers().getContentType().toString() : "image/jpeg";
        
        return DataBufferUtils.join(file.content())
            .flatMap(dataBuffer -> {
                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                DataBufferUtils.release(dataBuffer);
                
                // Upload to storage
                return storageService.upload(bytes, originalFilename, contentType)
                    .flatMap(storagePath -> {
                        // Create receipt record
                        Receipt receipt = Receipt.builder()
                            .id(UUID.randomUUID())
                            .userId(userId)
                            .originalFilename(originalFilename)
                            .storagePath(storagePath)
                            .contentType(contentType)
                            .fileSize((long) bytes.length)
                            .status(ReceiptStatus.UPLOADED)
                            .uploadedAt(Instant.now())
                            .build();
                        
                        return receiptRepository.save(receipt)
                            .flatMap(savedReceipt -> 
                                // Start async processing
                                processReceiptAsync(savedReceipt.getId(), bytes, contentType)
                                    .thenReturn(savedReceipt)
                            );
                    });
            })
            .map(receipt -> ReceiptUploadResponse.builder()
                .id(receipt.getId())
                .status(receipt.getStatus().name())
                .uploadedAt(receipt.getUploadedAt())
                .build())
            .doOnSuccess(r -> log.info("Uploaded receipt: {}", r.getId()))
            .doOnError(e -> log.error("Failed to upload receipt", e));
    }
    
    private Mono<Void> processReceiptAsync(UUID receiptId, byte[] imageData, String contentType) {
        return Mono.defer(() -> {
            // Update status to processing
            return receiptRepository.findById(receiptId)
                .flatMap(receipt -> {
                    receipt.setStatus(ReceiptStatus.PROCESSING);
                    return receiptRepository.save(receipt);
                })
                .flatMap(receipt -> ocrService.processReceipt(imageData, contentType)
                    .flatMap(parsed -> {
                        try {
                            receipt.setParsedDataJson(objectMapper.writeValueAsString(parsed));
                            receipt.setStatus(ReceiptStatus.PARSED);
                            receipt.setProcessedAt(Instant.now());
                        } catch (Exception e) {
                            log.error("Failed to serialize parsed data", e);
                            receipt.setStatus(ReceiptStatus.FAILED);
                            receipt.setErrorMessage("Failed to serialize parsed data");
                        }
                        return receiptRepository.save(receipt);
                    })
                    .onErrorResume(e -> {
                        log.error("OCR processing failed for receipt: {}", receiptId, e);
                        receipt.setStatus(ReceiptStatus.FAILED);
                        receipt.setErrorMessage(e.getMessage());
                        receipt.setProcessedAt(Instant.now());
                        return receiptRepository.save(receipt);
                    })
                )
                .then();
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }
    
    public Mono<ReceiptResponse> getReceipt(UUID id, UUID userId) {
        return receiptRepository.findByIdAndUserId(id, userId)
            .map(this::toResponse);
    }
    
    public Flux<ReceiptResponse> getUserReceipts(UUID userId) {
        return receiptRepository.findByUserId(userId)
            .map(this::toResponse);
    }
    
    public Mono<ParsedReceipt> getParsedData(UUID id, UUID userId) {
        return receiptRepository.findByIdAndUserId(id, userId)
            .filter(receipt -> receipt.getStatus() == ReceiptStatus.PARSED)
            .flatMap(receipt -> {
                try {
                    return Mono.just(objectMapper.readValue(
                        receipt.getParsedDataJson(), ParsedReceipt.class));
                } catch (Exception e) {
                    return Mono.error(new RuntimeException("Failed to parse receipt data"));
                }
            });
    }
    
    public Mono<Void> delete(UUID id, UUID userId) {
        return receiptRepository.findByIdAndUserId(id, userId)
            .flatMap(receipt -> 
                storageService.delete(receipt.getStoragePath())
                    .then(receiptRepository.delete(receipt))
            );
    }
    
    private ReceiptResponse toResponse(Receipt receipt) {
        ParsedReceipt parsed = null;
        if (receipt.getParsedDataJson() != null) {
            try {
                parsed = objectMapper.readValue(receipt.getParsedDataJson(), ParsedReceipt.class);
            } catch (Exception e) {
                log.warn("Failed to parse receipt data for {}", receipt.getId());
            }
        }
        
        return ReceiptResponse.builder()
            .id(receipt.getId())
            .userId(receipt.getUserId())
            .expenseId(receipt.getExpenseId())
            .originalFilename(receipt.getOriginalFilename())
            .contentType(receipt.getContentType())
            .fileSize(receipt.getFileSize())
            .status(receipt.getStatus().name())
            .parsedData(parsed)
            .errorMessage(receipt.getErrorMessage())
            .uploadedAt(receipt.getUploadedAt())
            .processedAt(receipt.getProcessedAt())
            .build();
    }
}
