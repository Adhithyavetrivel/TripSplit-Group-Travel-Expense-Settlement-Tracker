package com.tripsplit.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ExpenseResponse {
    private Long id;
    private String description;
    private BigDecimal amount;
    private Long payerId;
    private String payerName;
    private LocalDate expenseDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ExpenseParticipantResponse> participants;
}
