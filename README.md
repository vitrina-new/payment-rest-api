# Payments REST API

A production-ready Java payments service built with Spring Boot, exposing REST APIs for payment processing.

## Technology Stack

- **Java 21** with **Spring Boot 3.x**
- **Gradle 8.x** (Kotlin DSL)
- **PostgreSQL** (production) / **H2** (development)
- **SpringDoc OpenAPI** for API documentation

## Quick Start

```bash
# Run with Docker Compose
docker-compose up -d

# Or run locally
./gradlew bootRun
```

The API will be available at `http://localhost:8080`

## API Documentation

Interactive API documentation is available at:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs

---

## REST API Endpoints

Base URL: `/api/v1/payments`

### Create Payment

Creates a new payment in `PENDING` status.

```
POST /api/v1/payments
```

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `amount` | BigDecimal | Yes | Payment amount (min: 0.01, max 2 decimal places) |
| `currency` | String | Yes | 3-letter ISO currency code (e.g., USD, EUR) |
| `merchantId` | String | Yes | Merchant identifier |
| `customerId` | String | Yes | Customer identifier |
| `description` | String | No | Payment description (max 500 chars) |
| `cardLastFour` | String | No | Last 4 digits of card (exactly 4 digits) |
| `paymentMethod` | String | No | Payment method identifier |
| `referenceId` | String | No | External reference ID |

**Example Request:**

```json
{
  "amount": 99.99,
  "currency": "USD",
  "merchantId": "merchant-123",
  "customerId": "customer-456",
  "description": "Order #12345",
  "cardLastFour": "4242",
  "paymentMethod": "card"
}
```

**Response:** `201 Created`

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "amount": 99.99,
  "currency": "USD",
  "status": "PENDING",
  "description": "Order #12345",
  "merchantId": "merchant-123",
  "customerId": "customer-456",
  "cardLastFour": "4242",
  "paymentMethod": "card",
  "referenceId": null,
  "createdAt": "2025-12-11T10:30:00Z",
  "updatedAt": null,
  "processedAt": null
}
```

---

### Get Payment

Retrieves a payment by its unique identifier.

```
GET /api/v1/payments/{id}
```

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Payment ID |

**Response:** `200 OK`

Returns the payment object (same structure as create response).

---

### List Payments

Retrieves a paginated list of payments with optional filtering.

```
GET /api/v1/payments
```

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `merchantId` | String | No | - | Filter by merchant ID |
| `page` | Integer | No | 0 | Page number (0-indexed) |
| `size` | Integer | No | 20 | Page size |
| `sort` | String | No | - | Sort field and direction (e.g., `createdAt,desc`) |

**Example Request:**

```
GET /api/v1/payments?merchantId=merchant-123&page=0&size=10&sort=createdAt,desc
```

**Response:** `200 OK`

```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "amount": 99.99,
      "currency": "USD",
      "status": "COMPLETED",
      ...
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 1,
  "totalPages": 1
}
```

---

### Update Payment

Updates an existing payment. Only payments in `PENDING` or `PROCESSING` status can be updated.

```
PUT /api/v1/payments/{id}
```

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Payment ID |

**Request Body:** Same structure as create payment.

**Response:** `200 OK`

Returns the updated payment object.

---

### Cancel Payment

Cancels a payment. Only payments in `PENDING` or `PROCESSING` status can be cancelled.

```
DELETE /api/v1/payments/{id}
```

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Payment ID |

**Response:** `204 No Content`

---

### Process Payment

Processes a pending payment. Transitions the payment from `PENDING` to `COMPLETED` or `FAILED`.

```
POST /api/v1/payments/{id}/process
```

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Payment ID |

**Response:** `200 OK`

Returns the processed payment object with updated status and `processedAt` timestamp.

---

### Refund Payment

Refunds a completed payment. Only payments in `COMPLETED` status can be refunded.

```
POST /api/v1/payments/{id}/refund
```

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Payment ID |

**Response:** `200 OK`

Returns the payment object with status changed to `REFUNDED`.

---

## Payment Status Lifecycle

```
PENDING → PROCESSING → COMPLETED → REFUNDED
    ↓         ↓
 CANCELLED  FAILED
```

| Status | Description |
|--------|-------------|
| `PENDING` | Payment created, awaiting processing |
| `PROCESSING` | Payment is being processed |
| `COMPLETED` | Payment successfully processed |
| `FAILED` | Payment processing failed |
| `CANCELLED` | Payment was cancelled |
| `REFUNDED` | Payment was refunded |

---

## Error Responses

All errors return a consistent JSON structure:

```json
{
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Payment not found with ID: 550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2025-12-11T10:30:00Z"
}
```

### Error Codes

| HTTP Status | Error Code | Description |
|-------------|------------|-------------|
| 400 | `VALIDATION_ERROR` | Invalid request data |
| 404 | `NOT_FOUND` | Payment not found |
| 409 | `CONFLICT` | Payment cannot be modified in current state |
| 409 | `INVALID_STATE` | Operation not allowed for current payment status |
| 500 | `INTERNAL_ERROR` | Unexpected server error |

### Validation Error Response

Validation errors include field-level details:

```json
{
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "timestamp": "2025-12-11T10:30:00Z",
  "fieldErrors": {
    "amount": "Amount is required",
    "currency": "Currency must be a 3-letter ISO code"
  }
}
```

---

## Health & Observability Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/health` | Health check |
| `GET /actuator/health/liveness` | Kubernetes liveness probe |
| `GET /actuator/health/readiness` | Kubernetes readiness probe |
| `GET /actuator/metrics` | Application metrics |
| `GET /actuator/prometheus` | Prometheus metrics |

---

## Build & Run

```bash
# Build
./gradlew clean build

# Run tests
./gradlew test

# Run with coverage report
./gradlew test jacocoTestReport

# Build Docker image
docker build -t payments-service:latest .

# Run with Docker
docker run -p 8080:8080 payments-service:latest
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Application port | `8080` |
| `SPRING_PROFILES_ACTIVE` | Active profile | `local` |
| `DATABASE_URL` | Database JDBC URL | H2 in-memory |
| `DATABASE_USERNAME` | Database username | - |
| `DATABASE_PASSWORD` | Database password | - |

### Profiles

- **local**: H2 in-memory database, debug logging
- **test**: Test configuration with mocked services
- **prod**: PostgreSQL, structured JSON logging
