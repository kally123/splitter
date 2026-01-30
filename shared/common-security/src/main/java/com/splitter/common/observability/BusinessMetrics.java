package com.splitter.common.observability;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Custom business metrics for the Splitter application.
 * Provides ready-to-use metrics for tracking business KPIs.
 */
@Component
public class BusinessMetrics implements MeterBinder {

    private MeterRegistry registry;
    
    // Counters
    private Counter expensesCreated;
    private Counter settlementsCompleted;
    private Counter groupsCreated;
    private Counter usersRegistered;
    private Counter paymentsProcessed;
    private Counter receiptsScanned;
    
    // Gauges
    private final AtomicLong activeUsers = new AtomicLong(0);
    private final AtomicLong pendingSettlements = new AtomicLong(0);
    
    // Timers
    private Timer expenseCreationTimer;
    private Timer balanceCalculationTimer;
    private Timer settlementProcessingTimer;
    private Timer receiptProcessingTimer;
    
    // Distribution summaries
    private DistributionSummary expenseAmountSummary;
    private DistributionSummary settlementAmountSummary;
    private DistributionSummary groupSizeSummary;

    @Override
    public void bindTo(MeterRegistry registry) {
        this.registry = registry;
        
        // Initialize counters
        expensesCreated = Counter.builder("splitter.expenses.created")
            .description("Total number of expenses created")
            .tag("version", "v1")
            .register(registry);
        
        settlementsCompleted = Counter.builder("splitter.settlements.completed")
            .description("Total number of settlements completed")
            .tag("version", "v1")
            .register(registry);
        
        groupsCreated = Counter.builder("splitter.groups.created")
            .description("Total number of groups created")
            .tag("version", "v1")
            .register(registry);
        
        usersRegistered = Counter.builder("splitter.users.registered")
            .description("Total number of user registrations")
            .tag("version", "v1")
            .register(registry);
        
        paymentsProcessed = Counter.builder("splitter.payments.processed")
            .description("Total number of payments processed")
            .tag("version", "v1")
            .register(registry);
        
        receiptsScanned = Counter.builder("splitter.receipts.scanned")
            .description("Total number of receipts scanned")
            .tag("version", "v1")
            .register(registry);
        
        // Initialize gauges
        Gauge.builder("splitter.users.active", activeUsers, AtomicLong::get)
            .description("Number of currently active users")
            .register(registry);
        
        Gauge.builder("splitter.settlements.pending", pendingSettlements, AtomicLong::get)
            .description("Number of pending settlements")
            .register(registry);
        
        // Initialize timers
        expenseCreationTimer = Timer.builder("splitter.expense.creation.time")
            .description("Time to create an expense")
            .publishPercentiles(0.5, 0.9, 0.95, 0.99)
            .register(registry);
        
        balanceCalculationTimer = Timer.builder("splitter.balance.calculation.time")
            .description("Time to calculate balances")
            .publishPercentiles(0.5, 0.9, 0.95, 0.99)
            .register(registry);
        
        settlementProcessingTimer = Timer.builder("splitter.settlement.processing.time")
            .description("Time to process a settlement")
            .publishPercentiles(0.5, 0.9, 0.95, 0.99)
            .register(registry);
        
        receiptProcessingTimer = Timer.builder("splitter.receipt.processing.time")
            .description("Time to process receipt OCR")
            .publishPercentiles(0.5, 0.9, 0.95, 0.99)
            .register(registry);
        
        // Initialize distribution summaries
        expenseAmountSummary = DistributionSummary.builder("splitter.expense.amount")
            .description("Distribution of expense amounts")
            .baseUnit("dollars")
            .publishPercentiles(0.5, 0.9, 0.95, 0.99)
            .register(registry);
        
        settlementAmountSummary = DistributionSummary.builder("splitter.settlement.amount")
            .description("Distribution of settlement amounts")
            .baseUnit("dollars")
            .publishPercentiles(0.5, 0.9, 0.95, 0.99)
            .register(registry);
        
        groupSizeSummary = DistributionSummary.builder("splitter.group.size")
            .description("Distribution of group sizes")
            .baseUnit("members")
            .register(registry);
    }

    // Counter methods
    public void incrementExpensesCreated() {
        expensesCreated.increment();
    }

    public void incrementExpensesCreated(String category) {
        Counter.builder("splitter.expenses.created.by_category")
            .tag("category", category)
            .register(registry)
            .increment();
    }

    public void incrementSettlementsCompleted() {
        settlementsCompleted.increment();
    }

    public void incrementSettlementsCompleted(String method) {
        Counter.builder("splitter.settlements.completed.by_method")
            .tag("method", method)
            .register(registry)
            .increment();
    }

    public void incrementGroupsCreated() {
        groupsCreated.increment();
    }

    public void incrementUsersRegistered() {
        usersRegistered.increment();
    }

    public void incrementPaymentsProcessed(String provider, String status) {
        Counter.builder("splitter.payments.processed.detailed")
            .tag("provider", provider)
            .tag("status", status)
            .register(registry)
            .increment();
    }

    public void incrementReceiptsScanned(String status) {
        Counter.builder("splitter.receipts.scanned.detailed")
            .tag("status", status)
            .register(registry)
            .increment();
    }

    // Gauge methods
    public void setActiveUsers(long count) {
        activeUsers.set(count);
    }

    public void incrementActiveUsers() {
        activeUsers.incrementAndGet();
    }

    public void decrementActiveUsers() {
        activeUsers.decrementAndGet();
    }

    public void setPendingSettlements(long count) {
        pendingSettlements.set(count);
    }

    // Timer methods
    public void recordExpenseCreationTime(long timeMs) {
        expenseCreationTimer.record(timeMs, TimeUnit.MILLISECONDS);
    }

    public Timer.Sample startExpenseCreation() {
        return Timer.start(registry);
    }

    public void stopExpenseCreation(Timer.Sample sample) {
        sample.stop(expenseCreationTimer);
    }

    public void recordBalanceCalculationTime(long timeMs) {
        balanceCalculationTimer.record(timeMs, TimeUnit.MILLISECONDS);
    }

    public <T> T timeBalanceCalculation(Supplier<T> operation) {
        return balanceCalculationTimer.record(operation);
    }

    public void recordSettlementProcessingTime(long timeMs) {
        settlementProcessingTimer.record(timeMs, TimeUnit.MILLISECONDS);
    }

    public void recordReceiptProcessingTime(long timeMs) {
        receiptProcessingTimer.record(timeMs, TimeUnit.MILLISECONDS);
    }

    // Distribution summary methods
    public void recordExpenseAmount(double amount) {
        expenseAmountSummary.record(amount);
    }

    public void recordExpenseAmount(double amount, String currency) {
        DistributionSummary.builder("splitter.expense.amount.by_currency")
            .tag("currency", currency)
            .register(registry)
            .record(amount);
    }

    public void recordSettlementAmount(double amount) {
        settlementAmountSummary.record(amount);
    }

    public void recordGroupSize(int size) {
        groupSizeSummary.record(size);
    }

    // Error tracking
    public void recordError(String service, String operation, String errorType) {
        Counter.builder("splitter.errors")
            .tag("service", service)
            .tag("operation", operation)
            .tag("type", errorType)
            .register(registry)
            .increment();
    }

    // API call tracking
    public void recordApiCall(String endpoint, String method, int statusCode, long durationMs) {
        Timer.builder("splitter.api.calls")
            .tag("endpoint", endpoint)
            .tag("method", method)
            .tag("status", String.valueOf(statusCode / 100) + "xx")
            .register(registry)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }

    // Cache metrics
    public void recordCacheHit(String cacheName) {
        Counter.builder("splitter.cache.hits")
            .tag("cache", cacheName)
            .register(registry)
            .increment();
    }

    public void recordCacheMiss(String cacheName) {
        Counter.builder("splitter.cache.misses")
            .tag("cache", cacheName)
            .register(registry)
            .increment();
    }

    // Kafka/messaging metrics
    public void recordMessagePublished(String topic) {
        Counter.builder("splitter.messages.published")
            .tag("topic", topic)
            .register(registry)
            .increment();
    }

    public void recordMessageConsumed(String topic, long processingTimeMs) {
        Timer.builder("splitter.messages.consumed")
            .tag("topic", topic)
            .register(registry)
            .record(processingTimeMs, TimeUnit.MILLISECONDS);
    }

    // Rate limiting metrics
    public void recordRateLimitHit(String endpoint, String userId) {
        Counter.builder("splitter.rate_limit.exceeded")
            .tag("endpoint", endpoint)
            .register(registry)
            .increment();
    }
}
