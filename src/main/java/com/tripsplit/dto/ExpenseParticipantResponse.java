package com.tripsplit.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ExpenseParticipantResponse {
    private Long participantId;
    private String participantName;
    private BigDecimal owedAmount;
}
