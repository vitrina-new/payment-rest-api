# CLAUDE.md - Java Payments Service

## Project Overview

This is a containerized Java payments service built with Spring Boot, exposing REST APIs for payment processing. The project uses Gradle for builds, GitHub Actions for CI/CD, and includes comprehensive observability tooling.

---

## Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.x |
| Build Tool | Gradle 8.x (Kotlin DSL) |
| Containerization | Docker with multi-stage builds |
| CI/CD | GitHub Actions |
| Testing | JUnit 5, Mockito, Spring Boot Test |
| Observability | Micrometer, Spring Actuator, OpenTelemetry |
| API Documentation | SpringDoc OpenAPI (Swagger) |

---

## Project Structure

```
payments-service/
├── .github/
│   └── workflows/
│       ├── ci.yml                 # CI pipeline
│       └── cd.yml                 # CD pipeline (optional)
├── src/
│   ├── main/
│   │   ├── java/com/example/payments/
│   │   │   ├── PaymentsApplication.java
│   │   │   ├── config/
│   │   │   │   ├── ObservabilityConfig.java
│   │   │   │   └── SecurityConfig.java
│   │   │   ├── controller/
│   │   │   │   └── PaymentController.java
│   │   │   ├── service/
│   │   │   │   └── PaymentService.java
│   │   │   ├── repository/
│   │   │   │   └── PaymentRepository.java
│   │   │   ├── model/
│   │   │   │   ├── Payment.java
│   │   │   │   ├── PaymentRequest.java
│   │   │   │   └── PaymentResponse.java
│   │   │   └── exception/
│   │   │       ├── GlobalExceptionHandler.java
│   │   │       └── PaymentException.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-local.yml
│   │       ├── application-prod.yml
│   │       └── logback-spring.xml
│   └── test/
│       └── java/com/example/payments/
│           ├── controller/
│           │   └── PaymentControllerTest.java
│           ├── service/
│           │   └── PaymentServiceTest.java
│           └── integration/
│               └── PaymentIntegrationTest.java
├── Dockerfile
├── docker-compose.yml
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

---

## Build Commands

### Gradle Commands

```bash
# Clean build
./gradlew clean build

# Run tests only
./gradlew test

# Run with test coverage report
./gradlew test jacocoTestReport

# Build without tests
./gradlew build -x test

# Run the application locally
./gradlew bootRun

# Run with specific profile
./gradlew bootRun --args='--spring.profiles.active=local'

# Build Docker image using Spring Boot plugin
./gradlew bootBuildImage

# Check for dependency updates
./gradlew dependencyUpdates

# Format code (if using Spotless)
./gradlew spotlessApply
```

### Docker Commands

```bash
# Build Docker image
docker build -t payments-service:latest .

# Run container
docker run -p 8080:8080 payments-service:latest

# Run with environment variables
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL=jdbc:postgresql://host:5432/payments \
  payments-service:latest

# Docker Compose (full stack with dependencies)
docker-compose up -d

# View logs
docker-compose logs -f payments-service
```

---

## API Endpoints

### Payment Operations

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/payments` | Create a new payment |
| GET | `/api/v1/payments/{id}` | Get payment by ID |
| GET | `/api/v1/payments` | List payments (paginated) |
| PUT | `/api/v1/payments/{id}` | Update payment |
| DELETE | `/api/v1/payments/{id}` | Cancel/delete payment |
| POST | `/api/v1/payments/{id}/refund` | Refund a payment |

### Health & Observability Endpoints

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Health check (liveness) |
| `/actuator/health/readiness` | Readiness probe |
| `/actuator/info` | Application info |
| `/actuator/metrics` | Micrometer metrics |
| `/actuator/prometheus` | Prometheus metrics endpoint |
| `/swagger-ui.html` | API documentation UI |
| `/v3/api-docs` | OpenAPI spec (JSON) |

---

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Application port | `8080` |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `local` |
| `DATABASE_URL` | Database JDBC URL | - |
| `DATABASE_USERNAME` | Database username | - |
| `DATABASE_PASSWORD` | Database password | - |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | OpenTelemetry collector endpoint | - |
| `OTEL_SERVICE_NAME` | Service name for tracing | `payments-service` |
| `LOG_LEVEL` | Root logging level | `INFO` |

### Application Profiles

- **local**: Development settings, H2 in-memory database, debug logging
- **test**: Test configuration, mocked external services
- **prod**: Production settings, external database, structured JSON logging

---

## Testing Guidelines

### Test Categories

1. **Unit Tests** (`src/test/java/.../`)
   - Test individual components in isolation
   - Mock all dependencies
   - Fast execution, no Spring context loading
   - Naming: `*Test.java`

2. **Integration Tests** (`src/test/java/.../integration/`)
   - Test component interactions
   - Use `@SpringBootTest` with test containers
   - Naming: `*IntegrationTest.java`

3. **Controller Tests**
   - Use `@WebMvcTest` for slice testing
   - Test request/response serialization
   - Verify HTTP status codes and headers

### Running Tests

```bash
# All tests
./gradlew test

# Unit tests only
./gradlew test --tests "*Test"

# Integration tests only
./gradlew test --tests "*IntegrationTest"

# Single test class
./gradlew test --tests "PaymentServiceTest"

# With coverage
./gradlew test jacocoTestReport
# Report at: build/reports/jacoco/test/html/index.html
```

### Test Coverage Requirements

- Minimum line coverage: 80%
- Minimum branch coverage: 70%
- Critical paths (payment processing): 90%+

---

## Observability

### Logging

- **Format**: JSON in production, human-readable locally
- **Framework**: SLF4J with Logback
- **Correlation**: Trace IDs included in all log entries
- **Levels**: ERROR, WARN, INFO, DEBUG, TRACE

```java
// Always use structured logging with context
log.info("Payment processed", 
    kv("paymentId", payment.getId()),
    kv("amount", payment.getAmount()),
    kv("status", payment.getStatus()));
```

### Metrics

Key metrics exposed via Micrometer:

- `payments.created.total` - Counter of created payments
- `payments.processed.total` - Counter by status (success/failure)
- `payments.amount.total` - Sum of payment amounts
- `payments.processing.duration` - Histogram of processing time
- `http.server.requests` - HTTP request metrics (auto-configured)
- `jvm.*` - JVM metrics (memory, GC, threads)

### Tracing

- **Protocol**: OpenTelemetry (OTLP)
- **Propagation**: W3C Trace Context
- **Spans**: Auto-instrumented for HTTP, database, and custom spans for business operations

### Health Checks

```yaml
# Kubernetes probes configuration
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 5
  periodSeconds: 5
```

---

## CI/CD Pipeline

### GitHub Actions Workflow

The CI pipeline (`.github/workflows/ci.yml`) performs:

1. **Build & Test**
   - Checkout code
   - Set up Java 21
   - Cache Gradle dependencies
   - Run `./gradlew build`
   - Run tests with coverage

2. **Code Quality**
   - Static analysis (SpotBugs/PMD)
   - Code formatting check
   - Dependency vulnerability scan

3. **Docker Build**
   - Build Docker image
   - Push to container registry (on main branch)

4. **Deploy** (CD)
   - Deploy to staging (on PR merge)
   - Deploy to production (on release tag)

### Branch Strategy

- `main` - Production-ready code
- `develop` - Integration branch
- `feature/*` - Feature branches
- `hotfix/*` - Production hotfixes

### Required Checks

- All tests pass
- Code coverage >= 80%
- No critical security vulnerabilities
- Docker image builds successfully

---

## Development Workflow

### Setting Up Local Environment

```bash
# Clone repository
git clone <repo-url>
cd payments-service

# Start dependencies (database, etc.)
docker-compose up -d postgres redis

# Run application
./gradlew bootRun

# Or run everything in Docker
docker-compose up -d
```

### Making Changes

1. Create feature branch from `develop`
2. Make changes following code style guidelines
3. Write/update tests
4. Run `./gradlew build` locally
5. Commit with conventional commit messages
6. Push and create PR

### Commit Message Format

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

Examples:
- `feat(payments): add refund endpoint`
- `fix(validation): handle null amount in request`
- `test(service): add unit tests for PaymentService`

---

## Security Considerations

### API Security

- All endpoints require authentication (except health checks)
- Use JWT tokens for API authentication
- Rate limiting enabled (100 req/min per client)
- Input validation on all request DTOs
- SQL injection prevention via parameterized queries

### Sensitive Data

- Never log full card numbers or CVV
- Mask PII in logs (show last 4 digits only)
- Encrypt sensitive data at rest
- Use secrets management for credentials (not env vars in production)

### Dependencies

- Regular dependency updates via Dependabot
- Security scanning in CI pipeline
- No dependencies with known critical vulnerabilities

---

## Common Tasks

### Adding a New Endpoint

1. Define request/response DTOs in `model/`
2. Add controller method in `PaymentController`
3. Implement business logic in `PaymentService`
4. Add validation annotations to DTOs
5. Write unit tests for service
6. Write controller tests for endpoint
7. Update OpenAPI documentation

### Adding a New Metric

```java
// In service class
private final Counter paymentCounter;

public PaymentService(MeterRegistry registry) {
    this.paymentCounter = Counter.builder("payments.created")
        .description("Number of payments created")
        .tag("type", "credit")
        .register(registry);
}

// Usage
paymentCounter.increment();
```

### Debugging

```bash
# Enable debug logging
./gradlew bootRun --args='--logging.level.com.example.payments=DEBUG'

# Remote debugging
./gradlew bootRun --debug-jvm
# Then attach debugger to port 5005

# View container logs
docker logs -f payments-service
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Build fails with OOM | Increase Gradle heap: `org.gradle.jvmargs=-Xmx2g` |
| Tests fail with connection refused | Ensure test containers are running |
| Docker build fails | Check Docker daemon is running, clear build cache |
| Application won't start | Check logs, verify all env vars are set |
| Health check failing | Verify database connectivity, check `/actuator/health` |

---

## Reference Documentation

- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer Documentation](https://micrometer.io/docs)
- [OpenTelemetry Java](https://opentelemetry.io/docs/instrumentation/java/)
- [Gradle User Guide](https://docs.gradle.org/current/userguide/userguide.html)
- [Docker Best Practices](https://docs.docker.com/develop/develop-images/dockerfile_best-practices/)
