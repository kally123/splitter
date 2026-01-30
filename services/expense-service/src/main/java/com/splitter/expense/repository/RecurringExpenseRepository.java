package com.splitter.expense.repository;

import com.splitter.expense.model.RecurringExpense;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface RecurringExpenseRepository extends ReactiveCrudRepository<RecurringExpense, UUID> {

    @Query("""
        SELECT * FROM recurring_expenses 
        WHERE group_id = :groupId 
        AND is_deleted = false
        ORDER BY created_at DESC
        """)
    Flux<RecurringExpense> findByGroupId(UUID groupId);

    @Query("""
        SELECT * FROM recurring_expenses 
        WHERE created_by = :userId 
        AND is_deleted = false
        ORDER BY created_at DESC
        """)
    Flux<RecurringExpense> findByCreatedBy(UUID userId);

    @Query("""
        SELECT * FROM recurring_expenses 
        WHERE is_active = true 
        AND is_deleted = false
        AND next_occurrence <= :date
        AND (end_date IS NULL OR end_date >= :date)
        ORDER BY next_occurrence ASC
        """)
    Flux<RecurringExpense> findDueRecurringExpenses(LocalDate date);

    @Query("""
        SELECT * FROM recurring_expenses 
        WHERE id = :id 
        AND is_deleted = false
        """)
    Mono<RecurringExpense> findActiveById(UUID id);

    @Query("""
        UPDATE recurring_expenses 
        SET is_active = :active, updated_at = NOW()
        WHERE id = :id
        """)
    Mono<Void> updateActiveStatus(UUID id, boolean active);

    @Query("""
        UPDATE recurring_expenses 
        SET next_occurrence = :nextOccurrence, 
            last_generated = :lastGenerated, 
            updated_at = NOW()
        WHERE id = :id
        """)
    Mono<Void> updateOccurrence(UUID id, LocalDate nextOccurrence, LocalDate lastGenerated);
}
