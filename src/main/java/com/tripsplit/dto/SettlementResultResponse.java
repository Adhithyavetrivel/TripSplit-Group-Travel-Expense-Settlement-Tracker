package com.tripsplit.dto;

import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SettlementResultResponse {
    private Long tripId;
    private List<SettlementResponse> settlements;
    private boolean verified;
}
