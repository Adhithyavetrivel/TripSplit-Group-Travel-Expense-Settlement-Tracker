# TripSplit API Documentation

## Base URL
```
http://localhost:8080/api
```

## Endpoints

### Trips

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/trips` | Create a new trip with participants |
| GET | `/api/trips/{tripId}` | Get trip details with balances |
| POST | `/api/trips/{tripId}/participants` | Add a participant to a trip |

### Expenses

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/trips/{tripId}/expenses` | Create an expense (equal or custom split) |
| GET | `/api/trips/{tripId}/expenses` | List expenses with pagination |
| PUT | `/api/trips/{tripId}/expenses/{expenseId}` | Update an expense |
| DELETE | `/api/trips/{tripId}/expenses/{expenseId}` | Delete an expense |

### Balances

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/trips/{tripId}/balances` | Get current balances for all participants |

### Settlements

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/trips/{tripId}/settlement` | Get current settlement |
| POST | `/api/trips/{tripId}/settlements/generate` | Generate optimized settlements |

### Audit & History

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/trips/{tripId}/history` | Get unified trip history |
| GET | `/api/trips/{tripId}/audit-logs` | Get audit logs |

## Headers

| Header | Required | Description |
|--------|----------|-------------|
| `X-User` | No | Identifies who performed the action. Defaults to `SYSTEM`. |

## Pagination

Expense list supports pagination:
```
GET /api/trips/{tripId}/expenses?page=0&size=10&sort=expenseDate&direction=desc
```

## Error Responses

All errors follow this format:
```json
{
  "timestamp": "2026-10-10T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Expense shares must equal total expense amount",
  "path": "/api/trips/1/expenses"
}
```

## Swagger UI

Available at: `http://localhost:8080/swagger-ui.html`
