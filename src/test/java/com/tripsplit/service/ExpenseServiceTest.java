package com.tripsplit.service;

import com.tripsplit.dto.*;
import com.tripsplit.entity.*;
import com.tripsplit.exception.BadRequestException;
import com.tripsplit.exception.ResourceNotFoundException;
import com.tripsplit.mapper.TripMapper;
import com.tripsplit.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock private ExpenseRepository expenseRepository;
    @Mock private TripRepository tripRepository;
    @Mock private ParticipantRepository participantRepository;
    @Mock private ExpenseParticipantRepository expenseParticipantRepository;
    @Mock private SettlementRepository settlementRepository;
    @Mock private AuditService auditService;
    @Spy private TripMapper tripMapper = new TripMapper();

    @InjectMocks
    private ExpenseService expenseService;

    private Trip trip;
    private Participant alice, bob, charlie;

    @BeforeEach
    void setUp() {
        trip = Trip.builder().id(1L).name("Test Trip").participants(new ArrayList<>())
                .expenses(new ArrayList<>()).settlements(new ArrayList<>()).auditLogs(new ArrayList<>())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        alice = Participant.builder().id(1L).name("Alice").email("alice@example.com").trip(trip).createdAt(LocalDateTime.now()).build();
        bob = Participant.builder().id(2L).name("Bob").email("bob@example.com").trip(trip).createdAt(LocalDateTime.now()).build();
        charlie = Participant.builder().id(3L).name("Charlie").email("charlie@example.com").trip(trip).createdAt(LocalDateTime.now()).build();
        trip.getParticipants().addAll(List.of(alice, bob, charlie));
    }

    @Test
    void createExpense_equalSplit_shouldCalculateCorrectly() {
        ExpenseRequest request = ExpenseRequest.builder()
                .description("Dinner")
                .amount(BigDecimal.valueOf(2400))
                .payerId(1L)
                .expenseDate(LocalDate.now())
                .participants(List.of(
                        ExpenseParticipantRequest.builder().participantId(1L).build(),
                        ExpenseParticipantRequest.builder().participantId(2L).build(),
                        ExpenseParticipantRequest.builder().participantId(3L).build()
                ))
                .build();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(participantRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(participantRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(participantRepository.findById(3L)).thenReturn(Optional.of(charlie));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> {
            Expense e = inv.getArgument(0);
            e.setId(1L);
            e.setCreatedAt(LocalDateTime.now());
            e.setUpdatedAt(LocalDateTime.now());
            return e;
        });

        ExpenseResponse response = expenseService.createExpense(1L, request, "TestUser");

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(2400), response.getAmount());
        assertEquals(3, response.getParticipants().size());
        // 2400 / 3 = 800.00 each
        for (ExpenseParticipantResponse ep : response.getParticipants()) {
            assertEquals(0, ep.getOwedAmount().compareTo(BigDecimal.valueOf(800.00)));
        }
    }

    @Test
    void createExpense_customSplit_shouldValidateTotal() {
        ExpenseRequest request = ExpenseRequest.builder()
                .description("Hotel")
                .amount(BigDecimal.valueOf(5000))
                .payerId(1L)
                .expenseDate(LocalDate.now())
                .participants(List.of(
                        ExpenseParticipantRequest.builder().participantId(1L).owedAmount(BigDecimal.valueOf(2000)).build(),
                        ExpenseParticipantRequest.builder().participantId(2L).owedAmount(BigDecimal.valueOf(1500)).build(),
                        ExpenseParticipantRequest.builder().participantId(3L).owedAmount(BigDecimal.valueOf(1500)).build()
                ))
                .build();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(participantRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(participantRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(participantRepository.findById(3L)).thenReturn(Optional.of(charlie));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> {
            Expense e = inv.getArgument(0);
            e.setId(1L);
            e.setCreatedAt(LocalDateTime.now());
            e.setUpdatedAt(LocalDateTime.now());
            return e;
        });

        ExpenseResponse response = expenseService.createExpense(1L, request, "TestUser");

        assertNotNull(response);
        assertEquals(3, response.getParticipants().size());
    }

    @Test
    void createExpense_invalidCustomSplit_shouldThrow() {
        ExpenseRequest request = ExpenseRequest.builder()
                .description("Hotel")
                .amount(BigDecimal.valueOf(5000))
                .payerId(1L)
                .expenseDate(LocalDate.now())
                .participants(List.of(
                        ExpenseParticipantRequest.builder().participantId(1L).owedAmount(BigDecimal.valueOf(2000)).build(),
                        ExpenseParticipantRequest.builder().participantId(2L).owedAmount(BigDecimal.valueOf(1500)).build(),
                        ExpenseParticipantRequest.builder().participantId(3L).owedAmount(BigDecimal.valueOf(1000)).build()
                ))
                .build();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(participantRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(participantRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(participantRepository.findById(3L)).thenReturn(Optional.of(charlie));

        assertThrows(BadRequestException.class, () -> expenseService.createExpense(1L, request, "TestUser"));
    }

    @Test
    void createExpense_payerNotInTrip_shouldThrow() {
        Trip otherTrip = Trip.builder().id(2L).name("Other").build();
        Participant outsider = Participant.builder().id(99L).name("Outsider").trip(otherTrip).build();

        ExpenseRequest request = ExpenseRequest.builder()
                .description("Dinner")
                .amount(BigDecimal.valueOf(1000))
                .payerId(99L)
                .participants(List.of(ExpenseParticipantRequest.builder().participantId(1L).build()))
                .build();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(participantRepository.findById(99L)).thenReturn(Optional.of(outsider));

        assertThrows(BadRequestException.class, () -> expenseService.createExpense(1L, request, "TestUser"));
    }

    @Test
    void createExpense_participantNotInTrip_shouldThrow() {
        Trip otherTrip = Trip.builder().id(2L).name("Other").build();
        Participant outsider = Participant.builder().id(99L).name("Outsider").trip(otherTrip).build();

        ExpenseRequest request = ExpenseRequest.builder()
                .description("Dinner")
                .amount(BigDecimal.valueOf(1000))
                .payerId(1L)
                .participants(List.of(
                        ExpenseParticipantRequest.builder().participantId(1L).build(),
                        ExpenseParticipantRequest.builder().participantId(99L).build()
                ))
                .build();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(participantRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(participantRepository.findById(99L)).thenReturn(Optional.of(outsider));

        assertThrows(BadRequestException.class, () -> expenseService.createExpense(1L, request, "TestUser"));
    }

    @Test
    void createExpense_duplicateParticipant_shouldThrow() {
        ExpenseRequest request = ExpenseRequest.builder()
                .description("Dinner")
                .amount(BigDecimal.valueOf(1000))
                .payerId(1L)
                .participants(List.of(
                        ExpenseParticipantRequest.builder().participantId(1L).build(),
                        ExpenseParticipantRequest.builder().participantId(1L).build()
                ))
                .build();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(participantRepository.findById(1L)).thenReturn(Optional.of(alice));

        assertThrows(BadRequestException.class, () -> expenseService.createExpense(1L, request, "TestUser"));
    }

    @Test
    void deleteExpense_shouldDelete() {
        Expense expense = Expense.builder().id(1L).trip(trip).description("Dinner")
                .amount(BigDecimal.valueOf(1000)).payer(alice).expenseParticipants(new ArrayList<>())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(expenseRepository.findByIdAndTripId(1L, 1L)).thenReturn(Optional.of(expense));

        assertDoesNotThrow(() -> expenseService.deleteExpense(1L, 1L, "TestUser"));
        verify(expenseRepository).delete(expense);
    }
}
