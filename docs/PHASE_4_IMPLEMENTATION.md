# Phase 4: Advanced Features & Scale

## Overview

Phase 4 focuses on expanding the Splitter platform with advanced features, mobile applications, payment integrations, and scaling for growth. This phase transforms the MVP into a feature-rich, production-grade application.

**Duration:** Weeks 13-18  
**Dependencies:** Phase 3 (MVP Complete) must be finished

---

## Goals

1. **Multi-Currency Support** - Full currency conversion with real-time exchange rates
2. **Recurring Expenses** - Automated recurring expense creation
3. **Receipt Scanning** - OCR-powered receipt capture and auto-fill
4. **Payment Integrations** - Venmo, PayPal, Stripe Connect for in-app settlements
5. **Mobile Applications** - React Native apps for iOS and Android
6. **Analytics Dashboard** - Spending insights and reports
7. **Advanced Group Features** - Categories, budgets, and group templates
8. **Performance & Scale** - Caching strategies, read replicas, CDN

---

## Sprint 4.1: Multi-Currency Support (Week 13)

### 4.1.1 Currency Service

**Objective:** Create a dedicated service for currency conversion with caching

**New Service Structure:**
```
services/currency-service/
├── src/main/java/com/splitter/currency/
│   ├── CurrencyServiceApplication.java
│   ├── config/
│   │   ├── CacheConfig.java
│   │   └── ExchangeRateClientConfig.java
│   ├── controller/
│   │   └── CurrencyController.java
│   ├── service/
│   │   ├── CurrencyService.java
│   │   ├── ExchangeRateService.java
│   │   └── ExchangeRateProvider.java
│   ├── client/
│   │   ├── OpenExchangeRatesClient.java
│   │   └── FallbackExchangeRateClient.java
│   ├── model/
│   │   ├── Currency.java
│   │   ├── ExchangeRate.java
│   │   └── ConversionResult.java
│   └── repository/
│       └── ExchangeRateRepository.java
└── src/main/resources/
    ├── application.yml
    └── currencies.json
```

**API Endpoints:**

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/currencies` | List all supported currencies |
| GET | `/api/v1/currencies/{code}` | Get currency details |
| GET | `/api/v1/currencies/convert` | Convert amount between currencies |
| GET | `/api/v1/currencies/rates` | Get exchange rates for base currency |

**Currency Conversion Service:**
```java
@Service
@Slf4j
public class CurrencyService {
    
    private final ExchangeRateService exchangeRateService;
    private final ReactiveRedisTemplate<String, ExchangeRate> redisTemplate;
    
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    
    public Mono<ConversionResult> convert(
            BigDecimal amount, 
            String fromCurrency, 
            String toCurrency,
            LocalDate date) {
        
        if (fromCurrency.equals(toCurrency)) {
            return Mono.just(new ConversionResult(amount, amount, BigDecimal.ONE, fromCurrency, toCurrency));
        }
        
        return getExchangeRate(fromCurrency, toCurrency, date)
            .map(rate -> {
                BigDecimal convertedAmount = amount.multiply(rate)
                    .setScale(2, RoundingMode.HALF_UP);
                return new ConversionResult(amount, convertedAmount, rate, fromCurrency, toCurrency);
            });
    }
    
    public Mono<BigDecimal> getExchangeRate(String from, String to, LocalDate date) {
        String cacheKey = String.format("rate:%s:%s:%s", from, to, date);
        
        return redisTemplate.opsForValue().get(cacheKey)
            .map(ExchangeRate::getRate)
            .switchIfEmpty(
                exchangeRateService.fetchRate(from, to, date)
                    .flatMap(rate -> cacheRate(cacheKey, rate).thenReturn(rate.getRate()))
            );
    }
    
    public Flux<Currency> getSupportedCurrencies() {
        return Flux.fromIterable(CurrencyRegistry.getAllCurrencies());
    }
    
    private Mono<Boolean> cacheRate(String key, ExchangeRate rate) {
        return redisTemplate.opsForValue().set(key, rate, CACHE_TTL);
    }
}
```

**Exchange Rate Provider Interface:**
```java
public interface ExchangeRateProvider {
    Mono<ExchangeRate> getRate(String baseCurrency, String targetCurrency, LocalDate date);
    Flux<ExchangeRate> getRates(String baseCurrency, LocalDate date);
    boolean supports(String provider);
}

@Component
@Primary
public class OpenExchangeRatesProvider implements ExchangeRateProvider {
    
    private final WebClient webClient;
    private final String apiKey;
    
    @Override
    public Mono<ExchangeRate> getRate(String base, String target, LocalDate date) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/historical/{date}.json")
                .queryParam("app_id", apiKey)
                .queryParam("base", base)
                .queryParam("symbols", target)
                .build(date.toString()))
            .retrieve()
            .bodyToMono(OpenExchangeResponse.class)
            .map(response -> ExchangeRate.builder()
                .baseCurrency(base)
                .targetCurrency(target)
                .rate(response.getRates().get(target))
                .date(date)
                .provider("openexchangerates")
                .build());
    }
}
```

### 4.1.2 Multi-Currency Expense Updates

**Objective:** Update expense service to handle multi-currency expenses

**Enhanced Expense Model:**
```java
@Table("expenses")
public class Expense {
    // Existing fields...
    
    @Column("original_amount")
    private BigDecimal originalAmount;
    
    @Column("original_currency")
    private String originalCurrency;
    
    @Column("converted_amount")
    private BigDecimal convertedAmount;  // Amount in group's default currency
    
    @Column("group_currency")
    private String groupCurrency;
    
    @Column("exchange_rate")
    private BigDecimal exchangeRate;
    
    @Column("exchange_rate_date")
    private LocalDate exchangeRateDate;
}
```

**Currency-Aware Balance Calculation:**
```java
@Service
public class MultiCurrencyBalanceService {
    
    private final CurrencyServiceClient currencyClient;
    private final BalanceRepository balanceRepository;
    
    public Mono<GroupBalanceSummary> calculateGroupBalances(UUID groupId) {
        return groupRepository.findById(groupId)
            .flatMap(group -> {
                String groupCurrency = group.getDefaultCurrency();
                
                return expenseRepository.findByGroupId(groupId)
                    .flatMap(expense -> convertToGroupCurrency(expense, groupCurrency))
                    .collectList()
                    .map(expenses -> calculateBalances(expenses, groupCurrency));
            });
    }
    
    private Mono<NormalizedExpense> convertToGroupCurrency(Expense expense, String groupCurrency) {
        if (expense.getOriginalCurrency().equals(groupCurrency)) {
            return Mono.just(NormalizedExpense.from(expense));
        }
        
        return currencyClient.convert(
                expense.getOriginalAmount(),
                expense.getOriginalCurrency(),
                groupCurrency,
                expense.getDate())
            .map(result -> NormalizedExpense.builder()
                .expenseId(expense.getId())
                .originalAmount(expense.getOriginalAmount())
                .originalCurrency(expense.getOriginalCurrency())
                .convertedAmount(result.getConvertedAmount())
                .groupCurrency(groupCurrency)
                .exchangeRate(result.getRate())
                .build());
    }
}
```

### 4.1.3 Frontend Currency Support

**Currency Selector Component:**
```typescript
// components/currency/currency-selector.tsx
'use client';

import { useState } from 'react';
import { Check, ChevronsUpDown, Search } from 'lucide-react';
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  Popover,
  PopoverContent,
  PopoverTrigger,
  Button,
} from '@/components/ui';
import { useCurrencies } from '@/lib/hooks/useCurrencies';
import { cn } from '@/lib/utils';

interface CurrencySelectorProps {
  value: string;
  onChange: (currency: string) => void;
  label?: string;
}

export function CurrencySelector({ value, onChange, label }: CurrencySelectorProps) {
  const [open, setOpen] = useState(false);
  const { data: currencies, isLoading } = useCurrencies();

  const selectedCurrency = currencies?.find(c => c.code === value);

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="outline"
          role="combobox"
          aria-expanded={open}
          className="w-full justify-between"
        >
          {selectedCurrency ? (
            <span className="flex items-center gap-2">
              <span className="text-lg">{selectedCurrency.symbol}</span>
              <span>{selectedCurrency.code}</span>
              <span className="text-muted-foreground text-sm">
                {selectedCurrency.name}
              </span>
            </span>
          ) : (
            "Select currency..."
          )}
          <ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-[400px] p-0">
        <Command>
          <CommandInput placeholder="Search currencies..." />
          <CommandEmpty>No currency found.</CommandEmpty>
          <CommandGroup className="max-h-[300px] overflow-auto">
            {currencies?.map((currency) => (
              <CommandItem
                key={currency.code}
                value={`${currency.code} ${currency.name}`}
                onSelect={() => {
                  onChange(currency.code);
                  setOpen(false);
                }}
              >
                <Check
                  className={cn(
                    "mr-2 h-4 w-4",
                    value === currency.code ? "opacity-100" : "opacity-0"
                  )}
                />
                <span className="text-lg mr-2">{currency.symbol}</span>
                <span className="font-medium mr-2">{currency.code}</span>
                <span className="text-muted-foreground">{currency.name}</span>
              </CommandItem>
            ))}
          </CommandGroup>
        </Command>
      </PopoverContent>
    </Popover>
  );
}
```

**Multi-Currency Amount Display:**
```typescript
// components/currency/multi-currency-amount.tsx
interface MultiCurrencyAmountProps {
  originalAmount: number;
  originalCurrency: string;
  convertedAmount?: number;
  groupCurrency?: string;
  showConversion?: boolean;
}

export function MultiCurrencyAmount({
  originalAmount,
  originalCurrency,
  convertedAmount,
  groupCurrency,
  showConversion = true,
}: MultiCurrencyAmountProps) {
  const formatCurrency = (amount: number, currency: string) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency,
    }).format(amount);
  };

  const needsConversion = groupCurrency && originalCurrency !== groupCurrency;

  return (
    <div className="flex flex-col">
      <span className="font-semibold">
        {formatCurrency(originalAmount, originalCurrency)}
      </span>
      {showConversion && needsConversion && convertedAmount && (
        <span className="text-sm text-muted-foreground">
          ≈ {formatCurrency(convertedAmount, groupCurrency)}
        </span>
      )}
    </div>
  );
}
```

---

## Sprint 4.2: Recurring Expenses (Week 13-14)

### 4.2.1 Recurring Expense Model

**Objective:** Allow users to set up recurring expenses that auto-generate

**Database Schema:**
```sql
CREATE TABLE recurring_expenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL REFERENCES groups(id),
    created_by UUID NOT NULL,
    description VARCHAR(255) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    category VARCHAR(50),
    split_type VARCHAR(20) NOT NULL,
    splits JSONB,
    
    -- Recurrence settings
    frequency VARCHAR(20) NOT NULL,  -- DAILY, WEEKLY, BIWEEKLY, MONTHLY, YEARLY
    interval_value INT NOT NULL DEFAULT 1,
    day_of_week INT,  -- 1-7 for weekly
    day_of_month INT, -- 1-31 for monthly
    month_of_year INT, -- 1-12 for yearly
    
    -- Schedule
    start_date DATE NOT NULL,
    end_date DATE,
    next_occurrence DATE NOT NULL,
    last_generated DATE,
    
    -- Status
    is_active BOOLEAN DEFAULT true,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_recurring_next ON recurring_expenses(next_occurrence) WHERE is_active = true;
```

**Recurring Expense Entity:**
```java
@Table("recurring_expenses")
@Data
@Builder
public class RecurringExpense {
    @Id
    private UUID id;
    private UUID groupId;
    private UUID createdBy;
    private String description;
    private BigDecimal amount;
    private String currency;
    private String category;
    private SplitType splitType;
    
    @Column("splits")
    private String splitsJson;  // JSON array of splits
    
    private RecurrenceFrequency frequency;
    private Integer intervalValue;
    private Integer dayOfWeek;
    private Integer dayOfMonth;
    private Integer monthOfYear;
    
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate nextOccurrence;
    private LocalDate lastGenerated;
    
    private boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    
    public LocalDate calculateNextOccurrence(LocalDate from) {
        return switch (frequency) {
            case DAILY -> from.plusDays(intervalValue);
            case WEEKLY -> from.plusWeeks(intervalValue);
            case BIWEEKLY -> from.plusWeeks(2L * intervalValue);
            case MONTHLY -> from.plusMonths(intervalValue);
            case YEARLY -> from.plusYears(intervalValue);
        };
    }
}

public enum RecurrenceFrequency {
    DAILY, WEEKLY, BIWEEKLY, MONTHLY, YEARLY
}
```

### 4.2.2 Recurring Expense Scheduler

**Objective:** Background job to generate expenses from recurring templates

**Scheduler Service:**
```java
@Service
@Slf4j
@RequiredArgsConstructor
public class RecurringExpenseScheduler {
    
    private final RecurringExpenseRepository recurringExpenseRepository;
    private final ExpenseService expenseService;
    private final TransactionalOperator transactionalOperator;
    
    @Scheduled(cron = "0 0 1 * * *")  // Run at 1 AM daily
    public void processRecurringExpenses() {
        LocalDate today = LocalDate.now();
        
        recurringExpenseRepository.findDueRecurringExpenses(today)
            .flatMap(recurring -> generateExpense(recurring, today))
            .doOnNext(expense -> log.info("Generated expense {} from recurring {}", 
                expense.getId(), expense.getRecurringExpenseId()))
            .doOnError(error -> log.error("Error processing recurring expenses", error))
            .subscribe();
    }
    
    private Mono<Expense> generateExpense(RecurringExpense recurring, LocalDate date) {
        return transactionalOperator.transactional(
            Mono.defer(() -> {
                CreateExpenseRequest request = CreateExpenseRequest.builder()
                    .groupId(recurring.getGroupId())
                    .description(recurring.getDescription())
                    .amount(recurring.getAmount())
                    .currency(recurring.getCurrency())
                    .category(recurring.getCategory())
                    .splitType(recurring.getSplitType())
                    .splits(parseSplits(recurring.getSplitsJson()))
                    .date(date)
                    .recurringExpenseId(recurring.getId())
                    .build();
                
                return expenseService.createExpense(request, recurring.getCreatedBy())
                    .flatMap(expense -> updateNextOccurrence(recurring, date)
                        .thenReturn(expense));
            })
        );
    }
    
    private Mono<RecurringExpense> updateNextOccurrence(RecurringExpense recurring, LocalDate generated) {
        LocalDate nextDate = recurring.calculateNextOccurrence(generated);
        
        // Check if we've passed the end date
        if (recurring.getEndDate() != null && nextDate.isAfter(recurring.getEndDate())) {
            recurring.setActive(false);
        }
        
        recurring.setNextOccurrence(nextDate);
        recurring.setLastGenerated(generated);
        recurring.setUpdatedAt(Instant.now());
        
        return recurringExpenseRepository.save(recurring);
    }
}
```

### 4.2.3 Recurring Expense API

**Controller:**
```java
@RestController
@RequestMapping("/api/v1/recurring-expenses")
@RequiredArgsConstructor
public class RecurringExpenseController {
    
    private final RecurringExpenseService recurringExpenseService;
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RecurringExpenseResponse> create(
            @Valid @RequestBody CreateRecurringExpenseRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        return recurringExpenseService.create(request, user.getUserId());
    }
    
    @GetMapping
    public Flux<RecurringExpenseResponse> getByGroup(
            @RequestParam UUID groupId,
            @AuthenticationPrincipal UserPrincipal user) {
        return recurringExpenseService.getByGroup(groupId, user.getUserId());
    }
    
    @PutMapping("/{id}")
    public Mono<RecurringExpenseResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRecurringExpenseRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        return recurringExpenseService.update(id, request, user.getUserId());
    }
    
    @PostMapping("/{id}/pause")
    public Mono<RecurringExpenseResponse> pause(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        return recurringExpenseService.pause(id, user.getUserId());
    }
    
    @PostMapping("/{id}/resume")
    public Mono<RecurringExpenseResponse> resume(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        return recurringExpenseService.resume(id, user.getUserId());
    }
    
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        return recurringExpenseService.delete(id, user.getUserId());
    }
}
```

**Frontend - Recurring Expense Form:**
```typescript
// components/expenses/recurring-expense-form.tsx
'use client';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  Form, FormField, FormItem, FormLabel, FormControl, FormMessage,
  Input, Button, Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
  Switch, Calendar, Popover, PopoverContent, PopoverTrigger,
} from '@/components/ui';
import { CalendarIcon, Repeat } from 'lucide-react';
import { format } from 'date-fns';

const recurringExpenseSchema = z.object({
  description: z.string().min(1, 'Description is required'),
  amount: z.number().positive('Amount must be positive'),
  currency: z.string().length(3),
  category: z.string().optional(),
  splitType: z.enum(['EQUAL', 'PERCENTAGE', 'SHARES', 'EXACT']),
  frequency: z.enum(['DAILY', 'WEEKLY', 'BIWEEKLY', 'MONTHLY', 'YEARLY']),
  intervalValue: z.number().int().positive().default(1),
  dayOfWeek: z.number().int().min(1).max(7).optional(),
  dayOfMonth: z.number().int().min(1).max(31).optional(),
  startDate: z.date(),
  endDate: z.date().optional(),
});

type RecurringExpenseFormData = z.infer<typeof recurringExpenseSchema>;

interface RecurringExpenseFormProps {
  groupId: string;
  onSuccess: () => void;
}

export function RecurringExpenseForm({ groupId, onSuccess }: RecurringExpenseFormProps) {
  const form = useForm<RecurringExpenseFormData>({
    resolver: zodResolver(recurringExpenseSchema),
    defaultValues: {
      frequency: 'MONTHLY',
      intervalValue: 1,
      splitType: 'EQUAL',
      currency: 'USD',
      startDate: new Date(),
    },
  });

  const frequency = form.watch('frequency');

  const onSubmit = async (data: RecurringExpenseFormData) => {
    // API call to create recurring expense
  };

  const getFrequencyLabel = (freq: string, interval: number) => {
    if (interval === 1) {
      return freq.toLowerCase().replace('ly', '');
    }
    return `every ${interval} ${freq.toLowerCase().replace('ly', 's')}`;
  };

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
        <div className="flex items-center gap-2 text-primary mb-4">
          <Repeat className="h-5 w-5" />
          <h3 className="font-semibold">Recurring Expense</h3>
        </div>

        <FormField
          control={form.control}
          name="description"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Description</FormLabel>
              <FormControl>
                <Input placeholder="Monthly rent" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <div className="grid grid-cols-2 gap-4">
          <FormField
            control={form.control}
            name="amount"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Amount</FormLabel>
                <FormControl>
                  <Input 
                    type="number" 
                    step="0.01"
                    {...field}
                    onChange={e => field.onChange(parseFloat(e.target.value))}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="frequency"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Frequency</FormLabel>
                <Select onValueChange={field.onChange} defaultValue={field.value}>
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    <SelectItem value="DAILY">Daily</SelectItem>
                    <SelectItem value="WEEKLY">Weekly</SelectItem>
                    <SelectItem value="BIWEEKLY">Bi-weekly</SelectItem>
                    <SelectItem value="MONTHLY">Monthly</SelectItem>
                    <SelectItem value="YEARLY">Yearly</SelectItem>
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        {frequency === 'WEEKLY' && (
          <FormField
            control={form.control}
            name="dayOfWeek"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Day of Week</FormLabel>
                <Select onValueChange={(v) => field.onChange(parseInt(v))} defaultValue={field.value?.toString()}>
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue placeholder="Select day" />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    {['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'].map((day, i) => (
                      <SelectItem key={day} value={(i + 1).toString()}>{day}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />
        )}

        {frequency === 'MONTHLY' && (
          <FormField
            control={form.control}
            name="dayOfMonth"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Day of Month</FormLabel>
                <Select onValueChange={(v) => field.onChange(parseInt(v))} defaultValue={field.value?.toString()}>
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue placeholder="Select day" />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    {Array.from({ length: 31 }, (_, i) => (
                      <SelectItem key={i + 1} value={(i + 1).toString()}>
                        {i + 1}{getOrdinalSuffix(i + 1)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />
        )}

        <div className="grid grid-cols-2 gap-4">
          <FormField
            control={form.control}
            name="startDate"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Start Date</FormLabel>
                <Popover>
                  <PopoverTrigger asChild>
                    <FormControl>
                      <Button variant="outline" className="w-full justify-start">
                        <CalendarIcon className="mr-2 h-4 w-4" />
                        {field.value ? format(field.value, 'PPP') : 'Pick a date'}
                      </Button>
                    </FormControl>
                  </PopoverTrigger>
                  <PopoverContent>
                    <Calendar
                      mode="single"
                      selected={field.value}
                      onSelect={field.onChange}
                    />
                  </PopoverContent>
                </Popover>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="endDate"
            render={({ field }) => (
              <FormItem>
                <FormLabel>End Date (Optional)</FormLabel>
                <Popover>
                  <PopoverTrigger asChild>
                    <FormControl>
                      <Button variant="outline" className="w-full justify-start">
                        <CalendarIcon className="mr-2 h-4 w-4" />
                        {field.value ? format(field.value, 'PPP') : 'No end date'}
                      </Button>
                    </FormControl>
                  </PopoverTrigger>
                  <PopoverContent>
                    <Calendar
                      mode="single"
                      selected={field.value}
                      onSelect={field.onChange}
                    />
                  </PopoverContent>
                </Popover>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        <Button type="submit" className="w-full">
          Create Recurring Expense
        </Button>
      </form>
    </Form>
  );
}

function getOrdinalSuffix(n: number): string {
  const s = ['th', 'st', 'nd', 'rd'];
  const v = n % 100;
  return s[(v - 20) % 10] || s[v] || s[0];
}
```

---

## Sprint 4.3: Receipt Scanning (Week 14-15)

### 4.3.1 Receipt Processing Service

**Objective:** OCR-powered receipt capture with auto-fill capabilities

**New Service Structure:**
```
services/receipt-service/
├── src/main/java/com/splitter/receipt/
│   ├── ReceiptServiceApplication.java
│   ├── config/
│   │   ├── S3Config.java
│   │   └── OcrConfig.java
│   ├── controller/
│   │   └── ReceiptController.java
│   ├── service/
│   │   ├── ReceiptService.java
│   │   ├── OcrService.java
│   │   ├── ReceiptParserService.java
│   │   └── StorageService.java
│   ├── model/
│   │   ├── Receipt.java
│   │   ├── ReceiptData.java
│   │   └── ParsedReceipt.java
│   └── ocr/
│       ├── OcrProvider.java
│       ├── GoogleVisionOcrProvider.java
│       └── AWSTextractProvider.java
```

**Receipt Model:**
```java
@Table("receipts")
@Data
@Builder
public class Receipt {
    @Id
    private UUID id;
    private UUID userId;
    private UUID expenseId;
    
    private String originalFilename;
    private String storagePath;
    private String contentType;
    private Long fileSize;
    
    private ReceiptStatus status;
    private String rawOcrText;
    private String parsedDataJson;
    
    private Instant uploadedAt;
    private Instant processedAt;
    private String errorMessage;
}

public enum ReceiptStatus {
    UPLOADED, PROCESSING, PARSED, FAILED
}

@Data
@Builder
public class ParsedReceipt {
    private String merchantName;
    private String merchantAddress;
    private LocalDate date;
    private LocalTime time;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal tip;
    private BigDecimal total;
    private String currency;
    private List<LineItem> items;
    private String paymentMethod;
    private Double confidence;
}

@Data
public class LineItem {
    private String description;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
```

**OCR Service:**
```java
@Service
@Slf4j
@RequiredArgsConstructor
public class OcrService {
    
    private final List<OcrProvider> providers;
    private final ReceiptParserService parserService;
    
    public Mono<ParsedReceipt> processReceipt(byte[] imageData, String contentType) {
        return getProvider()
            .extractText(imageData, contentType)
            .flatMap(rawText -> parserService.parse(rawText))
            .doOnSuccess(parsed -> log.info("Receipt parsed: merchant={}, total={}", 
                parsed.getMerchantName(), parsed.getTotal()))
            .doOnError(error -> log.error("OCR processing failed", error));
    }
    
    private OcrProvider getProvider() {
        return providers.stream()
            .filter(OcrProvider::isAvailable)
            .findFirst()
            .orElseThrow(() -> new OcrException("No OCR provider available"));
    }
}

@Component
@RequiredArgsConstructor
public class GoogleVisionOcrProvider implements OcrProvider {
    
    private final ImageAnnotatorClient visionClient;
    
    @Override
    public Mono<String> extractText(byte[] imageData, String contentType) {
        return Mono.fromCallable(() -> {
            Image image = Image.newBuilder()
                .setContent(ByteString.copyFrom(imageData))
                .build();
            
            Feature feature = Feature.newBuilder()
                .setType(Feature.Type.DOCUMENT_TEXT_DETECTION)
                .build();
            
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(feature)
                .setImage(image)
                .build();
            
            BatchAnnotateImagesResponse response = visionClient.batchAnnotateImages(
                List.of(request));
            
            return response.getResponses(0).getFullTextAnnotation().getText();
        }).subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public boolean isAvailable() {
        return visionClient != null;
    }
}
```

**Receipt Parser with ML:**
```java
@Service
@Slf4j
public class ReceiptParserService {
    
    private static final Pattern TOTAL_PATTERN = Pattern.compile(
        "(?i)(?:total|amount|sum|grand total)[:\\s]*\\$?([0-9]+[.,][0-9]{2})"
    );
    private static final Pattern DATE_PATTERN = Pattern.compile(
        "(\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{2,4})"
    );
    private static final Pattern TAX_PATTERN = Pattern.compile(
        "(?i)(?:tax|vat|gst)[:\\s]*\\$?([0-9]+[.,][0-9]{2})"
    );
    
    public Mono<ParsedReceipt> parse(String rawText) {
        return Mono.fromCallable(() -> {
            ParsedReceipt.ParsedReceiptBuilder builder = ParsedReceipt.builder();
            
            // Extract total
            extractAmount(rawText, TOTAL_PATTERN)
                .ifPresent(builder::total);
            
            // Extract date
            extractDate(rawText, DATE_PATTERN)
                .ifPresent(builder::date);
            
            // Extract tax
            extractAmount(rawText, TAX_PATTERN)
                .ifPresent(builder::tax);
            
            // Extract merchant (usually first line)
            extractMerchant(rawText)
                .ifPresent(builder::merchantName);
            
            // Extract line items
            List<LineItem> items = extractLineItems(rawText);
            builder.items(items);
            
            // Calculate confidence based on extracted fields
            double confidence = calculateConfidence(builder.build());
            builder.confidence(confidence);
            
            return builder.build();
        });
    }
    
    private Optional<BigDecimal> extractAmount(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String amountStr = matcher.group(1).replace(",", ".");
            return Optional.of(new BigDecimal(amountStr));
        }
        return Optional.empty();
    }
    
    private Optional<LocalDate> extractDate(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            try {
                return Optional.of(parseFlexibleDate(matcher.group(1)));
            } catch (Exception e) {
                log.warn("Could not parse date: {}", matcher.group(1));
            }
        }
        return Optional.empty();
    }
    
    private double calculateConfidence(ParsedReceipt receipt) {
        int fields = 0;
        int populated = 0;
        
        fields++; if (receipt.getTotal() != null) populated++;
        fields++; if (receipt.getDate() != null) populated++;
        fields++; if (receipt.getMerchantName() != null) populated++;
        fields++; if (receipt.getItems() != null && !receipt.getItems().isEmpty()) populated++;
        
        return (double) populated / fields;
    }
}
```

### 4.3.2 Receipt Upload API & Frontend

**Controller:**
```java
@RestController
@RequestMapping("/api/v1/receipts")
@RequiredArgsConstructor
public class ReceiptController {
    
    private final ReceiptService receiptService;
    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ReceiptUploadResponse> uploadReceipt(
            @RequestPart("file") FilePart file,
            @AuthenticationPrincipal UserPrincipal user) {
        return receiptService.upload(file, user.getUserId());
    }
    
    @GetMapping("/{id}")
    public Mono<ReceiptResponse> getReceipt(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        return receiptService.getReceipt(id, user.getUserId());
    }
    
    @GetMapping("/{id}/parsed")
    public Mono<ParsedReceipt> getParsedData(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal user) {
        return receiptService.getParsedData(id, user.getUserId());
    }
    
    @PostMapping("/{id}/apply")
    public Mono<ExpenseResponse> applyToExpense(
            @PathVariable UUID id,
            @RequestBody ApplyReceiptRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        return receiptService.applyToExpense(id, request, user.getUserId());
    }
}
```

**Frontend - Receipt Scanner:**
```typescript
// components/expenses/receipt-scanner.tsx
'use client';

import { useState, useRef, useCallback } from 'react';
import { Camera, Upload, X, Check, Loader2 } from 'lucide-react';
import { Button, Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui';
import { useUploadReceipt, useReceiptData } from '@/lib/hooks/useReceipts';
import { cn } from '@/lib/utils';

interface ReceiptScannerProps {
  onReceiptParsed: (data: ParsedReceiptData) => void;
  groupId: string;
}

export function ReceiptScanner({ onReceiptParsed, groupId }: ReceiptScannerProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [preview, setPreview] = useState<string | null>(null);
  const [receiptId, setReceiptId] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const cameraInputRef = useRef<HTMLInputElement>(null);

  const uploadMutation = useUploadReceipt();
  const { data: parsedData, isLoading: isParsing } = useReceiptData(receiptId);

  const handleFileSelect = useCallback(async (file: File) => {
    // Preview
    const reader = new FileReader();
    reader.onload = (e) => setPreview(e.target?.result as string);
    reader.readAsDataURL(file);

    // Upload
    try {
      const result = await uploadMutation.mutateAsync(file);
      setReceiptId(result.id);
    } catch (error) {
      console.error('Upload failed:', error);
    }
  }, [uploadMutation]);

  const handleApply = useCallback(() => {
    if (parsedData) {
      onReceiptParsed({
        description: parsedData.merchantName || 'Receipt expense',
        amount: parsedData.total,
        date: parsedData.date,
        category: guessCategory(parsedData.merchantName),
      });
      setIsOpen(false);
      resetState();
    }
  }, [parsedData, onReceiptParsed]);

  const resetState = () => {
    setPreview(null);
    setReceiptId(null);
  };

  return (
    <>
      <Button variant="outline" size="sm" onClick={() => setIsOpen(true)}>
        <Camera className="h-4 w-4 mr-2" />
        Scan Receipt
      </Button>

      <Dialog open={isOpen} onOpenChange={setIsOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Scan Receipt</DialogTitle>
          </DialogHeader>

          {!preview ? (
            <div className="grid grid-cols-2 gap-4 py-4">
              <Button
                variant="outline"
                className="h-32 flex-col gap-2"
                onClick={() => cameraInputRef.current?.click()}
              >
                <Camera className="h-8 w-8" />
                Take Photo
              </Button>
              <Button
                variant="outline"
                className="h-32 flex-col gap-2"
                onClick={() => fileInputRef.current?.click()}
              >
                <Upload className="h-8 w-8" />
                Upload Image
              </Button>

              <input
                ref={cameraInputRef}
                type="file"
                accept="image/*"
                capture="environment"
                className="hidden"
                onChange={(e) => e.target.files?.[0] && handleFileSelect(e.target.files[0])}
              />
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                className="hidden"
                onChange={(e) => e.target.files?.[0] && handleFileSelect(e.target.files[0])}
              />
            </div>
          ) : (
            <div className="space-y-4">
              <div className="relative">
                <img
                  src={preview}
                  alt="Receipt preview"
                  className="w-full max-h-64 object-contain rounded-lg border"
                />
                <Button
                  variant="ghost"
                  size="icon"
                  className="absolute top-2 right-2"
                  onClick={resetState}
                >
                  <X className="h-4 w-4" />
                </Button>
              </div>

              {uploadMutation.isPending || isParsing ? (
                <div className="flex items-center justify-center gap-2 py-4">
                  <Loader2 className="h-5 w-5 animate-spin" />
                  <span>{uploadMutation.isPending ? 'Uploading...' : 'Analyzing receipt...'}</span>
                </div>
              ) : parsedData ? (
                <div className="space-y-3 p-4 bg-muted rounded-lg">
                  <h4 className="font-medium">Extracted Data</h4>
                  
                  <div className="grid grid-cols-2 gap-2 text-sm">
                    <div>
                      <span className="text-muted-foreground">Merchant:</span>
                      <p className="font-medium">{parsedData.merchantName || 'Unknown'}</p>
                    </div>
                    <div>
                      <span className="text-muted-foreground">Date:</span>
                      <p className="font-medium">{parsedData.date || 'Not detected'}</p>
                    </div>
                    <div>
                      <span className="text-muted-foreground">Total:</span>
                      <p className="font-medium text-lg">
                        {parsedData.total ? `$${parsedData.total.toFixed(2)}` : 'Not detected'}
                      </p>
                    </div>
                    <div>
                      <span className="text-muted-foreground">Confidence:</span>
                      <p className={cn(
                        "font-medium",
                        parsedData.confidence > 0.8 ? "text-green-600" : 
                        parsedData.confidence > 0.5 ? "text-yellow-600" : "text-red-600"
                      )}>
                        {Math.round(parsedData.confidence * 100)}%
                      </p>
                    </div>
                  </div>

                  {parsedData.items && parsedData.items.length > 0 && (
                    <div>
                      <span className="text-muted-foreground text-sm">Items:</span>
                      <ul className="mt-1 space-y-1">
                        {parsedData.items.slice(0, 5).map((item, i) => (
                          <li key={i} className="text-sm flex justify-between">
                            <span>{item.description}</span>
                            <span>${item.totalPrice?.toFixed(2)}</span>
                          </li>
                        ))}
                        {parsedData.items.length > 5 && (
                          <li className="text-sm text-muted-foreground">
                            +{parsedData.items.length - 5} more items
                          </li>
                        )}
                      </ul>
                    </div>
                  )}

                  <div className="flex gap-2 pt-2">
                    <Button variant="outline" className="flex-1" onClick={resetState}>
                      Try Again
                    </Button>
                    <Button className="flex-1" onClick={handleApply}>
                      <Check className="h-4 w-4 mr-2" />
                      Use This Data
                    </Button>
                  </div>
                </div>
              ) : null}
            </div>
          )}
        </DialogContent>
      </Dialog>
    </>
  );
}

function guessCategory(merchantName?: string): string {
  if (!merchantName) return 'OTHER';
  
  const lower = merchantName.toLowerCase();
  if (/restaurant|cafe|coffee|pizza|burger|sushi|bar|grill/.test(lower)) return 'FOOD';
  if (/uber|lyft|taxi|gas|parking|transit/.test(lower)) return 'TRANSPORTATION';
  if (/walmart|target|amazon|costco/.test(lower)) return 'SHOPPING';
  if (/hotel|airbnb|inn|motel/.test(lower)) return 'ACCOMMODATION';
  
  return 'OTHER';
}
```

---

## Sprint 4.4: Payment Integrations (Week 15-16)

### 4.4.1 Payment Service

**Objective:** Integrate payment providers for in-app settlements

**New Service Structure:**
```
services/payment-service/
├── src/main/java/com/splitter/payment/
│   ├── PaymentServiceApplication.java
│   ├── config/
│   │   ├── StripeConfig.java
│   │   ├── PayPalConfig.java
│   │   └── VenmoConfig.java
│   ├── controller/
│   │   ├── PaymentController.java
│   │   └── WebhookController.java
│   ├── service/
│   │   ├── PaymentService.java
│   │   ├── PaymentProviderFactory.java
│   │   └── PayoutService.java
│   ├── provider/
│   │   ├── PaymentProvider.java
│   │   ├── StripePaymentProvider.java
│   │   ├── PayPalPaymentProvider.java
│   │   └── VenmoPaymentProvider.java
│   ├── model/
│   │   ├── Payment.java
│   │   ├── PaymentMethod.java
│   │   ├── PaymentIntent.java
│   │   └── Payout.java
│   └── webhook/
│       ├── WebhookHandler.java
│       ├── StripeWebhookHandler.java
│       └── PayPalWebhookHandler.java
```

**Payment Provider Interface:**
```java
public interface PaymentProvider {
    
    String getProviderId();
    
    Mono<PaymentIntent> createPaymentIntent(PaymentRequest request);
    
    Mono<Payment> processPayment(String paymentIntentId);
    
    Mono<Payout> createPayout(PayoutRequest request);
    
    Mono<PaymentStatus> getPaymentStatus(String paymentId);
    
    Mono<Void> handleWebhook(String payload, String signature);
    
    boolean supportsP2P();
}

@Component
@RequiredArgsConstructor
public class StripePaymentProvider implements PaymentProvider {
    
    private final StripeClient stripeClient;
    private final PaymentRepository paymentRepository;
    
    @Override
    public String getProviderId() {
        return "stripe";
    }
    
    @Override
    public Mono<PaymentIntent> createPaymentIntent(PaymentRequest request) {
        return Mono.fromCallable(() -> {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(request.getAmountInCents())
                .setCurrency(request.getCurrency().toLowerCase())
                .setMetadata(Map.of(
                    "settlementId", request.getSettlementId().toString(),
                    "fromUserId", request.getFromUserId().toString(),
                    "toUserId", request.getToUserId().toString()
                ))
                .setTransferData(
                    PaymentIntentCreateParams.TransferData.builder()
                        .setDestination(request.getRecipientStripeAccountId())
                        .build()
                )
                .build();
            
            com.stripe.model.PaymentIntent stripeIntent = 
                com.stripe.model.PaymentIntent.create(params);
            
            return PaymentIntent.builder()
                .id(stripeIntent.getId())
                .clientSecret(stripeIntent.getClientSecret())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(PaymentStatus.PENDING)
                .build();
        }).subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<Void> handleWebhook(String payload, String signature) {
        return Mono.fromRunnable(() -> {
            Event event = Webhook.constructEvent(payload, signature, webhookSecret);
            
            switch (event.getType()) {
                case "payment_intent.succeeded":
                    handlePaymentSuccess(event);
                    break;
                case "payment_intent.payment_failed":
                    handlePaymentFailure(event);
                    break;
                case "payout.paid":
                    handlePayoutSuccess(event);
                    break;
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
```

### 4.4.2 Connected Accounts Setup

**Stripe Connect Onboarding:**
```java
@Service
@RequiredArgsConstructor
public class StripeConnectService {
    
    private final StripeClient stripeClient;
    private final UserPaymentAccountRepository accountRepository;
    
    public Mono<AccountLinkResponse> createConnectedAccount(UUID userId, String email) {
        return Mono.fromCallable(() -> {
            // Create Express connected account
            AccountCreateParams params = AccountCreateParams.builder()
                .setType(AccountCreateParams.Type.EXPRESS)
                .setEmail(email)
                .setCapabilities(
                    AccountCreateParams.Capabilities.builder()
                        .setTransfers(
                            AccountCreateParams.Capabilities.Transfers.builder()
                                .setRequested(true)
                                .build()
                        )
                        .build()
                )
                .build();
            
            Account account = Account.create(params);
            
            // Save account reference
            saveUserPaymentAccount(userId, account.getId());
            
            // Create account link for onboarding
            AccountLinkCreateParams linkParams = AccountLinkCreateParams.builder()
                .setAccount(account.getId())
                .setRefreshUrl(refreshUrl)
                .setReturnUrl(returnUrl)
                .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                .build();
            
            AccountLink link = AccountLink.create(linkParams);
            
            return AccountLinkResponse.builder()
                .accountId(account.getId())
                .onboardingUrl(link.getUrl())
                .build();
        }).subscribeOn(Schedulers.boundedElastic());
    }
    
    public Mono<Boolean> isAccountReady(String accountId) {
        return Mono.fromCallable(() -> {
            Account account = Account.retrieve(accountId);
            return account.getPayoutsEnabled() && account.getChargesEnabled();
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
```

**Frontend - Payment Setup:**
```typescript
// components/settings/payment-setup.tsx
'use client';

import { useState } from 'react';
import { CreditCard, CheckCircle, ExternalLink, Loader2 } from 'lucide-react';
import { Button, Card, CardContent, CardHeader, CardTitle, Alert } from '@/components/ui';
import { usePaymentAccounts, useConnectStripe } from '@/lib/hooks/usePayments';

export function PaymentSetup() {
  const { data: accounts, isLoading } = usePaymentAccounts();
  const connectStripe = useConnectStripe();
  const [connecting, setConnecting] = useState(false);

  const stripeAccount = accounts?.find(a => a.provider === 'stripe');
  const paypalAccount = accounts?.find(a => a.provider === 'paypal');
  const venmoAccount = accounts?.find(a => a.provider === 'venmo');

  const handleConnectStripe = async () => {
    setConnecting(true);
    try {
      const result = await connectStripe.mutateAsync();
      // Redirect to Stripe onboarding
      window.location.href = result.onboardingUrl;
    } catch (error) {
      console.error('Failed to connect Stripe:', error);
      setConnecting(false);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h3 className="text-lg font-semibold">Payment Methods</h3>
        <p className="text-muted-foreground">
          Connect payment accounts to send and receive settlements directly in Splitter.
        </p>
      </div>

      <div className="grid gap-4">
        {/* Stripe Connect */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-base font-medium flex items-center gap-2">
              <CreditCard className="h-5 w-5" />
              Card Payments (Stripe)
            </CardTitle>
            {stripeAccount?.isActive ? (
              <span className="flex items-center text-sm text-green-600">
                <CheckCircle className="h-4 w-4 mr-1" />
                Connected
              </span>
            ) : null}
          </CardHeader>
          <CardContent>
            {stripeAccount?.isActive ? (
              <div className="text-sm text-muted-foreground">
                <p>You can receive payments directly to your bank account.</p>
                <Button variant="link" className="p-0 h-auto" asChild>
                  <a href="/settings/payments/stripe" className="flex items-center">
                    Manage account <ExternalLink className="h-3 w-3 ml-1" />
                  </a>
                </Button>
              </div>
            ) : (
              <div className="space-y-3">
                <p className="text-sm text-muted-foreground">
                  Connect Stripe to receive card payments and bank transfers.
                </p>
                <Button onClick={handleConnectStripe} disabled={connecting}>
                  {connecting ? (
                    <>
                      <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                      Connecting...
                    </>
                  ) : (
                    'Connect Stripe'
                  )}
                </Button>
              </div>
            )}
          </CardContent>
        </Card>

        {/* PayPal */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-base font-medium flex items-center gap-2">
              <PayPalIcon className="h-5 w-5" />
              PayPal
            </CardTitle>
            {paypalAccount?.isActive ? (
              <span className="flex items-center text-sm text-green-600">
                <CheckCircle className="h-4 w-4 mr-1" />
                {paypalAccount.email}
              </span>
            ) : null}
          </CardHeader>
          <CardContent>
            {paypalAccount?.isActive ? (
              <Button variant="outline" size="sm">
                Disconnect
              </Button>
            ) : (
              <Button variant="outline">
                Connect PayPal
              </Button>
            )}
          </CardContent>
        </Card>

        {/* Venmo */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-base font-medium flex items-center gap-2">
              <VenmoIcon className="h-5 w-5" />
              Venmo
            </CardTitle>
            {venmoAccount?.isActive ? (
              <span className="flex items-center text-sm text-green-600">
                <CheckCircle className="h-4 w-4 mr-1" />
                @{venmoAccount.username}
              </span>
            ) : null}
          </CardHeader>
          <CardContent>
            {venmoAccount?.isActive ? (
              <Button variant="outline" size="sm">
                Disconnect
              </Button>
            ) : (
              <Button variant="outline">
                Connect Venmo
              </Button>
            )}
          </CardContent>
        </Card>
      </div>

      <Alert>
        <AlertDescription>
          Payment processing is powered by Stripe. Your payment information is securely handled 
          and never stored on our servers.
        </AlertDescription>
      </Alert>
    </div>
  );
}
```

---

## Sprint 4.5: Mobile Applications (Week 16-17)

### 4.5.1 React Native Setup

**Objective:** Build cross-platform mobile apps using React Native

**Project Structure:**
```
mobile/
├── package.json
├── app.json
├── babel.config.js
├── metro.config.js
├── tsconfig.json
├── src/
│   ├── App.tsx
│   ├── navigation/
│   │   ├── AppNavigator.tsx
│   │   ├── AuthNavigator.tsx
│   │   └── MainNavigator.tsx
│   ├── screens/
│   │   ├── auth/
│   │   │   ├── LoginScreen.tsx
│   │   │   └── RegisterScreen.tsx
│   │   ├── dashboard/
│   │   │   └── DashboardScreen.tsx
│   │   ├── groups/
│   │   │   ├── GroupsListScreen.tsx
│   │   │   ├── GroupDetailScreen.tsx
│   │   │   └── CreateGroupScreen.tsx
│   │   ├── expenses/
│   │   │   ├── AddExpenseScreen.tsx
│   │   │   └── ExpenseDetailScreen.tsx
│   │   ├── balances/
│   │   │   └── BalancesScreen.tsx
│   │   └── settings/
│   │       └── SettingsScreen.tsx
│   ├── components/
│   │   ├── ui/
│   │   │   ├── Button.tsx
│   │   │   ├── Input.tsx
│   │   │   ├── Card.tsx
│   │   │   └── Avatar.tsx
│   │   ├── expenses/
│   │   ├── groups/
│   │   └── balances/
│   ├── lib/
│   │   ├── api/
│   │   ├── hooks/
│   │   ├── stores/
│   │   └── utils/
│   └── theme/
│       ├── colors.ts
│       ├── spacing.ts
│       └── typography.ts
├── ios/
└── android/
```

**Package.json:**
```json
{
  "name": "splitter-mobile",
  "version": "1.0.0",
  "scripts": {
    "start": "expo start",
    "android": "expo start --android",
    "ios": "expo start --ios",
    "build:android": "eas build --platform android",
    "build:ios": "eas build --platform ios"
  },
  "dependencies": {
    "expo": "~50.0.0",
    "expo-camera": "~14.0.0",
    "expo-image-picker": "~14.7.0",
    "expo-notifications": "~0.27.0",
    "expo-secure-store": "~12.8.0",
    "react": "18.2.0",
    "react-native": "0.73.0",
    "@react-navigation/native": "^6.1.0",
    "@react-navigation/native-stack": "^6.9.0",
    "@react-navigation/bottom-tabs": "^6.5.0",
    "@tanstack/react-query": "^5.0.0",
    "zustand": "^4.5.0",
    "react-hook-form": "^7.50.0",
    "zod": "^3.22.0",
    "@hookform/resolvers": "^3.3.0",
    "axios": "^1.6.0",
    "date-fns": "^3.0.0"
  },
  "devDependencies": {
    "@types/react": "~18.2.0",
    "typescript": "^5.3.0"
  }
}
```

### 4.5.2 Core Mobile Screens

**App Navigator:**
```typescript
// src/navigation/AppNavigator.tsx
import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { useAuthStore } from '@/lib/stores/authStore';
import { AuthNavigator } from './AuthNavigator';
import { MainNavigator } from './MainNavigator';
import { SplashScreen } from '@/screens/SplashScreen';

const Stack = createNativeStackNavigator();

export function AppNavigator() {
  const { isAuthenticated, isLoading } = useAuthStore();

  if (isLoading) {
    return <SplashScreen />;
  }

  return (
    <NavigationContainer>
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        {isAuthenticated ? (
          <Stack.Screen name="Main" component={MainNavigator} />
        ) : (
          <Stack.Screen name="Auth" component={AuthNavigator} />
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
}
```

**Dashboard Screen:**
```typescript
// src/screens/dashboard/DashboardScreen.tsx
import React from 'react';
import { View, ScrollView, RefreshControl, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import { Text, Card, Avatar, Button } from '@/components/ui';
import { BalanceSummary } from '@/components/balances/BalanceSummary';
import { RecentExpenses } from '@/components/expenses/RecentExpenses';
import { useDashboard } from '@/lib/hooks/useDashboard';
import { useAuthStore } from '@/lib/stores/authStore';
import { colors, spacing } from '@/theme';

export function DashboardScreen() {
  const navigation = useNavigation();
  const { user } = useAuthStore();
  const { data, isLoading, refetch } = useDashboard();

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView
        contentContainerStyle={styles.content}
        refreshControl={
          <RefreshControl refreshing={isLoading} onRefresh={refetch} />
        }
      >
        {/* Header */}
        <View style={styles.header}>
          <View>
            <Text variant="body" color="muted">Welcome back,</Text>
            <Text variant="h2">{user?.displayName}</Text>
          </View>
          <Avatar source={{ uri: user?.avatarUrl }} size={48} />
        </View>

        {/* Overall Balance */}
        <Card style={styles.balanceCard}>
          <Text variant="body" color="muted">Overall Balance</Text>
          <Text 
            variant="h1" 
            style={[
              styles.balanceAmount,
              { color: data?.overallBalance >= 0 ? colors.success : colors.error }
            ]}
          >
            {data?.overallBalance >= 0 ? '+' : ''}
            ${Math.abs(data?.overallBalance || 0).toFixed(2)}
          </Text>
          <View style={styles.balanceDetails}>
            <View style={styles.balanceItem}>
              <Text variant="caption" color="muted">You are owed</Text>
              <Text variant="body" color="success">
                ${data?.youAreOwed?.toFixed(2) || '0.00'}
              </Text>
            </View>
            <View style={styles.balanceItem}>
              <Text variant="caption" color="muted">You owe</Text>
              <Text variant="body" color="error">
                ${data?.youOwe?.toFixed(2) || '0.00'}
              </Text>
            </View>
          </View>
        </Card>

        {/* Quick Actions */}
        <View style={styles.quickActions}>
          <Button
            icon="plus"
            onPress={() => navigation.navigate('AddExpense')}
            style={styles.quickAction}
          >
            Add Expense
          </Button>
          <Button
            icon="users"
            variant="outline"
            onPress={() => navigation.navigate('CreateGroup')}
            style={styles.quickAction}
          >
            New Group
          </Button>
        </View>

        {/* Balance Summary by Group */}
        <BalanceSummary balances={data?.groupBalances || []} />

        {/* Recent Expenses */}
        <RecentExpenses expenses={data?.recentExpenses || []} />
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  content: {
    padding: spacing.md,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.lg,
  },
  balanceCard: {
    padding: spacing.lg,
    marginBottom: spacing.md,
  },
  balanceAmount: {
    marginVertical: spacing.sm,
  },
  balanceDetails: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: spacing.sm,
  },
  balanceItem: {
    alignItems: 'center',
  },
  quickActions: {
    flexDirection: 'row',
    gap: spacing.sm,
    marginBottom: spacing.lg,
  },
  quickAction: {
    flex: 1,
  },
});
```

**Add Expense Screen with Camera:**
```typescript
// src/screens/expenses/AddExpenseScreen.tsx
import React, { useState } from 'react';
import { View, ScrollView, StyleSheet, Alert } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation, useRoute } from '@react-navigation/native';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as ImagePicker from 'expo-image-picker';
import {
  Text, Input, Button, Select, Card, CurrencyInput,
} from '@/components/ui';
import { SplitSelector } from '@/components/expenses/SplitSelector';
import { GroupSelector } from '@/components/groups/GroupSelector';
import { useCreateExpense, useUploadReceipt } from '@/lib/hooks/useExpenses';
import { expenseSchema, ExpenseFormData } from '@/lib/schemas/expense';
import { colors, spacing } from '@/theme';

export function AddExpenseScreen() {
  const navigation = useNavigation();
  const route = useRoute();
  const [isScanning, setIsScanning] = useState(false);
  
  const createExpense = useCreateExpense();
  const uploadReceipt = useUploadReceipt();
  
  const { control, handleSubmit, setValue, watch, formState: { errors } } = useForm<ExpenseFormData>({
    resolver: zodResolver(expenseSchema),
    defaultValues: {
      groupId: route.params?.groupId,
      splitType: 'EQUAL',
      currency: 'USD',
      date: new Date().toISOString().split('T')[0],
    },
  });

  const handleScanReceipt = async () => {
    const permission = await ImagePicker.requestCameraPermissionsAsync();
    if (!permission.granted) {
      Alert.alert('Permission required', 'Camera access is needed to scan receipts');
      return;
    }

    const result = await ImagePicker.launchCameraAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      quality: 0.8,
    });

    if (!result.canceled && result.assets[0]) {
      setIsScanning(true);
      try {
        const parsed = await uploadReceipt.mutateAsync(result.assets[0]);
        
        // Auto-fill form with parsed data
        if (parsed.merchantName) setValue('description', parsed.merchantName);
        if (parsed.total) setValue('amount', parsed.total);
        if (parsed.date) setValue('date', parsed.date);
        
        Alert.alert('Receipt Scanned', `Detected: ${parsed.merchantName} - $${parsed.total}`);
      } catch (error) {
        Alert.alert('Scan Failed', 'Could not read the receipt. Please enter details manually.');
      } finally {
        setIsScanning(false);
      }
    }
  };

  const onSubmit = async (data: ExpenseFormData) => {
    try {
      await createExpense.mutateAsync(data);
      navigation.goBack();
    } catch (error) {
      Alert.alert('Error', 'Failed to create expense');
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content}>
        <Text variant="h2" style={styles.title}>Add Expense</Text>

        {/* Scan Receipt Button */}
        <Button
          icon="camera"
          variant="outline"
          onPress={handleScanReceipt}
          loading={isScanning}
          style={styles.scanButton}
        >
          {isScanning ? 'Scanning...' : 'Scan Receipt'}
        </Button>

        {/* Group Selector */}
        <Controller
          control={control}
          name="groupId"
          render={({ field: { onChange, value } }) => (
            <GroupSelector
              value={value}
              onChange={onChange}
              error={errors.groupId?.message}
            />
          )}
        />

        {/* Description */}
        <Controller
          control={control}
          name="description"
          render={({ field: { onChange, onBlur, value } }) => (
            <Input
              label="Description"
              placeholder="What was this expense for?"
              value={value}
              onChangeText={onChange}
              onBlur={onBlur}
              error={errors.description?.message}
            />
          )}
        />

        {/* Amount */}
        <Controller
          control={control}
          name="amount"
          render={({ field: { onChange, value } }) => (
            <CurrencyInput
              label="Amount"
              value={value}
              onChangeValue={onChange}
              currency={watch('currency')}
              error={errors.amount?.message}
            />
          )}
        />

        {/* Split Type */}
        <Controller
          control={control}
          name="splitType"
          render={({ field: { onChange, value } }) => (
            <SplitSelector
              value={value}
              onChange={onChange}
              groupId={watch('groupId')}
            />
          )}
        />

        {/* Category */}
        <Controller
          control={control}
          name="category"
          render={({ field: { onChange, value } }) => (
            <Select
              label="Category"
              value={value}
              onChange={onChange}
              options={[
                { label: 'Food & Drink', value: 'FOOD' },
                { label: 'Transportation', value: 'TRANSPORTATION' },
                { label: 'Shopping', value: 'SHOPPING' },
                { label: 'Entertainment', value: 'ENTERTAINMENT' },
                { label: 'Accommodation', value: 'ACCOMMODATION' },
                { label: 'Other', value: 'OTHER' },
              ]}
            />
          )}
        />

        {/* Submit Button */}
        <Button
          onPress={handleSubmit(onSubmit)}
          loading={createExpense.isPending}
          style={styles.submitButton}
        >
          Add Expense
        </Button>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  content: {
    padding: spacing.md,
  },
  title: {
    marginBottom: spacing.lg,
  },
  scanButton: {
    marginBottom: spacing.lg,
  },
  submitButton: {
    marginTop: spacing.lg,
  },
});
```

### 4.5.3 Push Notifications

**Push Notification Setup:**
```typescript
// src/lib/notifications/pushNotifications.ts
import * as Notifications from 'expo-notifications';
import * as Device from 'expo-device';
import { Platform } from 'react-native';
import { api } from '@/lib/api';

Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowAlert: true,
    shouldPlaySound: true,
    shouldSetBadge: true,
  }),
});

export async function registerForPushNotifications() {
  if (!Device.isDevice) {
    console.log('Push notifications require a physical device');
    return null;
  }

  const { status: existingStatus } = await Notifications.getPermissionsAsync();
  let finalStatus = existingStatus;

  if (existingStatus !== 'granted') {
    const { status } = await Notifications.requestPermissionsAsync();
    finalStatus = status;
  }

  if (finalStatus !== 'granted') {
    console.log('Failed to get push notification permissions');
    return null;
  }

  const token = await Notifications.getExpoPushTokenAsync({
    projectId: 'your-project-id',
  });

  // Register token with backend
  await api.post('/api/v1/users/me/push-token', {
    token: token.data,
    platform: Platform.OS,
  });

  if (Platform.OS === 'android') {
    await Notifications.setNotificationChannelAsync('default', {
      name: 'default',
      importance: Notifications.AndroidImportance.MAX,
      vibrationPattern: [0, 250, 250, 250],
      lightColor: '#FF231F7C',
    });
  }

  return token;
}

export function useNotificationListener() {
  const notificationListener = useRef<Notifications.Subscription>();
  const responseListener = useRef<Notifications.Subscription>();

  useEffect(() => {
    notificationListener.current = Notifications.addNotificationReceivedListener(
      notification => {
        console.log('Notification received:', notification);
      }
    );

    responseListener.current = Notifications.addNotificationResponseReceivedListener(
      response => {
        const data = response.notification.request.content.data;
        handleNotificationNavigation(data);
      }
    );

    return () => {
      if (notificationListener.current) {
        Notifications.removeNotificationSubscription(notificationListener.current);
      }
      if (responseListener.current) {
        Notifications.removeNotificationSubscription(responseListener.current);
      }
    };
  }, []);
}

function handleNotificationNavigation(data: any) {
  if (data.type === 'expense_added') {
    navigation.navigate('ExpenseDetail', { id: data.expenseId });
  } else if (data.type === 'settlement_request') {
    navigation.navigate('Balances', { groupId: data.groupId });
  } else if (data.type === 'group_invitation') {
    navigation.navigate('GroupDetail', { id: data.groupId });
  }
}
```

---

## Sprint 4.6: Analytics Dashboard (Week 17-18)

### 4.6.1 Analytics Service

**Objective:** Provide spending insights and reports

**Analytics Endpoints:**
```java
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    
    private final AnalyticsService analyticsService;
    
    @GetMapping("/spending/summary")
    public Mono<SpendingSummary> getSpendingSummary(
            @RequestParam(required = false) UUID groupId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal user) {
        return analyticsService.getSpendingSummary(user.getUserId(), groupId, startDate, endDate);
    }
    
    @GetMapping("/spending/by-category")
    public Flux<CategorySpending> getSpendingByCategory(
            @RequestParam(required = false) UUID groupId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal user) {
        return analyticsService.getSpendingByCategory(user.getUserId(), groupId, startDate, endDate);
    }
    
    @GetMapping("/spending/trend")
    public Flux<SpendingTrend> getSpendingTrend(
            @RequestParam(required = false) UUID groupId,
            @RequestParam String interval,  // DAILY, WEEKLY, MONTHLY
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal user) {
        return analyticsService.getSpendingTrend(user.getUserId(), groupId, 
            TrendInterval.valueOf(interval), startDate, endDate);
    }
    
    @GetMapping("/balances/history")
    public Flux<BalanceHistory> getBalanceHistory(
            @RequestParam UUID groupId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal user) {
        return analyticsService.getBalanceHistory(user.getUserId(), groupId, startDate, endDate);
    }
    
    @GetMapping("/export")
    public Mono<ResponseEntity<byte[]>> exportData(
            @RequestParam UUID groupId,
            @RequestParam String format,  // CSV, PDF
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal user) {
        return analyticsService.exportData(user.getUserId(), groupId, format, startDate, endDate)
            .map(data -> ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=splitter-export." + format.toLowerCase())
                .contentType(format.equals("PDF") ? MediaType.APPLICATION_PDF : 
                    MediaType.parseMediaType("text/csv"))
                .body(data));
    }
}
```

### 4.6.2 Analytics Frontend

**Spending Insights Page:**
```typescript
// app/analytics/page.tsx
'use client';

import { useState } from 'react';
import { subMonths, format } from 'date-fns';
import {
  Card, CardContent, CardHeader, CardTitle,
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
  DateRangePicker,
  Tabs, TabsContent, TabsList, TabsTrigger,
} from '@/components/ui';
import { SpendingChart } from '@/components/analytics/SpendingChart';
import { CategoryBreakdown } from '@/components/analytics/CategoryBreakdown';
import { TrendChart } from '@/components/analytics/TrendChart';
import { TopExpenses } from '@/components/analytics/TopExpenses';
import { ExportButton } from '@/components/analytics/ExportButton';
import { useSpendingSummary, useSpendingByCategory, useSpendingTrend } from '@/lib/hooks/useAnalytics';
import { useGroups } from '@/lib/hooks/useGroups';

export default function AnalyticsPage() {
  const [dateRange, setDateRange] = useState({
    from: subMonths(new Date(), 1),
    to: new Date(),
  });
  const [selectedGroup, setSelectedGroup] = useState<string>('all');

  const { data: groups } = useGroups();
  const { data: summary, isLoading: summaryLoading } = useSpendingSummary({
    groupId: selectedGroup !== 'all' ? selectedGroup : undefined,
    startDate: format(dateRange.from, 'yyyy-MM-dd'),
    endDate: format(dateRange.to, 'yyyy-MM-dd'),
  });
  const { data: categoryData } = useSpendingByCategory({
    groupId: selectedGroup !== 'all' ? selectedGroup : undefined,
    startDate: format(dateRange.from, 'yyyy-MM-dd'),
    endDate: format(dateRange.to, 'yyyy-MM-dd'),
  });
  const { data: trendData } = useSpendingTrend({
    groupId: selectedGroup !== 'all' ? selectedGroup : undefined,
    interval: 'WEEKLY',
    startDate: format(dateRange.from, 'yyyy-MM-dd'),
    endDate: format(dateRange.to, 'yyyy-MM-dd'),
  });

  return (
    <div className="container py-6 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Spending Insights</h1>
          <p className="text-muted-foreground">
            Analyze your spending patterns and trends
          </p>
        </div>
        <ExportButton groupId={selectedGroup} dateRange={dateRange} />
      </div>

      {/* Filters */}
      <div className="flex gap-4 items-center">
        <Select value={selectedGroup} onValueChange={setSelectedGroup}>
          <SelectTrigger className="w-[200px]">
            <SelectValue placeholder="All Groups" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All Groups</SelectItem>
            {groups?.map(group => (
              <SelectItem key={group.id} value={group.id}>
                {group.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <DateRangePicker
          value={dateRange}
          onChange={setDateRange}
        />
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Total Spent
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              ${summary?.totalSpent?.toFixed(2) || '0.00'}
            </div>
            <p className="text-xs text-muted-foreground">
              {summary?.expenseCount || 0} expenses
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Your Share
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              ${summary?.yourShare?.toFixed(2) || '0.00'}
            </div>
            <p className="text-xs text-muted-foreground">
              {((summary?.yourShare / summary?.totalSpent) * 100 || 0).toFixed(0)}% of total
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Average Expense
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              ${summary?.averageExpense?.toFixed(2) || '0.00'}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Top Category
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {summary?.topCategory || '-'}
            </div>
            <p className="text-xs text-muted-foreground">
              ${summary?.topCategoryAmount?.toFixed(2) || '0.00'}
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Charts */}
      <Tabs defaultValue="trend" className="space-y-4">
        <TabsList>
          <TabsTrigger value="trend">Spending Trend</TabsTrigger>
          <TabsTrigger value="categories">By Category</TabsTrigger>
          <TabsTrigger value="top">Top Expenses</TabsTrigger>
        </TabsList>

        <TabsContent value="trend">
          <Card>
            <CardHeader>
              <CardTitle>Spending Over Time</CardTitle>
            </CardHeader>
            <CardContent>
              <TrendChart data={trendData || []} />
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="categories">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Card>
              <CardHeader>
                <CardTitle>Category Breakdown</CardTitle>
              </CardHeader>
              <CardContent>
                <CategoryBreakdown data={categoryData || []} type="pie" />
              </CardContent>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle>Category Details</CardTitle>
              </CardHeader>
              <CardContent>
                <CategoryBreakdown data={categoryData || []} type="bar" />
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="top">
          <Card>
            <CardHeader>
              <CardTitle>Top Expenses</CardTitle>
            </CardHeader>
            <CardContent>
              <TopExpenses 
                groupId={selectedGroup !== 'all' ? selectedGroup : undefined}
                dateRange={dateRange}
              />
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}
```

---

## Sprint 4.7: Performance & Scale (Week 18)

### 4.7.1 Caching Strategy

**Multi-Level Caching:**
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager(ReactiveRedisConnectionFactory factory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(15))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
            "users", defaultConfig.entryTtl(Duration.ofHours(1)),
            "groups", defaultConfig.entryTtl(Duration.ofMinutes(30)),
            "balances", defaultConfig.entryTtl(Duration.ofMinutes(5)),
            "exchangeRates", defaultConfig.entryTtl(Duration.ofHours(1))
        );
        
        return RedisCacheManager.builder(factory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .build();
    }
}

@Service
public class CachedBalanceService {
    
    private final BalanceRepository balanceRepository;
    private final ReactiveRedisTemplate<String, GroupBalances> redisTemplate;
    private final BalanceCalculator balanceCalculator;
    
    private static final String BALANCE_KEY_PREFIX = "balance:group:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    
    public Mono<GroupBalances> getGroupBalances(UUID groupId) {
        String cacheKey = BALANCE_KEY_PREFIX + groupId;
        
        return redisTemplate.opsForValue().get(cacheKey)
            .switchIfEmpty(
                calculateAndCacheBalances(groupId, cacheKey)
            );
    }
    
    public Mono<Void> invalidateGroupBalances(UUID groupId) {
        String cacheKey = BALANCE_KEY_PREFIX + groupId;
        return redisTemplate.delete(cacheKey).then();
    }
    
    private Mono<GroupBalances> calculateAndCacheBalances(UUID groupId, String cacheKey) {
        return balanceCalculator.calculateGroupBalances(groupId)
            .flatMap(balances -> 
                redisTemplate.opsForValue().set(cacheKey, balances, CACHE_TTL)
                    .thenReturn(balances)
            );
    }
}
```

### 4.7.2 Read Replicas for Reporting

**Read Replica Configuration:**
```yaml
# application-production.yml
spring:
  r2dbc:
    # Primary for writes
    url: r2dbc:postgresql://${POSTGRES_PRIMARY_HOST}:5432/${POSTGRES_DB}
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}
    
  datasource:
    # Read replica for analytics queries
    read-replica:
      url: r2dbc:postgresql://${POSTGRES_REPLICA_HOST}:5432/${POSTGRES_DB}
      username: ${POSTGRES_REPLICA_USER}
      password: ${POSTGRES_REPLICA_PASSWORD}
```

**Routing Configuration:**
```java
@Configuration
public class DatabaseRoutingConfig {
    
    @Bean
    @Primary
    public ConnectionFactory primaryConnectionFactory(
            @Value("${spring.r2dbc.url}") String url,
            @Value("${spring.r2dbc.username}") String username,
            @Value("${spring.r2dbc.password}") String password) {
        return ConnectionFactories.get(ConnectionFactoryOptions.builder()
            .option(DRIVER, "postgresql")
            .from(ConnectionFactoryOptions.parse(url))
            .option(USER, username)
            .option(PASSWORD, password)
            .build());
    }
    
    @Bean
    @Qualifier("readReplicaConnectionFactory")
    public ConnectionFactory readReplicaConnectionFactory(
            @Value("${spring.datasource.read-replica.url}") String url,
            @Value("${spring.datasource.read-replica.username}") String username,
            @Value("${spring.datasource.read-replica.password}") String password) {
        return ConnectionFactories.get(ConnectionFactoryOptions.builder()
            .option(DRIVER, "postgresql")
            .from(ConnectionFactoryOptions.parse(url))
            .option(USER, username)
            .option(PASSWORD, password)
            .build());
    }
}

@Repository
public class AnalyticsRepositoryImpl {
    
    private final DatabaseClient readReplicaClient;
    
    public AnalyticsRepositoryImpl(
            @Qualifier("readReplicaConnectionFactory") ConnectionFactory connectionFactory) {
        this.readReplicaClient = DatabaseClient.create(connectionFactory);
    }
    
    public Flux<CategorySpending> getSpendingByCategory(UUID userId, LocalDate start, LocalDate end) {
        return readReplicaClient.sql("""
            SELECT category, SUM(amount) as total, COUNT(*) as count
            FROM expenses e
            JOIN expense_participants ep ON e.id = ep.expense_id
            WHERE ep.user_id = :userId
            AND e.date BETWEEN :start AND :end
            GROUP BY category
            ORDER BY total DESC
            """)
            .bind("userId", userId)
            .bind("start", start)
            .bind("end", end)
            .map(row -> new CategorySpending(
                row.get("category", String.class),
                row.get("total", BigDecimal.class),
                row.get("count", Long.class)
            ))
            .all();
    }
}
```

### 4.7.3 CDN for Static Assets

**CloudFront Configuration (Terraform):**
```hcl
# infrastructure/terraform/cdn.tf

resource "aws_cloudfront_distribution" "frontend" {
  origin {
    domain_name = aws_s3_bucket.frontend.bucket_regional_domain_name
    origin_id   = "S3-Frontend"

    s3_origin_config {
      origin_access_identity = aws_cloudfront_origin_access_identity.frontend.cloudfront_access_identity_path
    }
  }

  enabled             = true
  is_ipv6_enabled     = true
  default_root_object = "index.html"

  aliases = ["app.splitter.example.com"]

  default_cache_behavior {
    allowed_methods  = ["GET", "HEAD", "OPTIONS"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = "S3-Frontend"

    forwarded_values {
      query_string = false
      cookies {
        forward = "none"
      }
    }

    viewer_protocol_policy = "redirect-to-https"
    min_ttl                = 0
    default_ttl            = 3600
    max_ttl                = 86400
    compress               = true
  }

  # Cache static assets longer
  ordered_cache_behavior {
    path_pattern     = "/_next/static/*"
    allowed_methods  = ["GET", "HEAD"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = "S3-Frontend"

    forwarded_values {
      query_string = false
      cookies {
        forward = "none"
      }
    }

    viewer_protocol_policy = "redirect-to-https"
    min_ttl                = 86400
    default_ttl            = 604800
    max_ttl                = 31536000
    compress               = true
  }

  # SPA routing - serve index.html for all paths
  custom_error_response {
    error_code         = 404
    response_code      = 200
    response_page_path = "/index.html"
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    acm_certificate_arn      = aws_acm_certificate.frontend.arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }

  tags = {
    Name        = "splitter-frontend-cdn"
    Environment = "production"
  }
}
```

---

## Deliverables Summary

### Multi-Currency (Sprint 4.1)
- [ ] Currency service with exchange rate caching
- [ ] Support for 100+ currencies
- [ ] Automatic conversion in expense creation
- [ ] Currency selector component

### Recurring Expenses (Sprint 4.2)
- [ ] Recurring expense model and scheduler
- [ ] Frequency options (daily, weekly, monthly, yearly)
- [ ] Recurring expense management UI

### Receipt Scanning (Sprint 4.3)
- [ ] OCR integration (Google Vision or AWS Textract)
- [ ] Receipt parsing and data extraction
- [ ] Camera capture in mobile and web

### Payment Integrations (Sprint 4.4)
- [ ] Stripe Connect for card payments
- [ ] PayPal integration
- [ ] Venmo deep linking
- [ ] Payment onboarding flow

### Mobile Applications (Sprint 4.5)
- [ ] React Native app structure
- [ ] Core screens (Dashboard, Groups, Expenses, Balances)
- [ ] Push notification support
- [ ] Receipt scanning via camera

### Analytics Dashboard (Sprint 4.6)
- [ ] Spending summary and trends
- [ ] Category breakdown charts
- [ ] Data export (CSV, PDF)
- [ ] Balance history

### Performance & Scale (Sprint 4.7)
- [ ] Multi-level caching strategy
- [ ] Read replicas for reporting
- [ ] CDN for static assets
- [ ] Query optimization

---

## Success Criteria

1. **Multi-Currency** - Users can create expenses in any of 100+ supported currencies
2. **Recurring Expenses** - Scheduler generates expenses with 99.9% reliability
3. **Receipt Scanning** - 80%+ accuracy on receipt data extraction
4. **Payments** - Users can settle via Stripe with < 3% failure rate
5. **Mobile App** - iOS and Android apps published to app stores
6. **Analytics** - Dashboard loads in < 2 seconds for 1-year data range
7. **Performance** - API p95 latency < 200ms under 1000 RPS load

---

## Dependencies & Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| OCR accuracy varies by receipt quality | Medium | Provide manual correction, train ML model |
| Payment provider rate limits | High | Implement queuing, request throttling |
| Exchange rate API outages | Medium | Multiple providers, cached fallbacks |
| App store approval delays | High | Start submission early, follow guidelines |
| Mobile device fragmentation | Medium | Extensive testing on multiple devices |

---

## Timeline

| Week | Sprint | Focus |
|------|--------|-------|
| 13 | 4.1 | Multi-Currency Support |
| 13-14 | 4.2 | Recurring Expenses |
| 14-15 | 4.3 | Receipt Scanning |
| 15-16 | 4.4 | Payment Integrations |
| 16-17 | 4.5 | Mobile Applications |
| 17-18 | 4.6 | Analytics Dashboard |
| 18 | 4.7 | Performance & Scale |

**Total Duration:** 6 weeks
