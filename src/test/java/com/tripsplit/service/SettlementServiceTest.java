package com.tripsplit.service;

import com.tripsplit.dto.BalanceResponse;
import com.tripsplit.dto.SettlementResultResponse;
import com.tripsplit.entity.*;
import com.tripsplit.mapper.TripMapper;
import com.tripsplit.repository.ParticipantRepository;
import com.tripsplit.repository.SettlementRepository;
import com.tripsplit.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock private TripRepository tripRepository;
    @Mock private SettlementRepository settlementRepository;
    @Mock private ParticipantRepository participantRepository;
    @Mock private BalanceService balanceService;
    @Mock private AuditService auditService;
    @Spy private TripMapper tripMapper = new TripMapper();

    @InjectMocks
    private SettlementService settlementService;

    private Trip trip;
    private Participant alice, bob, charlie, david;

    @BeforeEach
    void setUp() {
        trip = Trip.builder().id(1L).name("Test").participants(new ArrayList<>())
                .expenses(new ArrayList<>()).settlements(new ArrayList<>()).auditLogs(new ArrayList<>())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        alice = Participant.builder().id(1L).name("Alice").trip(trip).createdAt(LocalDateTime.now()).build();
        bob = Participant.builder().id(2L).name("Bob").trip(trip).createdAt(LocalDateTime.now()).build();
        charlie = Participant.builder().id(3L).name("Charlie").trip(trip).createdAt(LocalDateTime.now()).build();
        david = Participant.builder().id(4L).name("David").trip(trip).createdAt(LocalDateTime.now()).build();
    }

    private void setupCommonMocks() {
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(participantRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(participantRepository.findById(2L)).thenReturn(Optional.of(bob));
        lenient().when(participantRepository.findById(3L)).thenReturn(Optional.of(charlie));
        lenient().when(participantRepository.findById(4L)).thenReturn(Optional.of(david));
        when(settlementRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void generateSettlements_simpleTwoPerson() {
        setupCommonMocks();
        when(balanceService.calculateBalances(1L)).thenReturn(List.of(
                BalanceResponse.builder().participantId(1L).participantName("Alice")
                        .totalPaid(BigDecimal.valueOf(1000)).totalOwed(BigDecimal.valueOf(500))
                        .netBalance(BigDecimal.valueOf(500)).status("RECEIVE").build(),
                BalanceResponse.builder().participantId(2L).participantName("Bob")
                        .totalPaid(BigDecimal.ZERO).totalOwed(BigDecimal.valueOf(500))
                        .netBalance(BigDecimal.valueOf(-500)).status("PAY").build()
        ));

        SettlementResultResponse result = settlementService.generateSettlements(1L, "TestUser");

        assertTrue(result.isVerified());
        assertEquals(1, result.getSettlements().size());
        assertEquals(0, result.getSettlements().get(0).getAmount().compareTo(BigDecimal.valueOf(500)));
    }

    @Test
    void generateSettlements_threePersons() {
        setupCommonMocks();
        when(balanceService.calculateBalances(1L)).thenReturn(List.of(
                BalanceResponse.builder().participantId(1L).participantName("Alice")
                        .totalPaid(BigDecimal.valueOf(1800)).totalOwed(BigDecimal.valueOf(1200))
                        .netBalance(BigDecimal.valueOf(600)).status("RECEIVE").build(),
                BalanceResponse.builder().participantId(2L).participantName("Bob")
                        .totalPaid(BigDecimal.valueOf(900)).totalOwed(BigDecimal.valueOf(1200))
                        .netBalance(BigDecimal.valueOf(-300)).status("PAY").build(),
                BalanceResponse.builder().participantId(3L).participantName("Charlie")
                        .totalPaid(BigDecimal.valueOf(900)).totalOwed(BigDecimal.valueOf(1200))
                        .netBalance(BigDecimal.valueOf(-300)).status("PAY").build()
        ));

        SettlementResultResponse result = settlementService.generateSettlements(1L, "TestUser");

        assertTrue(result.isVerified());
        assertEquals(2, result.getSettlements().size());
    }

    @Test
    void generateSettlements_allSettled_noTransactions() {
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(balanceService.calculateBalances(1L)).thenReturn(List.of(
                BalanceResponse.builder().participantId(1L).participantName("Alice")
                        .totalPaid(BigDecimal.valueOf(500)).totalOwed(BigDecimal.valueOf(500))
                        .netBalance(BigDecimal.ZERO).status("SETTLED").build(),
                BalanceResponse.builder().participantId(2L).participantName("Bob")
                        .totalPaid(BigDecimal.valueOf(500)).totalOwed(BigDecimal.valueOf(500))
                        .netBalance(BigDecimal.ZERO).status("SETTLED").build()
        ));
        when(settlementRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        SettlementResultResponse result = settlementService.generateSettlements(1L, "TestUser");

        assertTrue(result.isVerified());
        assertEquals(0, result.getSettlements().size());
    }

    @Test
    void generateSettlements_multipleDebtorsCreditors() {
        setupCommonMocks();
        // Alice +500, Bob +300, Charlie -400, David -400
        when(balanceService.calculateBalances(1L)).thenReturn(List.of(
                BalanceResponse.builder().participantId(1L).participantName("Alice")
                        .totalPaid(BigDecimal.valueOf(1500)).totalOwed(BigDecimal.valueOf(1000))
                        .netBalance(BigDecimal.valueOf(500)).status("RECEIVE").build(),
                BalanceResponse.builder().participantId(2L).participantName("Bob")
                        .totalPaid(BigDecimal.valueOf(1300)).totalOwed(BigDecimal.valueOf(1000))
                        .netBalance(BigDecimal.valueOf(300)).status("RECEIVE").build(),
                BalanceResponse.builder().participantId(3L).participantName("Charlie")
                        .totalPaid(BigDecimal.valueOf(600)).totalOwed(BigDecimal.valueOf(1000))
                        .netBalance(BigDecimal.valueOf(-400)).status("PAY").build(),
                BalanceResponse.builder().participantId(4L).participantName("David")
                        .totalPaid(BigDecimal.valueOf(600)).totalOwed(BigDecimal.valueOf(1000))
                        .netBalance(BigDecimal.valueOf(-400)).status("PAY").build()
        ));

        SettlementResultResponse result = settlementService.generateSettlements(1L, "TestUser");

        assertTrue(result.isVerified());
        // Verify all amounts are positive
        result.getSettlements().forEach(s -> assertTrue(s.getAmount().compareTo(BigDecimal.ZERO) > 0));
    }

    @Test
    void generateSettlements_decimalCase() {
        setupCommonMocks();
        when(balanceService.calculateBalances(1L)).thenReturn(List.of(
                BalanceResponse.builder().participantId(1L).participantName("Alice")
                        .totalPaid(BigDecimal.valueOf(100)).totalOwed(BigDecimal.valueOf(33.33))
                        .netBalance(BigDecimal.valueOf(66.67)).status("RECEIVE").build(),
                BalanceResponse.builder().participantId(2L).participantName("Bob")
                        .totalPaid(BigDecimal.ZERO).totalOwed(BigDecimal.valueOf(33.34))
                        .netBalance(BigDecimal.valueOf(-33.34)).status("PAY").build(),
                BalanceResponse.builder().participantId(3L).participantName("Charlie")
                        .totalPaid(BigDecimal.ZERO).totalOwed(BigDecimal.valueOf(33.33))
                        .netBalance(BigDecimal.valueOf(-33.33)).status("PAY").build()
        ));

        SettlementResultResponse result = settlementService.generateSettlements(1L, "TestUser");
        assertTrue(result.isVerified());
    }
}
