package com.splitter.receipt.repository;

import com.splitter.receipt.model.Receipt;
import com.splitter.receipt.model.ReceiptStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface ReceiptRepository extends ReactiveCrudRepository<Receipt, UUID> {
    
    Flux<Receipt> findByUserId(UUID userId);
    
    Mono<Receipt> findByIdAndUserId(UUID id, UUID userId);
    
    Flux<Receipt> findByExpenseId(UUID expenseId);
    
    Flux<Receipt> findByStatus(ReceiptStatus status);
    
    @Query("SELECT * FROM receipts WHERE status = 'UPLOADED' ORDER BY uploaded_at ASC LIMIT :limit")
    Flux<Receipt> findPendingReceipts(int limit);
    
    @Query("UPDATE receipts SET status = :status, processed_at = NOW() WHERE id = :id")
    Mono<Void> updateStatus(UUID id, ReceiptStatus status);
}
