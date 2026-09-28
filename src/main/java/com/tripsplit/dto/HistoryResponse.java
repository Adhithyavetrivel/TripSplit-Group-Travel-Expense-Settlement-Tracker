package com.tripsplit.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class HistoryResponse {
    private String eventType; // TRIP_CREATED, PARTICIPANT_ADDED, EXPENSE_CREATED, etc.
    private String description;
    private String performedBy;
    private LocalDateTime timestamp;
    private Long entityId;
    private String entityType;
}
