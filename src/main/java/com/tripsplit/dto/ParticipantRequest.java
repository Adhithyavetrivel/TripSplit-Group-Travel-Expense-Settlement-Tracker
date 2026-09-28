package com.tripsplit.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ParticipantRequest {
    @NotBlank(message = "Participant name is required")
    private String name;

    @Email(message = "Invalid email format")
    private String email;
}
