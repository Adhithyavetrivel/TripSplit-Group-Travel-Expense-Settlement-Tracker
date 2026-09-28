# TripSplit Database Schema

## Entity Relationship Diagram

```
trips
├── participants (1:N)
├── expenses (1:N)
│   └── expense_participants (1:N)
├── settlements (1:N)
└── audit_logs (1:N)
```

## Tables

### trips
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| name | VARCHAR(255) | NOT NULL |
| description | VARCHAR(500) | |
| destination | VARCHAR(255) | |
| start_date | DATE | |
| end_date | DATE | |
| created_at | DATETIME | NOT NULL |
| updated_at | DATETIME | |

### participants
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| name | VARCHAR(255) | NOT NULL |
| email | VARCHAR(255) | |
| trip_id | BIGINT | FK → trips(id), NOT NULL |
| created_at | DATETIME | NOT NULL |

**Unique Constraint:** (trip_id, email)

### expenses
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| trip_id | BIGINT | FK → trips(id), NOT NULL |
| description | VARCHAR(255) | NOT NULL |
| amount | DECIMAL(12,2) | NOT NULL |
| payer_id | BIGINT | FK → participants(id), NOT NULL |
| expense_date | DATE | |
| created_at | DATETIME | NOT NULL |
| updated_at | DATETIME | |

### expense_participants
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| expense_id | BIGINT | FK → expenses(id), NOT NULL |
| participant_id | BIGINT | FK → participants(id), NOT NULL |
| owed_amount | DECIMAL(12,2) | NOT NULL |

### settlements
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| trip_id | BIGINT | FK → trips(id), NOT NULL |
| from_participant_id | BIGINT | FK → participants(id), NOT NULL |
| to_participant_id | BIGINT | FK → participants(id), NOT NULL |
| amount | DECIMAL(12,2) | NOT NULL |
| created_at | DATETIME | NOT NULL |

### audit_logs
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT |
| trip_id | BIGINT | FK → trips(id), NOT NULL |
| action | VARCHAR(100) | NOT NULL |
| entity_type | VARCHAR(100) | |
| entity_id | BIGINT | |
| details | TEXT | |
| changed_by | VARCHAR(255) | |
| changed_at | DATETIME | NOT NULL |

## Indexes
- `idx_participant_trip` on participants(trip_id)
- `idx_expense_trip` on expenses(trip_id)
- `idx_expense_payer` on expenses(payer_id)
- `idx_expense_date` on expenses(expense_date)
- `idx_settlement_trip` on settlements(trip_id)
- `idx_audit_trip` on audit_logs(trip_id)
