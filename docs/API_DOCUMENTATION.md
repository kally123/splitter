# API Documentation

This document provides an overview of all API endpoints in the Splitter application.

## Base URLs

| Environment | Base URL |
|-------------|----------|
| Development | http://localhost:8080 |
| Staging | https://api-staging.splitter.app |
| Production | https://api.splitter.app |

## Authentication

All protected endpoints require a JWT access token in the Authorization header:

```
Authorization: Bearer <access_token>
```

---

## Authentication Service

### Register New User

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe",
  "defaultCurrency": "USD"
}
```

**Response:** `201 Created`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "a1b2c3d4-e5f6-7890-...",
  "expiresIn": 900,
  "tokenType": "Bearer",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "displayName": "John Doe"
}
```

### Login

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Response:** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "a1b2c3d4-e5f6-7890-...",
  "expiresIn": 900,
  "tokenType": "Bearer",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "displayName": "John Doe"
}
```

### Refresh Token

```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "a1b2c3d4-e5f6-7890-..."
}
```

### Logout

```http
POST /api/v1/auth/logout
Content-Type: application/json

{
  "refreshToken": "a1b2c3d4-e5f6-7890-..."
}
```

**Response:** `204 No Content`

### Logout All Devices

```http
POST /api/v1/auth/logout-all
Authorization: Bearer <access_token>
```

**Response:** `204 No Content`

---

## User Service

### Get Current User

```http
GET /api/v1/users/me
Authorization: Bearer <access_token>
```

**Response:** `200 OK`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "displayName": "John Doe",
  "phoneNumber": "+1234567890",
  "avatarUrl": "https://example.com/avatar.jpg",
  "defaultCurrency": "USD",
  "locale": "en-US",
  "timezone": "America/New_York",
  "emailVerified": true,
  "createdAt": "2024-01-15T10:30:00Z"
}
```

### Update Current User

```http
PUT /api/v1/users/me
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Smith",
  "displayName": "John S.",
  "defaultCurrency": "EUR"
}
```

### Get User by ID

```http
GET /api/v1/users/{userId}
Authorization: Bearer <access_token>
```

### Get Multiple Users

```http
GET /api/v1/users/batch?ids=id1,id2,id3
Authorization: Bearer <access_token>
```

### Search Users

```http
GET /api/v1/users/search?q=john&limit=10
Authorization: Bearer <access_token>
```

---

## Group Service

### Create Group

```http
POST /api/v1/groups
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "name": "Apartment Expenses",
  "description": "Shared expenses for our apartment",
  "type": "HOME",
  "defaultCurrency": "USD",
  "memberEmails": ["roommate1@example.com", "roommate2@example.com"]
}
```

**Response:** `201 Created`
```json
{
  "id": "group-uuid",
  "name": "Apartment Expenses",
  "description": "Shared expenses for our apartment",
  "type": "HOME",
  "defaultCurrency": "USD",
  "coverImageUrl": null,
  "createdBy": "user-uuid",
  "createdAt": "2024-01-15T10:30:00Z",
  "memberCount": 3
}
```

### Get User's Groups

```http
GET /api/v1/groups
Authorization: Bearer <access_token>
```

### Get Group Details

```http
GET /api/v1/groups/{groupId}
Authorization: Bearer <access_token>
```

### Add Member to Group

```http
POST /api/v1/groups/{groupId}/members
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "email": "newmember@example.com"
}
```

### Remove Member from Group

```http
DELETE /api/v1/groups/{groupId}/members/{userId}
Authorization: Bearer <access_token>
```

---

## Expense Service

### Create Expense

```http
POST /api/v1/expenses
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "description": "Dinner at Italian Restaurant",
  "amount": 120.50,
  "currency": "USD",
  "groupId": "group-uuid",
  "paidBy": "user-uuid",
  "splitType": "EQUAL",
  "date": "2024-01-15",
  "category": "FOOD_AND_DRINK",
  "participants": ["user1-uuid", "user2-uuid", "user3-uuid"],
  "notes": "Birthday dinner"
}
```

**Split Types:**
- `EQUAL` - Split equally among participants
- `EXACT` - Specify exact amounts for each participant
- `PERCENTAGE` - Specify percentage for each participant
- `SHARES` - Specify shares for each participant

### Create Expense with Exact Split

```http
POST /api/v1/expenses
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "description": "Groceries",
  "amount": 85.00,
  "currency": "USD",
  "groupId": "group-uuid",
  "paidBy": "user-uuid",
  "splitType": "EXACT",
  "date": "2024-01-15",
  "shares": [
    {"userId": "user1-uuid", "amount": 40.00},
    {"userId": "user2-uuid", "amount": 25.00},
    {"userId": "user3-uuid", "amount": 20.00}
  ]
}
```

### Get Group Expenses

```http
GET /api/v1/expenses?groupId={groupId}&page=0&size=20
Authorization: Bearer <access_token>
```

### Get Expense Details

```http
GET /api/v1/expenses/{expenseId}
Authorization: Bearer <access_token>
```

### Delete Expense

```http
DELETE /api/v1/expenses/{expenseId}
Authorization: Bearer <access_token>
```

---

## Balance Service

### Get Group Balances

```http
GET /api/v1/balances/groups/{groupId}
Authorization: Bearer <access_token>
```

**Response:** `200 OK`
```json
{
  "groupId": "group-uuid",
  "balances": [
    {
      "userId": "user1-uuid",
      "displayName": "John",
      "netBalance": 45.50,
      "currency": "USD"
    },
    {
      "userId": "user2-uuid",
      "displayName": "Jane",
      "netBalance": -25.00,
      "currency": "USD"
    }
  ],
  "simplifiedDebts": [
    {
      "fromUserId": "user2-uuid",
      "fromDisplayName": "Jane",
      "toUserId": "user1-uuid",
      "toDisplayName": "John",
      "amount": 25.00,
      "currency": "USD"
    }
  ]
}
```

### Get User's Overall Balance

```http
GET /api/v1/balances/me
Authorization: Bearer <access_token>
```

---

## Settlement Service

### Record Settlement

```http
POST /api/v1/settlements
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "groupId": "group-uuid",
  "fromUserId": "payer-uuid",
  "toUserId": "receiver-uuid",
  "amount": 50.00,
  "currency": "USD",
  "paymentMethod": "CASH",
  "notes": "Paid at lunch"
}
```

**Payment Methods:**
- `CASH`
- `BANK_TRANSFER`
- `PAYPAL`
- `VENMO`
- `OTHER`

### Get Group Settlements

```http
GET /api/v1/settlements?groupId={groupId}
Authorization: Bearer <access_token>
```

---

## Error Responses

All errors follow a consistent format:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/users",
  "timestamp": "2024-01-15T10:30:00Z",
  "fieldErrors": [
    {
      "field": "email",
      "message": "must be a valid email address",
      "rejectedValue": "invalid-email"
    }
  ]
}
```

### Common Error Codes

| Status | Description |
|--------|-------------|
| 400 | Bad Request - Invalid input |
| 401 | Unauthorized - Missing or invalid token |
| 403 | Forbidden - Insufficient permissions |
| 404 | Not Found - Resource doesn't exist |
| 409 | Conflict - Duplicate resource |
| 500 | Internal Server Error |

---

## Rate Limiting

API requests are rate-limited per user:

| Tier | Requests/Hour | Requests/Minute |
|------|---------------|-----------------|
| Anonymous | 100 | 10 |
| Authenticated | 1000 | 100 |

Rate limit headers are included in responses:
```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 995
X-RateLimit-Reset: 1705323600
```

---

## OpenAPI/Swagger

Interactive API documentation is available at:

- **Development:** http://localhost:8080/swagger-ui.html
- **Per-Service:** http://localhost:{port}/swagger-ui.html

OpenAPI JSON specs are available at:
- http://localhost:{port}/v3/api-docs
