package com.tripsplit.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TripResponse {
    private Long id;
    private String name;
    private String description;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ParticipantResponse> participants;
    private int expenseCount;
    private BigDecimal totalExpenses;
    private List<BalanceResponse> balances;
}
