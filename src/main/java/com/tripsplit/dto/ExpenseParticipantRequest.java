package com.tripsplit.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ExpenseParticipantRequest {
    @NotNull(message = "Participant ID is required")
    private Long participantId;

    // If null, will be auto-calculated for equal split
    private BigDecimal owedAmount;
}
