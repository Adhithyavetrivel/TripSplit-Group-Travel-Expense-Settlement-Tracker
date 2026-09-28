package com.tripsplit.mapper;

import com.tripsplit.dto.*;
import com.tripsplit.entity.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TripMapper {

    public Trip toEntity(TripRequest request) {
        Trip trip = Trip.builder()
                .name(request.getName())
                .description(request.getDescription())
                .destination(request.getDestination())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();
        return trip;
    }

    public TripResponse toResponse(Trip trip, int expenseCount, BigDecimal totalExpenses, List<BalanceResponse> balances) {
        return TripResponse.builder()
                .id(trip.getId())
                .name(trip.getName())
                .description(trip.getDescription())
                .destination(trip.getDestination())
                .startDate(trip.getStartDate())
                .endDate(trip.getEndDate())
                .createdAt(trip.getCreatedAt())
                .updatedAt(trip.getUpdatedAt())
                .participants(trip.getParticipants().stream()
                        .map(this::toParticipantResponse)
                        .collect(Collectors.toList()))
                .expenseCount(expenseCount)
                .totalExpenses(totalExpenses)
                .balances(balances)
                .build();
    }

    public ParticipantResponse toParticipantResponse(Participant participant) {
        return ParticipantResponse.builder()
                .id(participant.getId())
                .name(participant.getName())
                .email(participant.getEmail())
                .createdAt(participant.getCreatedAt())
                .build();
    }

    public ExpenseResponse toExpenseResponse(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .payerId(expense.getPayer().getId())
                .payerName(expense.getPayer().getName())
                .expenseDate(expense.getExpenseDate())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .participants(expense.getExpenseParticipants().stream()
                        .map(ep -> ExpenseParticipantResponse.builder()
                                .participantId(ep.getParticipant().getId())
                                .participantName(ep.getParticipant().getName())
                                .owedAmount(ep.getOwedAmount())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    public SettlementResponse toSettlementResponse(Settlement settlement) {
        return SettlementResponse.builder()
                .fromParticipantId(settlement.getFromParticipant().getId())
                .fromParticipantName(settlement.getFromParticipant().getName())
                .toParticipantId(settlement.getToParticipant().getId())
                .toParticipantName(settlement.getToParticipant().getName())
                .amount(settlement.getAmount())
                .build();
    }

    public AuditLogResponse toAuditLogResponse(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .action(auditLog.getAction())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .details(auditLog.getDetails())
                .changedBy(auditLog.getChangedBy())
                .changedAt(auditLog.getChangedAt())
                .build();
    }
}
