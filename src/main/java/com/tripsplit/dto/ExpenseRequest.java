package com.tripsplit.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ExpenseRequest {
    @NotBlank(message = "Expense description is required")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Payer ID is required")
    private Long payerId;

    private LocalDate expenseDate;

    @Valid
    @NotEmpty(message = "At least one participant is required")
    private List<ExpenseParticipantRequest> participants;
}
