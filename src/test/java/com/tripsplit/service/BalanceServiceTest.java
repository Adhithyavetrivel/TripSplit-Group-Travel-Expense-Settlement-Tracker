package com.tripsplit.service;

import com.tripsplit.dto.BalanceResponse;
import com.tripsplit.entity.*;
import com.tripsplit.exception.ResourceNotFoundException;
import com.tripsplit.repository.ExpenseRepository;
import com.tripsplit.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock private TripRepository tripRepository;
    @Mock private ExpenseRepository expenseRepository;

    @InjectMocks
    private BalanceService balanceService;

    private Trip trip;
    private Participant alice, bob, charlie;

    @BeforeEach
    void setUp() {
        trip = Trip.builder().id(1L).name("Test").participants(new ArrayList<>())
                .expenses(new ArrayList<>()).settlements(new ArrayList<>()).auditLogs(new ArrayList<>())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        alice = Participant.builder().id(1L).name("Alice").trip(trip).createdAt(LocalDateTime.now()).build();
        bob = Participant.builder().id(2L).name("Bob").trip(trip).createdAt(LocalDateTime.now()).build();
        charlie = Participant.builder().id(3L).name("Charlie").trip(trip).createdAt(LocalDateTime.now()).build();
        trip.getParticipants().addAll(List.of(alice, bob, charlie));
    }

    @Test
    void calculateBalances_noExpenses_allZero() {
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(expenseRepository.findByTripId(1L)).thenReturn(List.of());

        List<BalanceResponse> balances = balanceService.calculateBalances(1L);

        assertEquals(3, balances.size());
        for (BalanceResponse b : balances) {
            assertEquals(0, b.getNetBalance().compareTo(BigDecimal.ZERO));
            assertEquals("SETTLED", b.getStatus());
        }
    }

    @Test
    void calculateBalances_singleExpense_correctBalances() {
        // Alice pays 2400 for dinner, split equally among 3
        Expense expense = Expense.builder().id(1L).trip(trip).description("Dinner")
                .amount(BigDecimal.valueOf(2400)).payer(alice).expenseParticipants(new ArrayList<>())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        expense.getExpenseParticipants().addAll(List.of(
                ExpenseParticipant.builder().expense(expense).participant(alice).owedAmount(BigDecimal.valueOf(800)).build(),
                ExpenseParticipant.builder().expense(expense).participant(bob).owedAmount(BigDecimal.valueOf(800)).build(),
                ExpenseParticipant.builder().expense(expense).participant(charlie).owedAmount(BigDecimal.valueOf(800)).build()
        ));

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(expenseRepository.findByTripId(1L)).thenReturn(List.of(expense));

        List<BalanceResponse> balances = balanceService.calculateBalances(1L);

        assertEquals(3, balances.size());

        // Alice: paid 2400, owed 800, net = +1600 (RECEIVE)
        BalanceResponse aliceBalance = balances.stream().filter(b -> b.getParticipantId().equals(1L)).findFirst().orElseThrow();
        assertEquals(0, aliceBalance.getTotalPaid().compareTo(BigDecimal.valueOf(2400)));
        assertEquals(0, aliceBalance.getTotalOwed().compareTo(BigDecimal.valueOf(800)));
        assertEquals(0, aliceBalance.getNetBalance().compareTo(BigDecimal.valueOf(1600)));
        assertEquals("RECEIVE", aliceBalance.getStatus());

        // Bob: paid 0, owed 800, net = -800 (PAY)
        BalanceResponse bobBalance = balances.stream().filter(b -> b.getParticipantId().equals(2L)).findFirst().orElseThrow();
        assertEquals(0, bobBalance.getNetBalance().compareTo(BigDecimal.valueOf(-800)));
        assertEquals("PAY", bobBalance.getStatus());

        // Sum of net balances must be 0
        BigDecimal sum = balances.stream().map(BalanceResponse::getNetBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, sum.compareTo(BigDecimal.ZERO));
    }

    @Test
    void calculateBalances_tripNotFound_shouldThrow() {
        when(tripRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> balanceService.calculateBalances(99L));
    }

    @Test
    void calculateBalances_sumAlwaysZero() {
        // Alice pays 1000, Bob pays 500, split equally among 3
        Expense e1 = Expense.builder().id(1L).trip(trip).description("E1")
                .amount(BigDecimal.valueOf(1000)).payer(alice).expenseParticipants(new ArrayList<>())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        BigDecimal share1 = BigDecimal.valueOf(1000).divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
        BigDecimal remainder1 = BigDecimal.valueOf(1000).subtract(share1.multiply(BigDecimal.valueOf(3)));
        e1.getExpenseParticipants().addAll(List.of(
                ExpenseParticipant.builder().expense(e1).participant(alice).owedAmount(share1.add(remainder1)).build(),
                ExpenseParticipant.builder().expense(e1).participant(bob).owedAmount(share1).build(),
                ExpenseParticipant.builder().expense(e1).participant(charlie).owedAmount(share1).build()
        ));

        Expense e2 = Expense.builder().id(2L).trip(trip).description("E2")
                .amount(BigDecimal.valueOf(500)).payer(bob).expenseParticipants(new ArrayList<>())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        BigDecimal share2 = BigDecimal.valueOf(500).divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
        BigDecimal remainder2 = BigDecimal.valueOf(500).subtract(share2.multiply(BigDecimal.valueOf(3)));
        e2.getExpenseParticipants().addAll(List.of(
                ExpenseParticipant.builder().expense(e2).participant(alice).owedAmount(share2.add(remainder2)).build(),
                ExpenseParticipant.builder().expense(e2).participant(bob).owedAmount(share2).build(),
                ExpenseParticipant.builder().expense(e2).participant(charlie).owedAmount(share2).build()
        ));

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(expenseRepository.findByTripId(1L)).thenReturn(List.of(e1, e2));

        List<BalanceResponse> balances = balanceService.calculateBalances(1L);
        BigDecimal sum = balances.stream().map(BalanceResponse::getNetBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, sum.compareTo(BigDecimal.ZERO), "Sum of net balances must be zero");
    }
}
