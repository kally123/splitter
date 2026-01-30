# Splitter - Project Implementation Plan

## 📊 Team Structure & Roles

| Role | Count | Responsibilities |
|------|-------|------------------|
| **Tech Lead** | 1 | Architecture decisions, code reviews, technical guidance |
| **Backend Developer** | 3-4 | Microservices development, API design, database |
| **Frontend Developer** | 2 | Web app, mobile responsiveness |
| **DevOps Engineer** | 1 | CI/CD, infrastructure, monitoring |
| **QA Engineer** | 1 | Testing strategy, automation, quality assurance |

---

## 🗓️ Project Timeline Overview

| Phase | Duration | Focus |
|-------|----------|-------|
| **Phase 0: Setup** | Week 1-2 | Infrastructure, project scaffolding |
| **Phase 1: Core Services** | Week 3-8 | User, Group, Expense, Balance services |
| **Phase 2: Integration** | Week 9-10 | API Gateway, Event integration, Frontend |
| **Phase 3: MVP Complete** | Week 11-12 | Testing, bug fixes, deployment |
| **Phase 4: Enhanced Features** | Week 13-18 | Advanced features, mobile |
| **Phase 5: Production** | Week 19-20 | Performance, security, go-live |

---

## 📋 PHASE 0: Project Setup (Week 1-2)

### Sprint 0.1: Infrastructure Setup

#### Task 0.1.1: Development Environment Setup
| Field | Details |
|-------|---------|
| **Assignee** | DevOps Engineer |
| **Duration** | 2 days |
| **Priority** | P0 - Critical |
| **Dependencies** | None |

**Deliverables:**
- [ ] Create Git repository with branch protection rules
- [ ] Set up monorepo structure as per ARCHITECTURE.md
- [ ] Configure `.gitignore`, `.editorconfig`
- [ ] Create `README.md` with setup instructions
- [ ] Set up PR templates and issue templates

**Acceptance Criteria:**
- All team members can clone and access repository
- Branch protection requires PR reviews
- Consistent code formatting across team

---

#### Task 0.1.2: Docker Development Environment
| Field | Details |
|-------|---------|
| **Assignee** | DevOps Engineer |
| **Duration** | 2 days |
| **Priority** | P0 - Critical |
| **Dependencies** | Task 0.1.1 |

**Deliverables:**
- [ ] Create `docker-compose.dev.yml` with:
  - PostgreSQL 15
  - Redis 7
  - Apache Kafka + Zookeeper
  - Kafka UI (for debugging)
- [ ] Create initialization scripts for databases
- [ ] Document local setup process

**Files to Create:**
```
infrastructure/
├── docker/
│   ├── docker-compose.dev.yml
│   ├── init-scripts/
│   │   ├── init-databases.sql
│   │   └── init-kafka-topics.sh
│   └── .env.example
```

**Acceptance Criteria:**
- `docker-compose up` starts all infrastructure services
- Each microservice has its own database schema
- Kafka topics are pre-created

---

#### Task 0.1.3: CI/CD Pipeline Setup
| Field | Details |
|-------|---------|
| **Assignee** | DevOps Engineer |
| **Duration** | 3 days |
| **Priority** | P0 - Critical |
| **Dependencies** | Task 0.1.1 |

**Deliverables:**
- [ ] GitHub Actions workflow for:
  - Build and test on PR
  - Code quality checks (SonarQube/CodeClimate)
  - Docker image build
  - Deploy to staging (later phase)
- [ ] Configure branch-specific workflows

**Files to Create:**
```
.github/
├── workflows/
│   ├── ci.yml
│   ├── build-images.yml
│   └── deploy-staging.yml
```

**Acceptance Criteria:**
- PRs automatically trigger build and tests
- Failed tests block PR merge
- Build status visible in GitHub

---

### Sprint 0.2: Project Scaffolding

#### Task 0.2.1: Shared Libraries Setup
| Field | Details |
|-------|---------|
| **Assignee** | Tech Lead |
| **Duration** | 2 days |
| **Priority** | P0 - Critical |
| **Dependencies** | Task 0.1.1 |

**Deliverables:**
- [ ] Create `shared/common-dto` module with base DTOs
- [ ] Create `shared/common-events` module with event base classes
- [ ] Create `shared/common-security` module with JWT utilities
- [ ] Set up multi-module Maven/Gradle build

**Files to Create:**
```
shared/
├── common-dto/
│   ├── pom.xml
│   └── src/main/java/com/splitter/common/dto/
│       ├── BaseDto.java
│       ├── PagedResponse.java
│       └── ErrorResponse.java
├── common-events/
│   ├── pom.xml
│   └── src/main/java/com/splitter/common/events/
│       ├── BaseEvent.java
│       ├── EventMetadata.java
│       └── EventPublisher.java
└── common-security/
    ├── pom.xml
    └── src/main/java/com/splitter/common/security/
        ├── JwtUtils.java
        └── SecurityConfig.java
```

**Acceptance Criteria:**
- Shared modules can be imported by services
- Consistent event structure across services
- JWT validation works with test tokens

---

#### Task 0.2.2: Service Template Creation
| Field | Details |
|-------|---------|
| **Assignee** | Tech Lead |
| **Duration** | 2 days |
| **Priority** | P0 - Critical |
| **Dependencies** | Task 0.2.1 |

**Deliverables:**
- [ ] Create service archetype/template with:
  - Spring Boot 3.x + WebFlux configuration
  - R2DBC database configuration
  - Kafka producer/consumer configuration
  - Health check endpoints
  - Structured logging setup
  - OpenAPI/Swagger configuration
- [ ] Document service creation process

**Template Structure:**
```
service-template/
├── pom.xml
├── src/main/java/com/splitter/{service}/
│   ├── Application.java
│   ├── config/
│   │   ├── DatabaseConfig.java
│   │   ├── KafkaConfig.java
│   │   ├── SecurityConfig.java
│   │   └── OpenApiConfig.java
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── model/
│   ├── dto/
│   └── event/
├── src/main/resources/
│   ├── application.yml
│   └── application-dev.yml
└── src/test/
```

**Acceptance Criteria:**
- New services can be created from template in < 5 minutes
- Template includes all required configurations
- Health endpoints return 200 OK

---

#### Task 0.2.3: API Gateway Scaffolding
| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer 1 |
| **Duration** | 2 days |
| **Priority** | P1 - High |
| **Dependencies** | Task 0.2.2 |

**Deliverables:**
- [ ] Set up Spring Cloud Gateway
- [ ] Configure route definitions (placeholder)
- [ ] Set up CORS configuration
- [ ] Configure rate limiting
- [ ] Set up request/response logging

**Files to Create:**
```
services/api-gateway/
├── pom.xml
├── src/main/java/com/splitter/gateway/
│   ├── GatewayApplication.java
│   ├── config/
│   │   ├── RouteConfig.java
│   │   ├── CorsConfig.java
│   │   └── RateLimitConfig.java
│   └── filter/
│       ├── AuthenticationFilter.java
│       └── LoggingFilter.java
└── src/main/resources/
    └── application.yml
```

**Acceptance Criteria:**
- Gateway starts and routes to placeholder services
- CORS allows frontend origins
- Rate limiting blocks excessive requests

---

## 📋 PHASE 1: Core Services (Week 3-8)

### Sprint 1.1: User Service (Week 3-4)

#### Task 1.1.1: User Service - Database Schema
| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer 1 |
| **Duration** | 1 day |
| **Priority** | P0 - Critical |
| **Dependencies** | Task 0.2.2 |

**Deliverables:**
- [ ] Create Flyway/Liquibase migrations for:
  - `users` table
  - `friendships` table
  - Required indexes
- [ ] Create R2DBC entity classes

**SQL Migrations:**
```sql
-- V1__create_users_table.sql
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

-- V2__create_friendships_table.sql
CREATE TABLE friendships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    friend_id UUID REFERENCES users(id),
    status VARCHAR(20) DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, friend_id)
);
```

**Acceptance Criteria:**
- Migrations run successfully on fresh database
- Indexes improve query performance
- Entity classes map correctly to tables

---

#### Task 1.1.2: User Service - Repository Layer
| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer 1 |
| **Duration** | 1 day |
| **Priority** | P0 - Critical |
| **Dependencies** | Task 1.1.1 |

**Deliverables:**
- [ ] Create `UserRepository` extending `ReactiveCrudRepository`
- [ ] Create `FriendshipRepository`
- [ ] Add custom query methods:
  - `findByEmail(String email)`
  - `findFriendsByUserId(UUID userId)`
  - `findPendingFriendRequests(UUID userId)`

**Files to Create:**
```java
// UserRepository.java
@Repository
public interface UserRepository extends ReactiveCrudRepository<User, UUID> {
    Mono<User> findByEmail(String email);
    Mono<Boolean> existsByEmail(String email);
}

// FriendshipRepository.java
@Repository
public interface FriendshipRepository extends ReactiveCrudRepository<Friendship, UUID> {
    Flux<Friendship> findByUserIdAndStatus(UUID userId, String status);
    Mono<Friendship> findByUserIdAndFriendId(UUID userId, UUID friendId);
}
```

**Acceptance Criteria:**
- All repository methods return reactive types (Mono/Flux)
- Custom queries execute correctly
- No blocking calls in repository layer

---

#### Task 1.1.3: User Service - Service Layer
| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer 1 |
| **Duration** | 2 days |
| **Priority** | P0 - Critical |
| **Dependencies** | Task 1.1.2 |

**Deliverables:**
- [ ] Create `UserService` with:
  - `createUser(CreateUserRequest)`
  - `getUserById(UUID)`
  - `updateUser(UUID, UpdateUserRequest)`
  - `deleteUser(UUID)`
  - `searchUsers(String query)`
- [ ] Create `FriendshipService` with:
  - `sendFriendRequest(UUID, UUID)`
  - `acceptFriendRequest(UUID)`
  - `rejectFriendRequest(UUID)`
  - `removeFriend(UUID, UUID)`
  - `getFriends(UUID)`

**Acceptance Criteria:**
- All methods are fully reactive (no `.block()`)
- Proper error handling with custom exceptions
- Business validation implemented

---

#### Task 1.1.4: User Service - Controller Layer
| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer 1 |
| **Duration** | 1 day |
| **Priority** | P0 - Critical |
| **Dependencies** | Task 1.1.3 |

**Deliverables:**
- [ ] Create `UserController` with REST endpoints:
  - `POST /api/v1/users` - Register
  - `GET /api/v1/users/{id}` - Get user
  - `PUT /api/v1/users/{id}` - Update user
  - `DELETE /api/v1/users/{id}` - Delete user
  - `GET /api/v1/users/search` - Search users
- [ ] Create `FriendController`:
  - `GET /api/v1/users/{id}/friends`
  - `POST /api/v1/users/{id}/friends`
  - `DELETE /api/v1/users/{id}/friends/{friendId}`
- [ ] Add request validation with `@Valid`
- [ ] Add OpenAPI annotations

**Acceptance Criteria:**
- All endpoints return proper HTTP status codes
- Validation errors return 400 with details
- OpenAPI docs generated correctly

---

#### Task 1.1.5: User Service - Event Publishing
| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer 1 |
| **Duration** | 1 day |
| **Priority** | P1 - High |
| **Dependencies** | Task 1.1.3 |

**Deliverables:**
- [ ] Create event classes:
  - `UserCreatedEvent`
  - `UserUpdatedEvent`
  - `FriendshipCreatedEvent`
- [ ] Implement Kafka producer for events
- [ ] Publish events from service layer

**Files to Create:**
```java
@Value
@Builder
public class UserCreatedEvent implements BaseEvent {
    String eventId = UUID.randomUUID().toString();
    String eventType = "user.created.v1";
    Instant eventTime = Instant.now();
    String source = "user-service";
    UserData data;
    EventMetadata metadata;
}
```

**Acceptance Criteria:**
- Events published to Kafka on user actions
- Events follow defined schema
- Events include correlation IDs

---

#### Task 1.1.6: User Service - Unit Tests
| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer 1 |
| **Duration** | 1 day |
| **Priority** | P1 - High |
| **Dependencies** | Task 1.1.4 |

**Deliverables:**
- [ ] Unit tests for `UserService` (80%+ coverage)
- [ ] Unit tests for `FriendshipService`
- [ ] Mock repository layer
- [ ] Test edge cases and error scenarios

**Acceptance Criteria:**
- All tests pass
- Code coverage > 80%
- Tests are deterministic and fast

---

#### Task 1.1.7: User Service - Integration Tests
| Field | Details |
|-------|---------|
| **Assignee** | QA Engineer |
| **Duration** | 1 day |
| **Priority** | P1 - High |
| **Dependencies** | Task 1.1.4 |

**Deliverables:**
- [ ] Integration tests with TestContainers (PostgreSQL)
- [ ] API endpoint tests with WebTestClient
- [ ] Test complete user lifecycle
- [ ] Test friend request flow

**Acceptance Criteria:**
- Tests run in CI pipeline
- Database state isolated between tests
- All API contracts verified

---

### Sprint 1.2: Group Service (Week 4-5)

#### Task 1.2.1: Group Service - Database Schema
| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer 2 |
| **Duration** | 1 day |
| **Priority** | P0 - Critical |
| **Dependencies** | Task 0.2.2 |

**Deliverables:**
- [ ] Create migrations for:
  - `groups` table
  - `group_members` table
  - Required indexes
- [ ] Create R2DBC entity classes

**Acceptance Criteria:**
- Migrations execute successfully
- Foreign key relationships correct
- Cascade deletes configured

---

#### Task 1.2.2: Group Service - Repository Layer
| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer 2 |
| **Duration** | 1 day |
| **Priority** | P0 - Critical |
| **Dependencies** | Task 1.2.1 |

**Deliverables:**
- [ ] `GroupRepository` with custom queries
- [ ] `GroupMemberRepository`
- [ ] Query methods:
  - `findGroupsByUserId(UUID userId)`
  - `findMembersByGroupId(UUID groupId)`
  - `isUserMemberOfGroup(UUID userId, UUID groupId)`

**Acceptance Criteria:**
- Reactive repositories only
- Efficient queries with proper joins
- Pagination support

---

#### Task 1.2.3: Group Service - Service Layer
| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer 2 |
| **Duration** | 2 days |
| **Priority** | P0 - Critical |
| **Dependencies** | Task 1.2.2 |

**Deliverables:**
- [ ] `GroupService`:
  - `createGroup(CreateGroupRequest)`
  - `getGroupById(UUID)`
  - `updateGroup(UUID, UpdateGroupRequest)`
  - `deleteGroup(UUID)`
  - `getGroupsByUser(UUID)`
- [ ] `GroupMembershipService`:
  - `addMember(UUID groupId, UUID userId)`
  - `removeMember(UUID groupId, UUID userId)`
  - `updateMemberRole(UUID groupId, UUID userId, Role)`
  - `getMembers(UUID groupId)`

**Acceptance Criteria:**
- Admin-only operations enforced
- Group creator becomes admin
- Member limits enforced (if any)

---

#### Task 1.2.4: Group Service - Controller Layer
| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer 2 |
| **Duration** | 1 day |
| **Priority** | P0 - Critical |
| **Dependencies** | Task 1.2.3 |

**Deliverables:**
- [ ] `GroupController` REST endpoints
- [ ] Request/Response DTOs
- [ ] Validation and error handling
- [ ] OpenAPI documentation

**Acceptance Criteria:**
- Authorization checks (only members access group)
- Proper HTTP status codes
- API documentation complete

---

#### Task 1.2.5: Group Service - Events & Tests
| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer 2 |
| **Duration** | 2 days |
| **Priority** | P1 - High |
| **Dependencies** | Task 1.2.4 |

**Deliverables:**
- [ ] Event classes: `GroupCreatedEvent`, `MemberAddedEvent`, `MemberRemovedEvent`
- [ ] Kafka producer integration
- [ ] Unit tests (80%+ coverage)
- [ ] Integration tests

**Acceptance Criteria:**
- Events published correctly
- All tests pass
- Coverage requirements met

---

### Sprint 1.3: Expense Service (Week 5-6)

#### Task 1.3.1: Expense Service - Database Schema
| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer 3 |
| **Duration** | 1 day |
| **Priority** | P0 - Critical |
| **Dependencies** | Task 0.2.2 |

**Deliverables:**
- [ ] Migrations for:
  - `expenses` table
  - `expense_shares` table
  - `categories` table (with seed data)
- [ ] Indexes for performance

**Acceptance Criteria:**
- Schema supports all split types
- Category hierarchy supported
- Soft delete for expenses (optional)

---

#### Task 1.3.2: Expense Service - Split Calculator
| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer 3 |
| **Duration** | 2 days |
| **Priority** | P0 - Critical |
| **Dependencies** | None |

**Deliverables:**
- [ ] `ExpenseSplitCalculator` service:
  - Equal split calculation
  - Percentage-based split
  - Shares-based split
  - Exact amount split
- [ ] Handle rounding (ensure total equals expense amount)
- [ ] Validation for each split type

**Example Logic:**
```java
public class ExpenseSplitCalculator {
    
    public Flux<ExpenseShare> calculateShares(
            BigDecimal totalAmount,
            SplitType splitType,
            List<Participant> participants) {
        
        return switch (splitType) {
            case EQUAL -> calculateEqualSplit(totalAmount, participants);
            case PERCENTAGE -> calculatePercentageSplit(totalAmount, participants);
            case SHARES -> calculateSharesSplit(totalAmount, participants);
            case EXACT -> validateExactSplit(totalAmount, participants);
        };
    }
    
    // Handle rounding: Last person gets remainder
    private Flux<ExpenseShare> calculateEqualSplit(...) {
        BigDecimal perPerson = totalAmount.divide(
            BigDecimal.valueOf(participants.size()), 
            2, RoundingMode.DOWN
        );
        BigDecimal remainder = totalAmount.subtract(
            perPerson.multiply(BigDecimal.valueOf(participants.size()))
        );
        // Assign remainder to last participant
    }
}
```

**Acceptance Criteria:**
- All split types calculate correctly
- Rounding doesn't lose/add money
- Edge cases handled (1 person, 0 amount, etc.)

---

#### Task 1.3.3: Expense Service - Repository Layer
| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer 3 |
| **Duration** | 1 day |
| **Priority** | P0 - Critical |
| **Dependencies** | Task 1.3.1 |

**Deliverables:**
- [ ] `ExpenseRepository`
- [ ] `ExpenseShareRepository`
- [ ] `CategoryRepository`
- [ ] Custom queries for filtering, pagination

**Acceptance Criteria:**
- Efficient queries for expense lists
- DTO projections for list views
- Proper eager/lazy loading

---

#### Task 1.3.4: Expense Service - Service Layer
| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer 3 |
| **Duration** | 2 days |
| **Priority** | P0 - Critical |
| **Dependencies** | Task 1.3.2, Task 1.3.3 |

**Deliverables:**
- [ ] `ExpenseService`:
  - `createExpense(CreateExpenseRequest)`
  - `getExpenseById(UUID)`
  - `updateExpense(UUID, UpdateExpenseRequest)`
  - `deleteExpense(UUID)`
  - `getExpensesByGroup(UUID, Pageable)`
  - `getExpensesByUser(UUID, Pageable)`
- [ ] Integrate split calculator
- [ ] Transaction management

**Acceptance Criteria:**
- Expense and shares saved atomically
- Authorization checks (payer or group member)
- Proper error handling

---

#### Task 1.3.5: Expense Service - Controller & Events
| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer 3 |
| **Duration** | 2 days |
| **Priority** | P0 - Critical |
| **Dependencies** | Task 1.3.4 |

**Deliverables:**
- [ ] `ExpenseController` REST endpoints
- [ ] Request validation
- [ ] Event publishing:
  - `ExpenseCreatedEvent`
  - `ExpenseUpdatedEvent`
  - `ExpenseDeletedEvent`
- [ ] OpenAPI documentation

**Acceptance Criteria:**
- All CRUD operations work
- Events trigger balance recalculation
- API contracts documented

---

#### Task 1.3.6: Expense Service - Tests
| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer 3 / QA |
| **Duration** | 2 days |
| **Priority** | P1 - High |
| **Dependencies** | Task 1.3.5 |

**Deliverables:**
- [ ] Unit tests for split calculator (critical!)
- [ ] Unit tests for service layer
- [ ] Integration tests for API
- [ ] Edge case testing

**Test Scenarios:**
```java
@Test
void equalSplit_shouldDivideEvenly() {
    // $100 between 3 people = $33.33, $33.33, $33.34
}

@Test
void equalSplit_singlePerson_shouldReturnFullAmount() {}

@Test
void percentageSplit_shouldValidateTotalIs100() {}

@Test
void exactSplit_shouldValidateTotalMatchesExpense() {}
```

**Acceptance Criteria:**
- Split calculator 100% test coverage
- All edge cases covered
- Tests run in < 30 seconds

---

### Sprint 1.4: Balance Service (Week 6-7)

#### Task 1.4.1: Balance Service - Core Algorithm
| Field | Details |
|-------|---------|
| **Assignee** | Tech Lead / Backend Developer 4 |
| **Duration** | 3 days |
| **Priority** | P0 - Critical |
| **Dependencies** | None |

**Deliverables:**
- [ ] `BalanceCalculator` service:
  - Calculate balances from expenses
  - Net balance between two users
  - Group balance summary
- [ ] `DebtSimplificationService`:
  - Min-cash-flow algorithm
  - Reduce number of transactions

**Algorithm Implementation:**
```java
public class DebtSimplifier {
    
    /**
     * Simplifies debts using greedy min-cash-flow algorithm.
     * 
     * 1. Calculate net balance for each person
     * 2. Match max creditor with max debtor
     * 3. Transfer min of their balances
     * 4. Repeat until all settled
     */
    public List<SimplifiedDebt> simplify(List<Debt> debts) {
        Map<String, BigDecimal> netBalances = calculateNetBalances(debts);
        List<SimplifiedDebt> simplified = new ArrayList<>();
        
        PriorityQueue<Balance> creditors = new PriorityQueue<>(
            Comparator.comparing(Balance::amount).reversed()
        );
        PriorityQueue<Balance> debtors = new PriorityQueue<>(
            Comparator.comparing(Balance::amount)
        );
        
        // Separate into creditors and debtors
        netBalances.forEach((userId, amount) -> {
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(new Balance(userId, amount));
            } else if (amount.compareTo(BigDecimal.ZERO) < 0) {
                debtors.add(new Balance(userId, amount.abs()));
            }
        });
        
        // Match and settle
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            Balance creditor = creditors.poll();
            Balance debtor = debtors.poll();
            
            BigDecimal settleAmount = creditor.amount().min(debtor.amount());
            simplified.add(new SimplifiedDebt(
                debtor.userId(), creditor.userId(), settleAmount
            ));
            
            // Handle remainder
            if (creditor.amount().compareTo(debtor.amount()) > 0) {
                creditors.add(new Balance(
                    creditor.userId(), 
                    creditor.amount().subtract(settleAmount)
                ));
            } else if (debtor.amount().compareTo(creditor.amount()) > 0) {
                debtors.add(new Balance(
                    debtor.userId(), 
                    debtor.amount().subtract(settleAmount)
                ));
            }
        }
        
        return simplified;
    }
}
```

**Acceptance Criteria:**
- Algorithm produces minimal transactions
- All balances settle to zero
- Performance: < 100ms for 100 users

---

#### Task 1.4.2: Balance Service - Event Consumer
| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer 4 |
| **Duration** | 2 days |
| **Priority** | P0 - Critical |
| **Dependencies** | Task 1.4.1 |

**Deliverables:**
- [ ] Kafka consumers for:
  - `expense.created.v1`
  - `expense.updated.v1`
  - `expense.deleted.v1`
  - `settlement.recorded.v1`
- [ ] Recalculate balances on events
- [ ] Idempotent processing

**Acceptance Criteria:**
- Balances update within 1 second of event
- Duplicate events don't corrupt data
- Failed events go to DLQ

---

#### Task 1.4.3: Balance Service - API & Storage
| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer 4 |
| **Duration** | 2 days |
| **Priority** | P0 - Critical |
| **Dependencies** | Task 1.4.1, Task 1.4.2 |

**Deliverables:**
- [ ] Balance cache/storage (Redis or PostgreSQL)
- [ ] `BalanceController`:
  - `GET /api/v1/balances/user/{userId}`
  - `GET /api/v1/balances/group/{groupId}`
  - `GET /api/v1/balances/between/{user1}/{user2}`
  - `GET /api/v1/balances/simplified/{groupId}`
- [ ] Response DTOs

**Acceptance Criteria:**
- Balance queries return < 100ms
- Simplified debts calculation correct
- Cache invalidation works

---

#### Task 1.4.4: Balance Service - Tests
| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer 4 / QA |
| **Duration** | 2 days |
| **Priority** | P0 - Critical |
| **Dependencies** | Task 1.4.3 |

**Deliverables:**
- [ ] Unit tests for `DebtSimplifier` (critical!)
- [ ] Unit tests for `BalanceCalculator`
- [ ] Integration tests for event processing
- [ ] Property-based testing for algorithm

**Critical Test Cases:**
```java
@Test
void simplify_chainedDebts_shouldOptimize() {
    // A owes B $10, B owes C $10 → A owes C $10
}

@Test
void simplify_circularDebts_shouldNetToZero() {
    // A owes B $10, B owes C $10, C owes A $10 → No debts
}

@Test
void simplify_complexScenario_shouldMinimizeTransactions() {
    // Multiple users with various debts
}

@Test
void simplify_shouldPreserveMonetaryInvariant() {
    // Total money in = Total money out
}
```

**Acceptance Criteria:**
- Algorithm correctness proven
- Edge cases covered
- Performance benchmarks pass

---

### Sprint 1.5: Settlement Service (Week 7-8)

#### Task 1.5.1: Settlement Service - Full Implementation
| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer 2 |
| **Duration** | 3 days |
| **Priority** | P1 - High |
| **Dependencies** | Task 1.4.3 |

**Deliverables:**
- [ ] Database schema and migrations
- [ ] `SettlementRepository`
- [ ] `SettlementService`:
  - `recordSettlement(RecordSettlementRequest)`
  - `getSettlementById(UUID)`
  - `getSettlementsByUser(UUID)`
  - `settleUpWithUser(UUID fromUser, UUID toUser)`
- [ ] `SettlementController`
- [ ] `SettlementRecordedEvent` publishing

**Acceptance Criteria:**
- Settlements trigger balance updates
- Settlement history queryable
- Payment method recorded

---

#### Task 1.5.2: Settlement Service - Tests
| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer 2 / QA |
| **Duration** | 1 day |
| **Priority** | P1 - High |
| **Dependencies** | Task 1.5.1 |

**Deliverables:**
- [ ] Unit tests
- [ ] Integration tests
- [ ] End-to-end settlement flow test

**Acceptance Criteria:**
- Settlement reduces balance correctly
- Over-settlement prevented
- Tests pass

---

### Sprint 1.6: Notification Service (Week 8)

#### Task 1.6.1: Notification Service - Event Listeners
| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer 1 |
| **Duration** | 2 days |
| **Priority** | P2 - Medium |
| **Dependencies** | Phase 1 services |

**Deliverables:**
- [ ] Kafka consumers for all domain events
- [ ] In-app notification storage
- [ ] `NotificationController`:
  - `GET /api/v1/notifications`
  - `PUT /api/v1/notifications/{id}/read`
  - `PUT /api/v1/notifications/read-all`
- [ ] Push notification integration (Firebase)

**Notification Types:**
- Expense added (you owe money)
- Expense added (someone owes you)
- Friend request received
- Added to group
- Settlement received
- Payment reminder (future)

**Acceptance Criteria:**
- Notifications delivered within 5 seconds
- Read/unread status works
- Push notifications reach devices

---

## 📋 PHASE 2: Integration (Week 9-10)

### Sprint 2.1: API Gateway Integration

#### Task 2.1.1: Route Configuration
| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer 1 |
| **Duration** | 1 day |
| **Priority** | P0 - Critical |
| **Dependencies** | All Phase 1 services |

**Deliverables:**
- [ ] Configure routes for all services
- [ ] JWT validation filter
- [ ] Request forwarding
- [ ] Load balancing (if multiple instances)

**Route Configuration:**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/v1/users/**
        - id: group-service
          uri: lb://group-service
          predicates:
            - Path=/api/v1/groups/**
        - id: expense-service
          uri: lb://expense-service
          predicates:
            - Path=/api/v1/expenses/**
        - id: balance-service
          uri: lb://balance-service
          predicates:
            - Path=/api/v1/balances/**
        - id: settlement-service
          uri: lb://settlement-service
          predicates:
            - Path=/api/v1/settlements/**
```

**Acceptance Criteria:**
- All services accessible through gateway
- Authentication enforced
- Proper error responses

---

#### Task 2.1.2: Authentication Integration
| Field | Details |
|-------|---------|
| **Assignee** | Tech Lead / Backend Developer |
| **Duration** | 2 days |
| **Priority** | P0 - Critical |
| **Dependencies** | Task 2.1.1 |

**Deliverables:**
- [ ] Keycloak/Auth0 setup
- [ ] User registration flow
- [ ] Login/logout endpoints
- [ ] Token refresh mechanism
- [ ] Password reset flow

**Acceptance Criteria:**
- Users can register and login
- JWT tokens issued correctly
- Token refresh works
- Password reset emails sent

---

### Sprint 2.2: Frontend Development (Week 9-10)

#### Task 2.2.1: Frontend Setup
| Field | Details |
|-------|---------|
| **Assignee** | Frontend Developer 1 |
| **Duration** | 2 days |
| **Priority** | P0 - Critical |
| **Dependencies** | None |

**Deliverables:**
- [ ] Next.js 14 project setup
- [ ] TypeScript configuration
- [ ] TailwindCSS setup
- [ ] Component library (shadcn/ui)
- [ ] API client setup (axios/fetch)
- [ ] Authentication context

**Project Structure:**
```
frontend/web/
├── src/
│   ├── app/                    # Next.js App Router
│   │   ├── (auth)/
│   │   │   ├── login/
│   │   │   └── register/
│   │   ├── (dashboard)/
│   │   │   ├── groups/
│   │   │   ├── expenses/
│   │   │   ├── friends/
│   │   │   └── activity/
│   │   └── layout.tsx
│   ├── components/
│   │   ├── ui/                 # shadcn components
│   │   ├── expense/
│   │   ├── group/
│   │   └── user/
│   ├── lib/
│   │   ├── api/
│   │   └── utils/
│   ├── hooks/
│   └── types/
├── public/
└── tailwind.config.js
```

**Acceptance Criteria:**
- Project builds successfully
- Hot reload works
- Responsive layout

---

#### Task 2.2.2: Authentication UI
| Field | Details |
|-------|---------|
| **Assignee** | Frontend Developer 1 |
| **Duration** | 2 days |
| **Priority** | P0 - Critical |
| **Dependencies** | Task 2.2.1, Task 2.1.2 |

**Deliverables:**
- [ ] Login page
- [ ] Registration page
- [ ] Forgot password page
- [ ] Protected route wrapper
- [ ] Auth state management

**Acceptance Criteria:**
- Forms validate input
- Error messages display
- Redirect after login
- Session persists on refresh

---

#### Task 2.2.3: Dashboard & Groups UI
| Field | Details |
|-------|---------|
| **Assignee** | Frontend Developer 1 |
| **Duration** | 3 days |
| **Priority** | P0 - Critical |
| **Dependencies** | Task 2.2.2 |

**Deliverables:**
- [ ] Dashboard with balance overview
- [ ] Groups list view
- [ ] Group detail view
- [ ] Create/edit group modals
- [ ] Member management UI

**Acceptance Criteria:**
- Dashboard shows overall balance
- Groups display with members
- Can create/edit groups
- Mobile responsive

---

#### Task 2.2.4: Expense UI
| Field | Details |
|-------|---------|
| **Assignee** | Frontend Developer 2 |
| **Duration** | 4 days |
| **Priority** | P0 - Critical |
| **Dependencies** | Task 2.2.1 |

**Deliverables:**
- [ ] Add expense form with:
  - Amount input
  - Description
  - Payer selection
  - Split type selector
  - Participant selection
  - Equal/unequal split UI
  - Category picker
  - Date picker
- [ ] Expense list view
- [ ] Expense detail view
- [ ] Edit/delete expense

**Acceptance Criteria:**
- All split types work
- Calculations show in real-time
- Form validation works
- Mobile-friendly

---

#### Task 2.2.5: Balance & Settlement UI
| Field | Details |
|-------|---------|
| **Assignee** | Frontend Developer 2 |
| **Duration** | 3 days |
| **Priority** | P0 - Critical |
| **Dependencies** | Task 2.2.4 |

**Deliverables:**
- [ ] Balance summary component
- [ ] "You owe" / "You are owed" lists
- [ ] Settle up flow
- [ ] Record payment modal
- [ ] Settlement history

**Acceptance Criteria:**
- Balances update after actions
- Settle up flow clear
- Payment recorded correctly

---

## 📋 PHASE 3: MVP Complete (Week 11-12)

### Sprint 3.1: End-to-End Testing

#### Task 3.1.1: E2E Test Suite
| Field | Details |
|-------|---------|
| **Assignee** | QA Engineer |
| **Duration** | 3 days |
| **Priority** | P0 - Critical |
| **Dependencies** | Phase 2 complete |

**Deliverables:**
- [ ] Playwright/Cypress setup
- [ ] Critical user journey tests:
  - User registration → Login
  - Create group → Add members
  - Add expense → View balance
  - Settle up flow
- [ ] API contract tests
- [ ] Performance tests (basic)

**Test Scenarios:**
```javascript
test('complete expense flow', async ({ page }) => {
  // 1. Login
  await page.goto('/login');
  await page.fill('[name=email]', 'user@test.com');
  await page.fill('[name=password]', 'password');
  await page.click('button[type=submit]');
  
  // 2. Create group
  await page.click('text=New Group');
  await page.fill('[name=name]', 'Trip to Paris');
  await page.click('text=Create');
  
  // 3. Add expense
  await page.click('text=Add Expense');
  await page.fill('[name=description]', 'Dinner');
  await page.fill('[name=amount]', '100');
  await page.click('text=Save');
  
  // 4. Verify balance
  await expect(page.locator('.balance')).toContainText('$50');
});
```

**Acceptance Criteria:**
- All critical flows pass
- Tests run in CI
- < 5 min total runtime

---

#### Task 3.1.2: Bug Fixes & Polish
| Field | Details |
|-------|---------|
| **Assignee** | All Developers |
| **Duration** | 3 days |
| **Priority** | P0 - Critical |
| **Dependencies** | Task 3.1.1 |

**Deliverables:**
- [ ] Fix all P0/P1 bugs found in testing
- [ ] UI polish and consistency
- [ ] Error message improvements
- [ ] Loading states
- [ ] Empty states

**Acceptance Criteria:**
- No P0/P1 bugs
- Consistent UI across app
- Good UX feedback

---

### Sprint 3.2: Deployment Preparation

#### Task 3.2.1: Production Infrastructure
| Field | Details |
|-------|---------|
| **Assignee** | DevOps Engineer |
| **Duration** | 3 days |
| **Priority** | P0 - Critical |
| **Dependencies** | None |

**Deliverables:**
- [ ] Kubernetes manifests (or ECS/Cloud Run)
- [ ] Production database setup
- [ ] Redis cluster
- [ ] Kafka cluster (managed)
- [ ] SSL certificates
- [ ] DNS configuration

**Acceptance Criteria:**
- Infrastructure provisioned
- SSL working
- Database backups configured

---

#### Task 3.2.2: Monitoring Setup
| Field | Details |
|-------|---------|
| **Assignee** | DevOps Engineer |
| **Duration** | 2 days |
| **Priority** | P1 - High |
| **Dependencies** | Task 3.2.1 |

**Deliverables:**
- [ ] Prometheus metrics collection
- [ ] Grafana dashboards:
  - Service health
  - Request latency
  - Error rates
  - Kafka lag
- [ ] Alerting rules
- [ ] Log aggregation (ELK/Loki)

**Acceptance Criteria:**
- All services have dashboards
- Alerts fire on issues
- Logs searchable

---

#### Task 3.2.3: Production Deployment
| Field | Details |
|-------|---------|
| **Assignee** | DevOps Engineer + Tech Lead |
| **Duration** | 2 days |
| **Priority** | P0 - Critical |
| **Dependencies** | Task 3.2.1, Task 3.2.2 |

**Deliverables:**
- [ ] Deploy all services to production
- [ ] Run smoke tests
- [ ] Monitor for issues
- [ ] Rollback plan ready

**Acceptance Criteria:**
- All services healthy
- Users can register and use app
- No critical errors

---

## 📋 PHASE 4: Enhanced Features (Week 13-18)

### Multi-Currency Support (Week 13-14)

#### Task 4.1: Currency Service
| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer |
| **Duration** | 5 days |
| **Priority** | P2 - Medium |

**Deliverables:**
- [ ] Currency exchange rate service
- [ ] Expense amount conversion
- [ ] Multi-currency balance calculations
- [ ] Currency preference in user settings

---

### Recurring Expenses (Week 14-15)

#### Task 4.2: Recurring Expense Feature
| Field | Details |
|-------|---------|
| **Assignee** | Backend Developer |
| **Duration** | 5 days |
| **Priority** | P2 - Medium |

**Deliverables:**
- [ ] Recurring expense scheduler
- [ ] Interval options (daily, weekly, monthly, yearly)
- [ ] Auto-creation of expenses
- [ ] UI for managing recurring expenses

---

### Charts & Analytics (Week 15-16)

#### Task 4.3: Analytics Dashboard
| Field | Details |
|-------|---------|
| **Assignee** | Frontend Developer |
| **Duration** | 5 days |
| **Priority** | P2 - Medium |

**Deliverables:**
- [ ] Spending by category chart
- [ ] Spending over time chart
- [ ] Group comparison
- [ ] Export functionality

---

### Mobile Apps (Week 16-18)

#### Task 4.4: React Native / Native Apps
| Field | Details |
|-------|---------|
| **Assignee** | Frontend Developer(s) |
| **Duration** | 10 days |
| **Priority** | P2 - Medium |

**Deliverables:**
- [ ] Mobile app setup
- [ ] Core screens (matching web)
- [ ] Push notification integration
- [ ] App store submission

---

## 📋 PHASE 5: Production Readiness (Week 19-20)

### Performance Optimization

#### Task 5.1: Performance Audit
| Field | Details |
|-------|---------|
| **Assignee** | Tech Lead |
| **Duration** | 3 days |
| **Priority** | P1 - High |

**Deliverables:**
- [ ] Load testing (k6/JMeter)
- [ ] Identify bottlenecks
- [ ] Query optimization
- [ ] Caching improvements
- [ ] Target: 500+ concurrent users

---

### Security Audit

#### Task 5.2: Security Review
| Field | Details |
|-------|---------|
| **Assignee** | Tech Lead / External |
| **Duration** | 3 days |
| **Priority** | P0 - Critical |

**Deliverables:**
- [ ] OWASP Top 10 review
- [ ] Penetration testing
- [ ] Dependency vulnerability scan
- [ ] Security headers check
- [ ] Data encryption audit

---

## 📊 Task Summary by Service

| Service | Total Tasks | Est. Days | Dependencies |
|---------|-------------|-----------|--------------|
| Infrastructure | 6 | 12 | None |
| Shared Libraries | 2 | 4 | Infrastructure |
| User Service | 7 | 10 | Shared Libraries |
| Group Service | 5 | 8 | Shared Libraries |
| Expense Service | 6 | 11 | Shared Libraries |
| Balance Service | 4 | 9 | Expense Service |
| Settlement Service | 2 | 4 | Balance Service |
| Notification Service | 1 | 2 | All Services |
| API Gateway | 2 | 3 | All Services |
| Frontend | 5 | 14 | API Gateway |
| Testing & QA | 3 | 8 | All |
| DevOps & Deploy | 3 | 7 | All |

---

## 🎯 Sprint Planning Template

### Sprint: [Sprint Name]
**Duration:** 2 weeks  
**Team Capacity:** [X] story points

#### Sprint Goals
1. 
2. 
3. 

#### Tasks

| ID | Task | Assignee | Points | Status |
|----|------|----------|--------|--------|
| | | | | |

#### Definition of Done
- [ ] Code reviewed and approved
- [ ] Unit tests passing (>80% coverage)
- [ ] Integration tests passing
- [ ] Documentation updated
- [ ] No P0/P1 bugs
- [ ] Deployed to staging

---

## 📅 Key Milestones

| Milestone | Target Date | Success Criteria |
|-----------|-------------|------------------|
| **M1: Dev Environment Ready** | Week 2 | All devs can run locally |
| **M2: Core Services Complete** | Week 8 | All APIs functional |
| **M3: Frontend MVP** | Week 10 | Basic UI working |
| **M4: MVP Launch** | Week 12 | Production deployment |
| **M5: Enhanced Features** | Week 18 | Mobile + advanced features |
| **M6: Production Ready** | Week 20 | Security + performance verified |

---

## 🚨 Risk Register

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Balance algorithm bugs | High | Medium | Extensive testing, property-based tests |
| Performance issues | High | Medium | Early load testing, caching |
| Team availability | Medium | Low | Cross-training, documentation |
| Scope creep | Medium | High | Strict MVP focus, backlog grooming |
| Third-party dependencies | Low | Medium | Abstract integrations, fallbacks |

---

*This plan should be reviewed and adjusted weekly based on actual progress.*
