package com.tripsplit.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TripRequest {
    @NotBlank(message = "Trip name is required")
    private String name;

    private String description;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;

    @Valid
    @Size(min = 2, message = "At least 2 participants are required")
    private List<ParticipantRequest> participants;
}
