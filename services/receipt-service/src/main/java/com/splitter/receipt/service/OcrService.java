package com.splitter.receipt.service;

import com.splitter.receipt.model.ParsedReceipt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OcrService {
    
    private final List<OcrProvider> ocrProviders;
    private final ReceiptParserService receiptParserService;
    
    public Mono<ParsedReceipt> processReceipt(byte[] imageData, String contentType) {
        // Find an available OCR provider
        OcrProvider provider = ocrProviders.stream()
            .filter(OcrProvider::isAvailable)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No OCR provider available"));
        
        log.info("Processing receipt with provider: {}", provider.getProviderName());
        
        return provider.extractText(imageData, contentType)
            .flatMap(rawText -> {
                log.debug("Extracted raw text: {}", rawText.substring(0, Math.min(500, rawText.length())));
                return receiptParserService.parse(rawText);
            })
            .doOnSuccess(parsed -> 
                log.info("Successfully parsed receipt: total={}, merchant={}", 
                    parsed.getTotal(), parsed.getMerchantName()))
            .doOnError(e -> log.error("Failed to process receipt", e));
    }
}
