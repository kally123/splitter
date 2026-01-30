# Phase 1: Core Microservices Implementation

## Overview

Phase 1 implements the core business microservices for the Splitter application. This phase builds on the infrastructure established in Phase 0 and delivers the essential functionality for expense splitting.

## Services Implemented

### 1. Group Service (Port 8082)
**Purpose:** Group creation, membership management, and invitations

**Components:**
- **Models:**
  - `Group` - Group entity with types (HOME, TRIP, COUPLE, FRIENDS, FAMILY, WORK, OTHER)
  - `GroupMember` - Membership with roles (OWNER, ADMIN, MEMBER)
  - `GroupInvitation` - Invitation workflow with statuses (PENDING, ACCEPTED, DECLINED, EXPIRED, CANCELLED)

- **Repositories:**
  - `GroupRepository` - Group CRUD and search
  - `GroupMemberRepository` - Membership queries
  - `GroupInvitationRepository` - Invitation management

- **Services:**
  - `GroupService` - Full group lifecycle management
  - `InvitationService` - Invitation workflow with expiration

- **Controllers:**
  - `GroupController` - REST endpoints for group operations
  - `InvitationController` - REST endpoints for invitations

- **Database:** `splitter_groups`

---

### 2. Expense Service (Port 8083)
**Purpose:** Expense CRUD and split calculations

**Components:**
- **Models:**
  - `Expense` - Expense entity with categories and split types
  - `ExpenseShare` - Individual participant shares

- **Split Types:**
  - `EQUAL` - Split equally among participants
  - `EXACT` - Specify exact amounts
  - `PERCENTAGE` - Specify percentages
  - `SHARES` - Specify share units

- **Categories:** FOOD_AND_DRINK, GROCERIES, SHOPPING, ENTERTAINMENT, TRANSPORTATION, UTILITIES, RENT, HEALTHCARE, EDUCATION, TRAVEL, SPORTS, GIFTS, OTHER

- **Services:**
  - `ExpenseService` - Expense CRUD with event publishing
  - `SplitCalculator` - Split calculation algorithms

- **Database:** `splitter_expenses`

---

### 3. Balance Service (Port 8084)
**Purpose:** Balance tracking and debt simplification

**Components:**
- **Models:**
  - `Balance` - Net balance between two users
  - `BalanceTransaction` - Audit trail of balance changes

- **Services:**
  - `BalanceService` - Balance calculations and Kafka event consumers
  - `DebtSimplifier` - Graph-based debt minimization algorithm

- **Features:**
  - Automatic balance updates from expense/settlement events
  - Debt simplification to minimize transactions
  - Redis caching for performance

- **Database:** `splitter_balances`

---

### 4. Settlement Service (Port 8085)
**Purpose:** Payment recording and confirmation

**Components:**
- **Models:**
  - `Settlement` - Payment record with status workflow

- **Payment Methods:** CASH, BANK_TRANSFER, VENMO, PAYPAL, ZELLE, CREDIT_CARD, CHECK, OTHER

- **Statuses:** PENDING, CONFIRMED, REJECTED, CANCELLED

- **Workflow:**
  1. User A creates settlement (PENDING)
  2. User B confirms or rejects
  3. Confirmed settlements publish events to update balances

- **Database:** `splitter_settlements`

---

### 5. Notification Service (Port 8086)
**Purpose:** Multi-channel notifications

**Components:**
- **Models:**
  - `Notification` - Notification entity
  - `NotificationPreference` - User preferences

- **Channels:**
  - IN_APP - Real-time in-app notifications
  - EMAIL - Email notifications with templates
  - PUSH - Push notifications (ready for integration)

- **Notification Types:**
  - EXPENSE_ADDED, EXPENSE_UPDATED, EXPENSE_DELETED
  - SETTLEMENT_REQUESTED, SETTLEMENT_CONFIRMED, SETTLEMENT_REJECTED
  - GROUP_INVITATION, GROUP_MEMBER_JOINED, GROUP_MEMBER_LEFT
  - REMINDER, SYSTEM

- **Features:**
  - Kafka event consumers for automatic notifications
  - Email templates using Thymeleaf
  - User preference management
  - Scheduled cleanup of old notifications

- **Database:** `splitter_notifications`

---

## Event-Driven Architecture

### Kafka Topics
| Topic | Publishers | Consumers |
|-------|-----------|-----------|
| `expense-events` | Expense Service | Balance Service, Notification Service |
| `settlement-events` | Settlement Service | Balance Service, Notification Service |
| `group-events` | Group Service | Notification Service |
| `user-events` | User Service | All services (optional) |

### Event Flow Examples

**Expense Creation Flow:**
```
User creates expense → Expense Service
                         ↓
                    Publish ExpenseCreatedEvent
                         ↓
         ┌───────────────┴───────────────┐
         ↓                               ↓
  Balance Service                 Notification Service
  (Update balances)               (Send notifications)
```

**Settlement Confirmation Flow:**
```
User confirms settlement → Settlement Service
                              ↓
                      Publish SettlementCreatedEvent
                              ↓
              ┌───────────────┴───────────────┐
              ↓                               ↓
       Balance Service                 Notification Service
       (Reduce debts)                  (Notify both parties)
```

---

## Database Schema Summary

### splitter_groups
- `groups` - Group definitions
- `group_members` - Membership records
- `group_invitations` - Invitation tokens

### splitter_expenses
- `expenses` - Expense records
- `expense_shares` - Individual shares

### splitter_balances
- `balances` - Current balance state
- `balance_transactions` - Transaction history

### splitter_settlements
- `settlements` - Payment records

### splitter_notifications
- `notifications` - Notification records
- `notification_preferences` - User preferences

---

## API Endpoints Summary

### Group Service
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/groups` | Create group |
| GET | `/api/v1/groups/{id}` | Get group |
| PUT | `/api/v1/groups/{id}` | Update group |
| DELETE | `/api/v1/groups/{id}` | Delete group |
| POST | `/api/v1/groups/{id}/members` | Add member |
| DELETE | `/api/v1/groups/{id}/members/{userId}` | Remove member |
| POST | `/api/v1/invitations` | Create invitation |
| POST | `/api/v1/invitations/{token}/accept` | Accept invitation |

### Expense Service
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/expenses` | Create expense |
| GET | `/api/v1/expenses/{id}` | Get expense |
| GET | `/api/v1/expenses?groupId=` | Get group expenses |
| PUT | `/api/v1/expenses/{id}` | Update expense |
| DELETE | `/api/v1/expenses/{id}` | Delete expense |

### Balance Service
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/balances/group/{groupId}` | Get group balances |
| GET | `/api/v1/balances/group/{groupId}/summary` | Get simplified debts |
| GET | `/api/v1/balances/user` | Get user balances |

### Settlement Service
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/settlements` | Create settlement |
| GET | `/api/v1/settlements/{id}` | Get settlement |
| POST | `/api/v1/settlements/{id}/confirm` | Confirm settlement |
| POST | `/api/v1/settlements/{id}/reject` | Reject settlement |
| POST | `/api/v1/settlements/{id}/cancel` | Cancel settlement |

### Notification Service
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/notifications` | Get notifications |
| GET | `/api/v1/notifications/unread/count` | Get unread count |
| POST | `/api/v1/notifications/{id}/read` | Mark as read |
| GET | `/api/v1/notifications/preferences` | Get preferences |
| PUT | `/api/v1/notifications/preferences` | Update preferences |

---

## Running Phase 1 Services

### Prerequisites
1. Phase 0 infrastructure running (Docker Compose)
2. Databases initialized
3. Shared libraries built

### Build Commands
```bash
# Build all services
cd services/group-service && mvn clean package -DskipTests
cd ../expense-service && mvn clean package -DskipTests
cd ../balance-service && mvn clean package -DskipTests
cd ../settlement-service && mvn clean package -DskipTests
cd ../notification-service && mvn clean package -DskipTests
```

### Run Commands
```bash
# Run each service (in separate terminals)
java -jar services/group-service/target/*.jar
java -jar services/expense-service/target/*.jar
java -jar services/balance-service/target/*.jar
java -jar services/settlement-service/target/*.jar
java -jar services/notification-service/target/*.jar
```

---

## Testing

### Health Checks
```bash
curl http://localhost:8082/actuator/health  # Group Service
curl http://localhost:8083/actuator/health  # Expense Service
curl http://localhost:8084/actuator/health  # Balance Service
curl http://localhost:8085/actuator/health  # Settlement Service
curl http://localhost:8086/actuator/health  # Notification Service
```

### API Documentation
- Group Service: http://localhost:8082/swagger-ui.html
- Expense Service: http://localhost:8083/swagger-ui.html
- Balance Service: http://localhost:8084/swagger-ui.html
- Settlement Service: http://localhost:8085/swagger-ui.html
- Notification Service: http://localhost:8086/swagger-ui.html

---

## Next Steps (Phase 2)

1. **Frontend Development**
   - React/Next.js web application
   - Mobile-responsive design
   - Real-time updates with WebSockets

2. **Enhanced Features**
   - Receipt scanning (OCR)
   - Currency conversion
   - Recurring expenses
   - Activity feed

3. **Testing**
   - Unit tests for all services
   - Integration tests
   - End-to-end tests
