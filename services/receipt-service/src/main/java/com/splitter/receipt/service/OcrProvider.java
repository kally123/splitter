package com.splitter.receipt.service;

import com.splitter.receipt.model.ParsedReceipt;
import reactor.core.publisher.Mono;

public interface OcrProvider {
    
    Mono<String> extractText(byte[] imageData, String contentType);
    
    String getProviderName();
    
    boolean isAvailable();
}
