package com.tripsplit.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SettlementResponse {
    private Long fromParticipantId;
    private String fromParticipantName;
    private Long toParticipantId;
    private String toParticipantName;
    private BigDecimal amount;
}
