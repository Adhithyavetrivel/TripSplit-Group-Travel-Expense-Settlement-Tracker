package com.tripsplit.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class BalanceResponse {
    private Long participantId;
    private String participantName;
    private BigDecimal totalPaid;
    private BigDecimal totalOwed;
    private BigDecimal netBalance;
    private String status; // RECEIVE, PAY, SETTLED
}
