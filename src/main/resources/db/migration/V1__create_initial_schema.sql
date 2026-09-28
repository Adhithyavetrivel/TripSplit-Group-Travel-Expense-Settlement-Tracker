CREATE TABLE trips (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    destination VARCHAR(255),
    start_date DATE,
    end_date DATE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME
);

CREATE TABLE participants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    trip_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_participant_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE,
    CONSTRAINT uk_participant_email_trip UNIQUE (trip_id, email)
);

CREATE INDEX idx_participant_trip ON participants(trip_id);

CREATE TABLE expenses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    description VARCHAR(255) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    payer_id BIGINT NOT NULL,
    expense_date DATE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT fk_expense_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE,
    CONSTRAINT fk_expense_payer FOREIGN KEY (payer_id) REFERENCES participants(id)
);

CREATE INDEX idx_expense_trip ON expenses(trip_id);
CREATE INDEX idx_expense_payer ON expenses(payer_id);
CREATE INDEX idx_expense_date ON expenses(expense_date);

CREATE TABLE expense_participants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    expense_id BIGINT NOT NULL,
    participant_id BIGINT NOT NULL,
    owed_amount DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_ep_expense FOREIGN KEY (expense_id) REFERENCES expenses(id) ON DELETE CASCADE,
    CONSTRAINT fk_ep_participant FOREIGN KEY (participant_id) REFERENCES participants(id)
);

CREATE TABLE settlements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    from_participant_id BIGINT NOT NULL,
    to_participant_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_settlement_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE,
    CONSTRAINT fk_settlement_from FOREIGN KEY (from_participant_id) REFERENCES participants(id),
    CONSTRAINT fk_settlement_to FOREIGN KEY (to_participant_id) REFERENCES participants(id)
);

CREATE INDEX idx_settlement_trip ON settlements(trip_id);

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id BIGINT,
    details TEXT,
    changed_by VARCHAR(255),
    changed_at DATETIME NOT NULL,
    CONSTRAINT fk_audit_trip FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE
);

CREATE INDEX idx_audit_trip ON audit_logs(trip_id);
