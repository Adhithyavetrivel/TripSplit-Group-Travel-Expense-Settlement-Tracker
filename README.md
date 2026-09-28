# TripSplit — Group Travel Expense Settlement Tracker

A Spring Boot backend application for tracking and settling shared expenses during group trips.

## Features

- **Trip Management** — Create trips with participants, destinations, and dates
- **Expense Tracking** — Record expenses with equal or custom splits
- **Balance Calculation** — Real-time balance computation with sum-zero invariant verification
- **Settlement Generation** — Optimized debt settlement with minimal transactions
- **Settlement Verification** — Automated verification that settlements clear all balances
- **Audit Logging** — Complete audit trail of all changes
- **Pagination & Sorting** — Paginated expense listing with flexible sorting
- **Caching** — In-memory balance caching with automatic invalidation
- **Interactive Web Interface** — Server-rendered Thymeleaf UI with dark sidebar, modals, dynamic splits, and live toasts
- **API Documentation** — Interactive Swagger UI

## Web Frontend Pages

Access in your browser at `http://localhost:8080`:

| Page | URL | Description |
|------|-----|-------------|
| Dashboard | `GET /dashboard?tripId={id}` | Trip overview, expense stats, participant cards, and balance summary |
| Create Trip | `GET /trips/create` | Dynamic participant addition form and trip creation |
| Expenses | `GET /trips/{id}/expenses` | Expense history with server pagination, sorting, and add/edit modals |
| Balances | `GET /trips/{id}/balances` | Participant ledger with zero-sum invariant verification |
| Settlement | `GET /trips/{id}/settlement` | Visual debt settlement flow with one-click generation and verification |
| Activity | `GET /trips/{id}/activity` | Trip history and chronological system audit logs |

## Technology Stack

| Technology | Version |
|------------|--------|
| Java | 17 LTS |
| Spring Boot | 3.3.x |
| Thymeleaf | 3.1.x |
| Spring Data JPA | 3.3.x |
| Hibernate | 6.x |
| MySQL | 8.0 |
| Flyway | 10.x |
| Lombok | Latest |
| Springdoc OpenAPI | 2.6.x |
| JUnit 5 | Latest |
| Mockito | Latest |
| H2 (tests) | Latest |

## Architecture

```
Controller → Service → Repository → Database
     ↓          ↓
    DTO      Entity
     ↓
   Mapper
```

Clean layered architecture with:
- Constructor injection
- DTOs for API contracts
- Service layer for business logic
- Repository layer for data access
- Global exception handling

## Database Design

See [docs/database-schema.md](docs/database-schema.md) for the complete schema.

## Business Rules

1. **BigDecimal** is used for all monetary values (never float/double)
2. **SUM(net balances) = 0** invariant is enforced for every trip
3. Settlement algorithm minimizes the number of transactions
4. Generated settlements are verified by simulation
5. Expense changes invalidate cached balances and existing settlements
6. Duplicate participant emails within a trip are prevented
7. Custom expense shares must equal the total expense amount

## API Endpoints

See [docs/api.md](docs/api.md) for the full API reference.

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/trips` | Create trip |
| GET | `/api/trips/{id}` | Get trip details |
| POST | `/api/trips/{id}/participants` | Add participant |
| POST | `/api/trips/{id}/expenses` | Create expense |
| GET | `/api/trips/{id}/expenses` | List expenses (paginated) |
| PUT | `/api/trips/{id}/expenses/{eid}` | Update expense |
| DELETE | `/api/trips/{id}/expenses/{eid}` | Delete expense |
| GET | `/api/trips/{id}/balances` | Get balances |
| GET | `/api/trips/{id}/settlement` | Get settlement |
| POST | `/api/trips/{id}/settlements/generate` | Generate settlement |
| GET | `/api/trips/{id}/history` | Get history |
| GET | `/api/trips/{id}/audit-logs` | Get audit logs |

## Setup Instructions

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8.0+

### MySQL Setup
```sql
CREATE DATABASE tripsplit;
```

### Environment Variables
Copy `.env.example` to `.env` and update:
```
DB_URL=jdbc:mysql://localhost:3306/tripsplit
DB_USERNAME=root
DB_PASSWORD=your_password
```

### Build & Run
```bash
# Build
mvn clean package

# Run tests
mvn clean test

# Run application
mvn spring-boot:run

# Or run the JAR
java -jar target/tripsplit-1.0.0.jar
```

### Swagger UI
After starting the app, visit:
```
http://localhost:8080/swagger-ui.html
```

## Docker Instructions
```bash
# Build and run with Docker Compose
docker-compose up --build

# Stop
docker-compose down
```

## Testing
```bash
# Run all tests
mvn clean test

# Run specific test class
mvn test -Dtest=TripServiceTest

# Run integration tests
mvn test -Dtest=TripSplitIntegrationTest
```

Tests use H2 in-memory database.

## Settlement Example

Given expenses:
- Alice pays ₹2400 for dinner (split equally among Alice, Bob, Charlie)
- Bob pays ₹5000 for hotel (custom split: Alice ₹2000, Bob ₹1500, Charlie ₹1500)

Balances:
| Participant | Paid | Owed | Net Balance | Status |
|-------------|------|------|-------------|--------|
| Alice | ₹2400 | ₹2800 | -₹400 | PAY |
| Bob | ₹5000 | ₹2300 | +₹2700 | RECEIVE |
| Charlie | ₹0 | ₹2300 | -₹2300 | PAY |

Generated settlements:
- Alice → Bob ₹400
- Charlie → Bob ₹2300

## Future Improvements
- User authentication with Spring Security
- Currency support and conversion
- Email notifications for settlements
- Receipt image uploads
- Trip sharing via invite links
- Mobile-friendly REST API versioning
- WebSocket for real-time updates
- Export to PDF/CSV
