# Splitter - Expense Sharing Application Architecture

## 📋 Project Overview

**Splitter** is a comprehensive expense-sharing application inspired by [Splitwise](https://www.splitwise.com/). It enables users to track shared expenses, manage group bills, settle debts, and maintain financial harmony with friends, family, and roommates.

---

## 🎯 Core Features

### MVP Features (Phase 1)
1. **User Management**
   - User registration and authentication (OAuth 2.0 / JWT)
   - Profile management
   - Friend connections

2. **Group Management**
   - Create/edit/delete groups (trips, households, events)
   - Add/remove group members
   - Group settings and permissions

3. **Expense Tracking**
   - Add expenses with description, amount, date
   - Assign payer and participants
   - Equal and unequal splits
   - Split by percentage or shares
   - Expense categories

4. **Balance Calculation**
   - Real-time balance tracking per user
   - Group balances and individual balances
   - Debt simplification algorithm
   - "Who owes whom" calculations

5. **Settlement**
   - Record cash payments
   - Mark debts as settled
   - Payment history

### Advanced Features (Phase 2)
1. **Multi-Currency Support** - 100+ currencies with conversion
2. **Receipt Scanning** - OCR for automatic expense entry
3. **Recurring Expenses** - Monthly bills, subscriptions
4. **Expense Itemization** - Split individual items differently
5. **Charts & Analytics** - Spending patterns, category breakdowns
6. **Payment Integrations** - Direct payment processing
7. **Offline Mode** - Local-first with cloud sync
8. **Push Notifications** - Expense alerts, payment reminders

---

## 🏗️ System Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENT LAYER                                    │
├─────────────────┬─────────────────┬─────────────────┬──────────────────────┤
│   Web App       │   iOS App       │   Android App   │   Public API         │
│   (React/Next)  │   (Swift)       │   (Kotlin)      │   (REST/GraphQL)     │
└────────┬────────┴────────┬────────┴────────┬────────┴──────────┬───────────┘
         │                 │                 │                   │
         └─────────────────┴────────┬────────┴───────────────────┘
                                    │
                        ┌───────────▼───────────┐
                        │     API Gateway       │
                        │   (Spring Cloud)      │
                        └───────────┬───────────┘
                                    │
         ┌──────────────────────────┼──────────────────────────┐
         │                          │                          │
┌────────▼────────┐     ┌───────────▼───────────┐   ┌─────────▼─────────┐
│  User Service   │     │   Expense Service     │   │  Group Service    │
│                 │     │                       │   │                   │
└────────┬────────┘     └───────────┬───────────┘   └─────────┬─────────┘
         │                          │                         │
         │              ┌───────────▼───────────┐             │
         │              │  Balance Service      │             │
         │              │  (Core Calculator)    │             │
         │              └───────────┬───────────┘             │
         │                          │                         │
         │              ┌───────────▼───────────┐             │
         │              │ Settlement Service    │             │
         │              └───────────┬───────────┘             │
         │                          │                         │
         └──────────────────────────┼─────────────────────────┘
                                    │
                        ┌───────────▼───────────┐
                        │     Message Broker    │
                        │       (Kafka)         │
                        └───────────┬───────────┘
                                    │
         ┌──────────────────────────┼──────────────────────────┐
         │                          │                          │
┌────────▼────────┐     ┌───────────▼───────────┐   ┌─────────▼─────────┐
│  Notification   │     │   Analytics Service   │   │   Sync Service    │
│    Service      │     │                       │   │                   │
└─────────────────┘     └───────────────────────┘   └───────────────────┘
```

---

## 🔧 Technology Stack

### Backend Services (Java 21+ with Spring WebFlux)

Following the [WebFlux Guidelines](.github/webflux-guidelines.md):

| Layer | Technology | Purpose |
|-------|------------|---------|
| **Framework** | Spring Boot 3.x + WebFlux | Reactive, non-blocking API |
| **Database** | PostgreSQL + R2DBC | Reactive database access (NO JPA/Hibernate) |
| **Cache** | Redis (Reactive) | Distributed caching |
| **Message Broker** | Apache Kafka | Event-driven communication |
| **API Gateway** | Spring Cloud Gateway | Routing, rate limiting, auth |
| **Service Discovery** | Consul/Eureka | Service registration |
| **Auth** | Keycloak / Auth0 | OAuth 2.0, JWT tokens |

### Frontend Applications

| Platform | Technology |
|----------|------------|
| **Web** | Next.js 14+ (React), TypeScript, TailwindCSS |
| **iOS** | Swift, SwiftUI |
| **Android** | Kotlin, Jetpack Compose |
| **Shared** | GraphQL client, React Query |

### Infrastructure

| Component | Technology |
|-----------|------------|
| **Containers** | Docker, Kubernetes |
| **CI/CD** | GitHub Actions |
| **Monitoring** | Prometheus, Grafana |
| **Logging** | ELK Stack (Elasticsearch, Logstash, Kibana) |
| **Tracing** | Jaeger / Zipkin |

---

## 📦 Microservices Architecture

Following the [Microservice Guidelines](.github/microservice-guidelines.md):

### Service Definitions

#### 1. User Service
```
user-service/
├── src/main/java/com/splitter/user/
│   ├── controller/
│   │   └── UserController.java
│   ├── service/
│   │   ├── UserService.java
│   │   └── FriendshipService.java
│   ├── repository/
│   │   └── UserRepository.java          # R2DBC Reactive Repository
│   ├── model/
│   │   ├── User.java
│   │   └── Friendship.java
│   ├── dto/
│   │   ├── UserDto.java
│   │   └── CreateUserRequest.java
│   └── event/
│       └── UserCreatedEvent.java
├── src/main/resources/
│   └── application.yml
└── src/test/
```

**Responsibilities:**
- User registration, authentication
- Profile management
- Friend connections and requests
- User preferences and settings

**API Endpoints:**
```
POST   /api/v1/users              - Register new user
GET    /api/v1/users/{id}         - Get user by ID
PUT    /api/v1/users/{id}         - Update user profile
DELETE /api/v1/users/{id}         - Delete user account
GET    /api/v1/users/{id}/friends - Get user's friends
POST   /api/v1/users/{id}/friends - Add friend
```

#### 2. Group Service
```
group-service/
├── src/main/java/com/splitter/group/
│   ├── controller/
│   │   └── GroupController.java
│   ├── service/
│   │   ├── GroupService.java
│   │   └── GroupMembershipService.java
│   ├── repository/
│   │   ├── GroupRepository.java
│   │   └── GroupMemberRepository.java
│   ├── model/
│   │   ├── Group.java
│   │   └── GroupMember.java
│   └── event/
│       ├── GroupCreatedEvent.java
│       └── MemberAddedEvent.java
└── ...
```

**Responsibilities:**
- Group CRUD operations
- Member management
- Group types (trip, household, couple, other)
- Group settings and permissions

**API Endpoints:**
```
POST   /api/v1/groups                    - Create group
GET    /api/v1/groups/{id}               - Get group details
PUT    /api/v1/groups/{id}               - Update group
DELETE /api/v1/groups/{id}               - Delete group
GET    /api/v1/groups/{id}/members       - List group members
POST   /api/v1/groups/{id}/members       - Add member
DELETE /api/v1/groups/{id}/members/{uid} - Remove member
GET    /api/v1/users/{id}/groups         - Get user's groups
```

#### 3. Expense Service
```
expense-service/
├── src/main/java/com/splitter/expense/
│   ├── controller/
│   │   └── ExpenseController.java
│   ├── service/
│   │   ├── ExpenseService.java
│   │   └── ExpenseSplitService.java
│   ├── repository/
│   │   ├── ExpenseRepository.java
│   │   └── ExpenseShareRepository.java
│   ├── model/
│   │   ├── Expense.java
│   │   ├── ExpenseShare.java
│   │   ├── SplitType.java           # EQUAL, PERCENTAGE, SHARES, EXACT
│   │   └── Category.java
│   └── event/
│       ├── ExpenseCreatedEvent.java
│       └── ExpenseUpdatedEvent.java
└── ...
```

**Responsibilities:**
- Expense CRUD operations
- Split calculations (equal, percentage, shares, exact amounts)
- Category management
- Receipt storage (S3/blob storage)

**API Endpoints:**
```
POST   /api/v1/expenses                  - Create expense
GET    /api/v1/expenses/{id}             - Get expense details
PUT    /api/v1/expenses/{id}             - Update expense
DELETE /api/v1/expenses/{id}             - Delete expense
GET    /api/v1/groups/{id}/expenses      - List group expenses
GET    /api/v1/users/{id}/expenses       - List user's expenses
POST   /api/v1/expenses/{id}/receipt     - Upload receipt
```

#### 4. Balance Service
```
balance-service/
├── src/main/java/com/splitter/balance/
│   ├── controller/
│   │   └── BalanceController.java
│   ├── service/
│   │   ├── BalanceCalculatorService.java
│   │   └── DebtSimplificationService.java
│   ├── repository/
│   │   └── BalanceRepository.java
│   ├── model/
│   │   ├── Balance.java
│   │   └── DebtRelation.java
│   └── algorithm/
│       └── DebtSimplifier.java      # Graph-based debt optimization
└── ...
```

**Responsibilities:**
- Real-time balance calculations
- Debt simplification algorithm
- Balance aggregation by group/user
- Balance history

**Core Algorithm - Debt Simplification:**
```java
/**
 * Simplifies debts using a graph-based approach.
 * Example: If A owes B $10, B owes C $10, simplify to A owes C $10
 */
public class DebtSimplifier {
    
    public Flux<SimplifiedDebt> simplifyDebts(Flux<Debt> debts) {
        return debts.collectList()
            .map(this::buildDebtGraph)
            .map(this::calculateNetBalances)
            .flatMapMany(this::generateOptimalTransfers);
    }
    
    // Uses min-cash-flow algorithm to minimize transactions
    private List<SimplifiedDebt> generateOptimalTransfers(Map<String, BigDecimal> netBalances) {
        // Implementation: Greedy approach matching max creditor with max debtor
    }
}
```

**API Endpoints:**
```
GET /api/v1/balances/user/{userId}           - User's overall balance
GET /api/v1/balances/group/{groupId}         - Group balances
GET /api/v1/balances/between/{user1}/{user2} - Balance between two users
GET /api/v1/balances/simplified/{groupId}    - Simplified debts for group
```

#### 5. Settlement Service
```
settlement-service/
├── src/main/java/com/splitter/settlement/
│   ├── controller/
│   │   └── SettlementController.java
│   ├── service/
│   │   └── SettlementService.java
│   ├── repository/
│   │   └── SettlementRepository.java
│   ├── model/
│   │   ├── Settlement.java
│   │   └── PaymentMethod.java
│   └── event/
│       └── SettlementRecordedEvent.java
└── ...
```

**Responsibilities:**
- Record payments between users
- Settlement history
- Integration with payment providers (future)

**API Endpoints:**
```
POST /api/v1/settlements                 - Record settlement
GET  /api/v1/settlements/{id}            - Get settlement details
GET  /api/v1/users/{id}/settlements      - User's settlement history
POST /api/v1/settlements/settle-up       - Settle all debts with user
```

#### 6. Notification Service
```
notification-service/
├── src/main/java/com/splitter/notification/
│   ├── listener/
│   │   └── EventListener.java
│   ├── service/
│   │   ├── NotificationService.java
│   │   ├── EmailService.java
│   │   └── PushNotificationService.java
│   └── template/
│       └── NotificationTemplates.java
└── ...
```

**Responsibilities:**
- Listen to domain events
- Send push notifications
- Send email notifications
- In-app notification management

---

## 📊 Database Schema

### User Service Database (PostgreSQL)
```sql
-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    display_name VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(500),
    phone VARCHAR(20),
    default_currency CHAR(3) DEFAULT 'USD',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Friendships table
CREATE TABLE friendships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    friend_id UUID REFERENCES users(id),
    status VARCHAR(20) DEFAULT 'pending', -- pending, accepted, blocked
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, friend_id)
);

CREATE INDEX idx_friendships_user ON friendships(user_id);
CREATE INDEX idx_friendships_friend ON friendships(friend_id);
```

### Group Service Database
```sql
-- Groups table
CREATE TABLE groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    group_type VARCHAR(20) DEFAULT 'other', -- trip, household, couple, other
    cover_image_url VARCHAR(500),
    simplify_debts BOOLEAN DEFAULT true,
    default_currency CHAR(3) DEFAULT 'USD',
    created_by UUID NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Group members table
CREATE TABLE group_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID REFERENCES groups(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    role VARCHAR(20) DEFAULT 'member', -- admin, member
    joined_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(group_id, user_id)
);

CREATE INDEX idx_group_members_group ON group_members(group_id);
CREATE INDEX idx_group_members_user ON group_members(user_id);
```

### Expense Service Database
```sql
-- Expenses table
CREATE TABLE expenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID,
    description VARCHAR(255) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    currency CHAR(3) DEFAULT 'USD',
    category VARCHAR(50),
    paid_by UUID NOT NULL,
    split_type VARCHAR(20) DEFAULT 'EQUAL', -- EQUAL, PERCENTAGE, SHARES, EXACT
    expense_date DATE NOT NULL,
    receipt_url VARCHAR(500),
    notes TEXT,
    is_recurring BOOLEAN DEFAULT false,
    recurring_interval VARCHAR(20), -- daily, weekly, monthly, yearly
    created_by UUID NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Expense shares table (who owes what)
CREATE TABLE expense_shares (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    expense_id UUID REFERENCES expenses(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    share_amount DECIMAL(15, 2) NOT NULL,
    share_percentage DECIMAL(5, 2),
    share_units INTEGER,
    is_settled BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_expenses_group ON expenses(group_id);
CREATE INDEX idx_expenses_paid_by ON expenses(paid_by);
CREATE INDEX idx_expense_shares_expense ON expense_shares(expense_id);
CREATE INDEX idx_expense_shares_user ON expense_shares(user_id);

-- Categories table
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL,
    icon VARCHAR(50),
    color VARCHAR(7),
    parent_id UUID REFERENCES categories(id)
);

-- Default categories
INSERT INTO categories (name, icon) VALUES
('General', 'receipt'),
('Food & Drink', 'restaurant'),
('Transportation', 'car'),
('Entertainment', 'movie'),
('Utilities', 'lightbulb'),
('Rent', 'home'),
('Groceries', 'shopping-cart'),
('Travel', 'plane'),
('Shopping', 'bag'),
('Healthcare', 'medical');
```

### Balance Service Database (Materialized View / Cache)
```sql
-- Balances table (denormalized for fast reads)
CREATE TABLE balances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    from_user_id UUID NOT NULL,
    to_user_id UUID NOT NULL,
    group_id UUID,
    amount DECIMAL(15, 2) NOT NULL, -- positive = from owes to
    currency CHAR(3) DEFAULT 'USD',
    last_calculated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(from_user_id, to_user_id, group_id)
);

CREATE INDEX idx_balances_from ON balances(from_user_id);
CREATE INDEX idx_balances_to ON balances(to_user_id);
CREATE INDEX idx_balances_group ON balances(group_id);
```

### Settlement Service Database
```sql
-- Settlements table
CREATE TABLE settlements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    from_user_id UUID NOT NULL,
    to_user_id UUID NOT NULL,
    group_id UUID,
    amount DECIMAL(15, 2) NOT NULL,
    currency CHAR(3) DEFAULT 'USD',
    payment_method VARCHAR(50), -- cash, bank_transfer, paypal, venmo
    notes TEXT,
    settled_at TIMESTAMP DEFAULT NOW(),
    created_by UUID NOT NULL
);

CREATE INDEX idx_settlements_from ON settlements(from_user_id);
CREATE INDEX idx_settlements_to ON settlements(to_user_id);
```

---

## 📨 Event-Driven Architecture

Following the [Event-Driven Architecture Guidelines](.github/event-driven-architecture-guidelines.md):

### Domain Events

```java
// Event structure following guidelines
@Value
@Builder
public class ExpenseCreatedEvent {
    String eventId;           // UUID
    String eventType;         // "expense.created.v1"
    Instant eventTime;
    String source;            // "expense-service"
    String subject;           // expense ID
    String dataVersion;       // "1.0"
    ExpenseData data;
    EventMetadata metadata;
    
    @Value
    @Builder
    public static class ExpenseData {
        String expenseId;
        String groupId;
        String paidByUserId;
        BigDecimal amount;
        String currency;
        List<ShareData> shares;
    }
    
    @Value
    @Builder
    public static class EventMetadata {
        String correlationId;
        String causationId;
        String userId;
    }
}
```

### Kafka Topics

| Topic Name | Producer | Consumers | Purpose |
|------------|----------|-----------|---------|
| `user.events` | User Service | Notification, Analytics | User lifecycle events |
| `group.events` | Group Service | Notification, Analytics | Group changes |
| `expense.events` | Expense Service | Balance, Notification, Analytics | Expense CRUD |
| `settlement.events` | Settlement Service | Balance, Notification | Payments recorded |
| `balance.events` | Balance Service | Notification | Balance updates |

### Event Flow Example

```
┌──────────────┐    expense.created.v1     ┌─────────────────┐
│   Expense    │ ──────────────────────────▶│  Balance        │
│   Service    │                            │  Service        │
└──────────────┘                            └────────┬────────┘
                                                     │
                                                     │ balance.updated.v1
                                                     ▼
┌──────────────────────────────────────────────────────────────────────┐
│                           Kafka                                       │
└──────────────────────────────────────────────────────────────────────┘
       │                          │                          │
       ▼                          ▼                          ▼
┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│ Notification │         │  Analytics   │         │    Sync      │
│   Service    │         │   Service    │         │   Service    │
└──────────────┘         └──────────────┘         └──────────────┘
```

### Outbox Pattern Implementation

```java
// Outbox table for reliable event publishing
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    private UUID id;
    private String aggregateType;
    private UUID aggregateId;
    private String eventType;
    
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private String payload;
    
    private Instant createdAt;
    private Instant processedAt;
}

// Transaction includes both business operation and event
@Transactional
public Mono<Expense> createExpense(CreateExpenseRequest request) {
    return expenseRepository.save(expense)
        .flatMap(saved -> outboxRepository.save(createOutboxEvent(saved))
            .thenReturn(saved));
}
```

---

## 🚀 Performance Guidelines

Following the [Performance Guidelines](.github/performance-guidelines.md):

### Caching Strategy

```java
// Redis caching for frequently accessed data
@Service
public class BalanceCacheService {
    
    private final ReactiveRedisTemplate<String, BalanceDto> redisTemplate;
    
    public Mono<BalanceDto> getUserBalance(String userId) {
        String cacheKey = "balance:user:" + userId;
        
        return redisTemplate.opsForValue().get(cacheKey)
            .switchIfEmpty(
                balanceService.calculateUserBalance(userId)
                    .flatMap(balance -> 
                        redisTemplate.opsForValue()
                            .set(cacheKey, balance, Duration.ofMinutes(5))
                            .thenReturn(balance)
                    )
            );
    }
    
    // Invalidate cache on expense/settlement events
    @KafkaListener(topics = "expense.events")
    public Mono<Void> onExpenseEvent(ExpenseEvent event) {
        return invalidateBalanceCaches(event.getAffectedUserIds());
    }
}
```

### Database Optimization

```java
// ✅ Good: Custom DTO projection for list views
@Query("""
    SELECT new com.splitter.expense.dto.ExpenseListDto(
        e.id, e.description, e.amount, e.currency, 
        e.category, e.paidBy, e.expenseDate
    ) FROM Expense e 
    WHERE e.groupId = :groupId 
    ORDER BY e.expenseDate DESC
    """)
Flux<ExpenseListDto> findExpenseListByGroupId(@Param("groupId") String groupId);

// ✅ Good: Batch operations
public Mono<Void> updateBalances(List<BalanceUpdate> updates) {
    return Flux.fromIterable(updates)
        .buffer(100) // Process in batches of 100
        .flatMap(batch -> balanceRepository.batchUpdate(batch))
        .then();
}
```

### Async Processing

```java
// Fire-and-forget for non-critical operations
public Mono<Expense> createExpense(CreateExpenseRequest request) {
    return expenseRepository.save(expense)
        .doOnSuccess(saved -> {
            // Async: Don't wait for these
            publishExpenseEvent(saved).subscribe();
            updateSearchIndex(saved).subscribe();
        });
}
```

---

## 🔐 Security Architecture

### Authentication Flow

```
┌─────────┐     ┌──────────┐     ┌─────────────┐     ┌─────────────┐
│  Client │────▶│ API GW   │────▶│   Keycloak  │────▶│  User DB    │
└─────────┘     └──────────┘     └─────────────┘     └─────────────┘
     │                                   │
     │◀──────── JWT Token ◀──────────────┘
     │
     │          ┌──────────┐     ┌─────────────┐
     └─────────▶│ API GW   │────▶│  Services   │
                │ (Verify) │     │             │
                └──────────┘     └─────────────┘
```

### JWT Token Structure

```json
{
  "sub": "user-uuid",
  "email": "user@example.com",
  "name": "John Doe",
  "roles": ["user"],
  "groups": ["group-uuid-1", "group-uuid-2"],
  "exp": 1735689600,
  "iat": 1735603200
}
```

### Authorization Rules

```java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf().disable()
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/health").permitAll()
                .pathMatchers("/api/v1/auth/**").permitAll()
                .pathMatchers("/api/v1/**").authenticated()
                .anyExchange().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt())
            .build();
    }
}

// Method-level security
@PreAuthorize("@groupAuthService.isMember(#groupId, authentication.principal)")
public Mono<List<ExpenseDto>> getGroupExpenses(String groupId) {
    // Only group members can access
}
```

---

## 📱 API Design

### REST API Conventions

Following the [Coding Guidelines](.github/coding-guidelines.md):

```yaml
# OpenAPI Specification Example
openapi: 3.0.3
info:
  title: Splitter API
  version: 1.0.0
  
paths:
  /api/v1/expenses:
    post:
      summary: Create a new expense
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateExpenseRequest'
      responses:
        '201':
          description: Expense created
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ExpenseDto'
        '400':
          description: Validation error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

components:
  schemas:
    CreateExpenseRequest:
      type: object
      required:
        - description
        - amount
        - paidBy
        - participants
      properties:
        description:
          type: string
          maxLength: 255
        amount:
          type: number
          format: decimal
          minimum: 0.01
        currency:
          type: string
          default: USD
        groupId:
          type: string
          format: uuid
        paidBy:
          type: string
          format: uuid
        splitType:
          type: string
          enum: [EQUAL, PERCENTAGE, SHARES, EXACT]
          default: EQUAL
        participants:
          type: array
          items:
            $ref: '#/components/schemas/ParticipantShare'
        category:
          type: string
        expenseDate:
          type: string
          format: date
        notes:
          type: string
          
    ErrorResponse:
      type: object
      properties:
        timestamp:
          type: string
          format: date-time
        status:
          type: integer
        error:
          type: string
        message:
          type: string
        path:
          type: string
```

### Error Handling

```java
@Value
@Builder
public class ErrorResponse {
    Instant timestamp;
    int status;
    String error;
    String message;
    String path;
    List<FieldError> fieldErrors;
}

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalErrorHandler implements ErrorWebExceptionHandler {
    
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        // Map exceptions to appropriate HTTP responses
    }
}
```

---

## 🧪 Testing Strategy

Following the test pyramid approach:

### Unit Tests
```java
@ExtendWith(MockitoExtension.class)
class DebtSimplifierTest {
    
    @InjectMocks
    private DebtSimplifier debtSimplifier;
    
    @Test
    void shouldSimplifyChainedDebts() {
        // Given: A owes B $10, B owes C $10
        List<Debt> debts = List.of(
            new Debt("A", "B", new BigDecimal("10")),
            new Debt("B", "C", new BigDecimal("10"))
        );
        
        // When
        List<SimplifiedDebt> result = debtSimplifier.simplify(debts);
        
        // Then: A owes C $10 (B is removed from chain)
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(new SimplifiedDebt("A", "C", new BigDecimal("10")));
    }
}
```

### Integration Tests
```java
@SpringBootTest
@AutoConfigureWebTestClient
class ExpenseControllerIntegrationTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @Test
    void shouldCreateExpense() {
        CreateExpenseRequest request = CreateExpenseRequest.builder()
            .description("Dinner")
            .amount(new BigDecimal("50.00"))
            .paidBy("user-1")
            .participants(List.of("user-1", "user-2"))
            .build();
            
        webTestClient.post()
            .uri("/api/v1/expenses")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(ExpenseDto.class)
            .value(expense -> {
                assertThat(expense.getDescription()).isEqualTo("Dinner");
                assertThat(expense.getAmount()).isEqualByComparingTo("50.00");
            });
    }
}
```

### Contract Tests
```java
@Pact(consumer = "balance-service", provider = "expense-service")
public RequestResponsePact expenseCreatedEventPact(PactDslWithProvider builder) {
    return builder
        .given("an expense exists")
        .uponReceiving("expense created event")
        .path("/api/v1/expenses/123")
        .method("GET")
        .willRespondWith()
        .status(200)
        .body(newJsonBody(body -> {
            body.stringValue("id", "123");
            body.decimalType("amount", 50.00);
        }).build())
        .toPact();
}
```

---

## 📁 Project Structure

```
splitter/
├── .github/
│   ├── workflows/
│   │   ├── ci.yml
│   │   └── deploy.yml
│   ├── coding-guidelines.md
│   ├── event-driven-architecture-guidelines.md
│   ├── microservice-guidelines.md
│   ├── performance-guidelines.md
│   └── webflux-guidelines.md
│
├── services/
│   ├── api-gateway/
│   ├── user-service/
│   ├── group-service/
│   ├── expense-service/
│   ├── balance-service/
│   ├── settlement-service/
│   └── notification-service/
│
├── shared/
│   ├── common-dto/              # Shared DTOs
│   ├── common-events/           # Event definitions
│   └── common-security/         # Security utilities
│
├── infrastructure/
│   ├── docker/
│   │   ├── docker-compose.yml
│   │   └── docker-compose.dev.yml
│   ├── kubernetes/
│   │   ├── base/
│   │   └── overlays/
│   └── terraform/               # Cloud infrastructure
│
├── frontend/
│   ├── web/                     # Next.js web app
│   ├── mobile/                  # React Native (alternative to native)
│   └── shared/                  # Shared components
│
├── docs/
│   ├── api/
│   ├── architecture/
│   └── runbooks/
│
└── scripts/
    ├── setup-dev.sh
    └── seed-data.sh
```

---

## 🚀 Getting Started

### Prerequisites
- Java 21+
- Docker & Docker Compose
- Node.js 18+ (for frontend)
- PostgreSQL 15+
- Apache Kafka
- Redis

### Local Development Setup

```bash
# 1. Clone the repository
git clone https://github.com/your-org/splitter.git
cd splitter

# 2. Start infrastructure services
docker-compose -f infrastructure/docker/docker-compose.dev.yml up -d

# 3. Start each microservice (in separate terminals)
cd services/user-service && ./mvnw spring-boot:run
cd services/group-service && ./mvnw spring-boot:run
cd services/expense-service && ./mvnw spring-boot:run
cd services/balance-service && ./mvnw spring-boot:run
cd services/settlement-service && ./mvnw spring-boot:run
cd services/notification-service && ./mvnw spring-boot:run

# 4. Start the API Gateway
cd services/api-gateway && ./mvnw spring-boot:run

# 5. Start the frontend
cd frontend/web && npm install && npm run dev
```

### Docker Compose (Full Stack)

```yaml
# docker-compose.yml
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: splitter
      POSTGRES_USER: splitter
      POSTGRES_PASSWORD: splitter
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
      
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    ports:
      - "9092:9092"
    environment:
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      
  api-gateway:
    build: ./services/api-gateway
    ports:
      - "8080:8080"
    depends_on:
      - user-service
      - group-service
      - expense-service
      
volumes:
  postgres_data:
```

---

## 📈 Monitoring & Observability

### Metrics (Prometheus + Grafana)

```yaml
# Key metrics to monitor
- http_requests_total
- http_request_duration_seconds
- expense_created_total
- settlement_processed_total
- kafka_consumer_lag
- db_connection_pool_size
- cache_hit_ratio
```

### Distributed Tracing

```java
// Automatic tracing with Spring Cloud Sleuth / Micrometer
@RestController
public class ExpenseController {
    
    @NewSpan("createExpense")
    @PostMapping("/expenses")
    public Mono<ExpenseDto> createExpense(@RequestBody CreateExpenseRequest request) {
        return expenseService.createExpense(request);
    }
}
```

### Health Checks

```java
@Component
public class DatabaseHealthIndicator implements ReactiveHealthIndicator {
    
    @Override
    public Mono<Health> health() {
        return databaseClient.sql("SELECT 1")
            .fetch().one()
            .map(result -> Health.up().build())
            .onErrorResume(ex -> Mono.just(Health.down(ex).build()));
    }
}
```

---

## 📝 Development Checklist

### Phase 1: MVP (8-12 weeks)
- [ ] Project setup with Spring Boot 3.x + WebFlux
- [ ] User Service with authentication
- [ ] Group Service with membership
- [ ] Expense Service with split calculations
- [ ] Balance Service with debt calculation
- [ ] Settlement Service
- [ ] API Gateway setup
- [ ] Basic web frontend
- [ ] Docker compose for local development
- [ ] CI/CD pipeline

### Phase 2: Enhanced Features (6-8 weeks)
- [ ] Multi-currency support
- [ ] Recurring expenses
- [ ] Push notifications
- [ ] Receipt scanning (OCR)
- [ ] Charts and analytics
- [ ] Mobile apps (iOS/Android)

### Phase 3: Scale & Polish (4-6 weeks)
- [ ] Performance optimization
- [ ] Kubernetes deployment
- [ ] Advanced monitoring
- [ ] Load testing
- [ ] Security audit
- [ ] Documentation

---

## 📚 References

- [Splitwise API Documentation](https://dev.splitwise.com/)
- [Spring WebFlux Documentation](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [R2DBC Documentation](https://r2dbc.io/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Project Guidelines](.github/)

---

*This architecture document is a living document. Update it as the project evolves.*
