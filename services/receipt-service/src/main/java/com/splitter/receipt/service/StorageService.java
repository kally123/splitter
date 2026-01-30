package com.splitter.receipt.service;

import reactor.core.publisher.Mono;

public interface StorageService {
    
    Mono<String> upload(byte[] data, String filename, String contentType);
    
    Mono<byte[]> download(String storagePath);
    
    Mono<Void> delete(String storagePath);
    
    Mono<String> getPresignedUrl(String storagePath, int expirationMinutes);
}
