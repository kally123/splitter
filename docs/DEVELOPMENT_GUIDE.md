# Development Guide

This guide provides detailed instructions for setting up and developing the Splitter application.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Environment Setup](#environment-setup)
3. [Running the Application](#running-the-application)
4. [Development Workflow](#development-workflow)
5. [Coding Standards](#coding-standards)
6. [Testing](#testing)
7. [Debugging](#debugging)
8. [Common Issues](#common-issues)

---

## Prerequisites

### Required Software

| Software | Version | Purpose |
|----------|---------|---------|
| Java JDK | 21+ | Runtime & compilation |
| Maven | 3.9+ | Build management |
| Docker | 24+ | Infrastructure services |
| Docker Compose | 2.20+ | Container orchestration |
| Git | 2.40+ | Version control |
| IDE | Latest | Development (VS Code, IntelliJ) |

### IDE Recommendations

**IntelliJ IDEA** (Recommended for Java)
- Install Lombok plugin
- Enable annotation processing
- Import as Maven project

**VS Code**
- Extension Pack for Java
- Spring Boot Extension Pack
- Docker extension
- REST Client extension

---

## Environment Setup

### 1. Clone the Repository

```bash
git clone https://github.com/your-org/splitter.git
cd splitter
```

### 2. Start Infrastructure Services

**Windows:**
```cmd
scripts\start-dev.bat
```

**Linux/macOS:**
```bash
chmod +x scripts/*.sh
./scripts/start-dev.sh
```

This starts:
- **PostgreSQL** (localhost:5432) - Database server
- **Redis** (localhost:6379) - Caching layer
- **Kafka** (localhost:9094) - Message broker
- **Kafka UI** (http://localhost:8090) - Kafka management
- **pgAdmin** (http://localhost:5050) - Database management
- **Mailhog** (http://localhost:8025) - Email testing

### 3. Verify Services

```bash
docker compose -f infrastructure/docker/docker-compose.dev.yml ps
```

All services should show as "healthy" or "running".

### 4. Build Shared Libraries

```bash
cd shared
mvn clean install
```

---

## Running the Application

### Option 1: Run from IDE

1. Import project as Maven project
2. Navigate to service's `*Application.java`
3. Right-click → Run

### Option 2: Run from Terminal

```bash
# API Gateway
cd services/api-gateway
mvn spring-boot:run

# User Service (new terminal)
cd services/user-service
mvn spring-boot:run
```

### Option 3: Build and Run JAR

```bash
# Build all services
scripts/build-all.bat  # Windows
./scripts/build-all.sh # Linux/macOS

# Run JAR
java -jar services/user-service/target/user-service-1.0.0-SNAPSHOT.jar
```

### Service Ports

| Service | Direct Port | Via Gateway |
|---------|-------------|-------------|
| API Gateway | 8080 | - |
| User Service | 8081 | /api/v1/users |
| Group Service | 8082 | /api/v1/groups |
| Expense Service | 8083 | /api/v1/expenses |
| Balance Service | 8084 | /api/v1/balances |
| Settlement Service | 8085 | /api/v1/settlements |
| Notification Service | 8086 | /api/v1/notifications |

---

## Development Workflow

### Creating a New Feature

1. **Create feature branch**
   ```bash
   git checkout -b feature/ISSUE-123-description
   ```

2. **Implement changes**
   - Follow coding standards
   - Write tests first (TDD)
   - Keep commits atomic

3. **Run tests**
   ```bash
   mvn test
   ```

4. **Create Pull Request**
   - Use PR template
   - Request reviews
   - Address feedback

### Adding a New Microservice

1. Copy `services/service-template` directory
2. Rename directory and packages
3. Update `pom.xml` with correct artifact ID
4. Configure `application.yml`
5. Add Flyway migrations
6. Add route in API Gateway

### Creating Database Migrations

```sql
-- Location: src/main/resources/db/migration/V{version}__{description}.sql
-- Example: V2__add_user_preferences.sql

CREATE TABLE user_preferences (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    notification_enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

---

## Coding Standards

### Java Code Style

```java
// Use records for DTOs
public record CreateUserRequest(
    @NotBlank String email,
    @NotBlank String password,
    String firstName,
    String lastName
) {}

// Use Mono/Flux for reactive operations
public Mono<UserDto> createUser(CreateUserRequest request) {
    return userRepository.save(user)
            .map(this::toDto);
}

// Use descriptive method names
public Flux<UserDto> findActiveUsersByGroupId(UUID groupId) {
    // Implementation
}
```

### Reactive Programming Rules

1. **Never block** - Use reactive operators instead
2. **No `.block()` in production code**
3. **Use `subscribeOn()` for blocking I/O**
4. **Handle errors with `onErrorResume()`**

### API Design

- Use REST conventions
- Version APIs (`/api/v1/...`)
- Return appropriate HTTP status codes
- Use consistent error responses

---

## Testing

### Running Tests

```bash
# Unit tests
mvn test

# Integration tests
mvn verify

# Specific test class
mvn test -Dtest=UserServiceTest

# With coverage
mvn test jacoco:report
```

### Test Structure

```java
@Test
void shouldCreateUser_whenValidRequest() {
    // Arrange
    CreateUserRequest request = new CreateUserRequest(/*...*/);
    
    // Act & Assert
    webTestClient.post()
        .uri("/api/v1/users")
        .bodyValue(request)
        .exchange()
        .expectStatus().isCreated()
        .expectBody(UserDto.class)
        .value(user -> {
            assertThat(user.email()).isEqualTo(request.email());
        });
}
```

### Test Categories

| Type | Location | Purpose |
|------|----------|---------|
| Unit | `src/test/java` | Test individual components |
| Integration | `src/test/java` | Test component interactions |
| Contract | `src/test/java` | Test API contracts |

---

## Debugging

### Enable Debug Logging

```yaml
# application.yml
logging:
  level:
    com.splitter: DEBUG
    org.springframework.r2dbc: DEBUG
```

### Debugging Reactive Streams

```java
// Add .log() operator for debugging
userRepository.findById(userId)
    .log("findUserById")
    .map(this::toDto);
```

### Checking Kafka Events

1. Open Kafka UI: http://localhost:8090
2. Navigate to Topics
3. View messages in relevant topic

### Database Inspection

1. Open pgAdmin: http://localhost:5050
2. Login: admin@splitter.local / admin
3. Add server connection (host: postgres, user: splitter)

---

## Common Issues

### Issue: Service won't start

**Cause:** Infrastructure not running
**Solution:**
```bash
scripts/start-dev.bat
```

### Issue: Database connection refused

**Cause:** PostgreSQL not ready
**Solution:** Wait for PostgreSQL to be healthy
```bash
docker compose -f infrastructure/docker/docker-compose.dev.yml ps
```

### Issue: Kafka connection timeout

**Cause:** Kafka using wrong port
**Solution:** Use port 9094 for host connections

### Issue: Flyway migration fails

**Cause:** Invalid SQL or migration conflict
**Solution:**
```bash
# Check Flyway history
docker exec -it splitter-postgres psql -U splitter -d splitter_users -c "SELECT * FROM flyway_schema_history;"
```

### Issue: Tests fail with "blocking call"

**Cause:** Blocking operation in reactive chain
**Solution:** Use `.subscribeOn(Schedulers.boundedElastic())` for blocking calls

---

## Resources

- [Spring WebFlux Documentation](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [Project Reactor Reference](https://projectreactor.io/docs/core/release/reference/)
- [R2DBC Documentation](https://r2dbc.io/)
- [Spring Cloud Gateway](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/)

---

## Getting Help

- Create an issue for bugs
- Use discussions for questions
- Check existing issues before creating new ones
