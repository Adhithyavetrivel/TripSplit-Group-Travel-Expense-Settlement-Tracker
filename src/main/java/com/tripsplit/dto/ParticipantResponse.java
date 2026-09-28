package com.tripsplit.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ParticipantResponse {
    private Long id;
    private String name;
    private String email;
    private LocalDateTime createdAt;
}
