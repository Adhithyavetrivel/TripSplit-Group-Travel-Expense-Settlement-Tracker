package com.tripsplit.controller;

import com.tripsplit.dto.*;
import com.tripsplit.entity.Participant;
import com.tripsplit.entity.Trip;
import com.tripsplit.repository.TripRepository;
import com.tripsplit.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TripWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TripService tripService;

    @MockBean
    private ExpenseService expenseService;

    @MockBean
    private BalanceService balanceService;

    @MockBean
    private SettlementService settlementService;

    @MockBean
    private AuditService auditService;

    @MockBean
    private TripRepository tripRepository;

    private Trip testTripEntity;
    private TripResponse testTripResponse;

    @BeforeEach
    void setUp() {
        testTripEntity = Trip.builder()
                .id(1L)
                .name("Goa Trip")
                .destination("Goa")
                .startDate(LocalDate.of(2026, 10, 10))
                .endDate(LocalDate.of(2026, 10, 13))
                .build();

        ParticipantResponse p1 = ParticipantResponse.builder().id(1L).name("Alice").email("alice@test.com").build();
        ParticipantResponse p2 = ParticipantResponse.builder().id(2L).name("Bob").email("bob@test.com").build();

        BalanceResponse b1 = BalanceResponse.builder()
                .participantId(1L)
                .participantName("Alice")
                .totalPaid(BigDecimal.valueOf(2000))
                .totalOwed(BigDecimal.valueOf(1000))
                .netBalance(BigDecimal.valueOf(1000))
                .status("RECEIVE")
                .build();

        BalanceResponse b2 = BalanceResponse.builder()
                .participantId(2L)
                .participantName("Bob")
                .totalPaid(BigDecimal.ZERO)
                .totalOwed(BigDecimal.valueOf(1000))
                .netBalance(BigDecimal.valueOf(-1000))
                .status("PAY")
                .build();

        testTripResponse = TripResponse.builder()
                .id(1L)
                .name("Goa Trip")
                .destination("Goa")
                .startDate(LocalDate.of(2026, 10, 10))
                .endDate(LocalDate.of(2026, 10, 13))
                .participants(List.of(p1, p2))
                .expenseCount(1)
                .totalExpenses(BigDecimal.valueOf(2000))
                .balances(List.of(b1, b2))
                .build();

        when(tripRepository.findAll(any(Sort.class))).thenReturn(List.of(testTripEntity));
        when(tripService.getTrip(1L)).thenReturn(testTripResponse);
    }

    @Test
    void testRootRedirect() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?tripId=1"));
    }

    @Test
    void testDashboard() throws Exception {
        SettlementResultResponse settlementResponse = SettlementResultResponse.builder()
                .tripId(1L)
                .settlements(List.of(SettlementResponse.builder()
                        .fromParticipantId(2L)
                        .fromParticipantName("Bob")
                        .toParticipantId(1L)
                        .toParticipantName("Alice")
                        .amount(BigDecimal.valueOf(1000))
                        .build()))
                .verified(true)
                .build();

        when(settlementService.getSettlements(1L)).thenReturn(settlementResponse);

        mockMvc.perform(get("/dashboard").param("tripId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("trip", "settlement", "allTrips", "activePage"));
    }

    @Test
    void testCreateTripPage() throws Exception {
        mockMvc.perform(get("/trips/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("create-trip"))
                .andExpect(model().attributeExists("allTrips", "activePage"));
    }

    @Test
    void testExpensesPage() throws Exception {
        PageResponse<ExpenseResponse> pageResponse = PageResponse.<ExpenseResponse>builder()
                .content(List.of(ExpenseResponse.builder()
                        .id(10L)
                        .description("Dinner")
                        .amount(BigDecimal.valueOf(2000))
                        .payerId(1L)
                        .payerName("Alice")
                        .expenseDate(LocalDate.of(2026, 10, 10))
                        .participants(List.of(
                                ExpenseParticipantResponse.builder().participantId(1L).participantName("Alice").owedAmount(BigDecimal.valueOf(1000)).build(),
                                ExpenseParticipantResponse.builder().participantId(2L).participantName("Bob").owedAmount(BigDecimal.valueOf(1000)).build()
                        ))
                        .build()))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(expenseService.getExpenses(eq(1L), anyInt(), anyInt(), anyString(), anyString())).thenReturn(pageResponse);

        mockMvc.perform(get("/trips/1/expenses"))
                .andExpect(status().isOk())
                .andExpect(view().name("expenses"))
                .andExpect(model().attributeExists("trip", "expensePage", "currentPage", "sort", "direction", "allTrips"));
    }

    @Test
    void testBalancesPage() throws Exception {
        BalanceResponse b1 = BalanceResponse.builder()
                .participantId(1L).participantName("Alice")
                .totalPaid(BigDecimal.valueOf(2000)).totalOwed(BigDecimal.valueOf(1000))
                .netBalance(BigDecimal.valueOf(1000)).status("RECEIVE").build();
        BalanceResponse b2 = BalanceResponse.builder()
                .participantId(2L).participantName("Bob")
                .totalPaid(BigDecimal.ZERO).totalOwed(BigDecimal.valueOf(1000))
                .netBalance(BigDecimal.valueOf(-1000)).status("PAY").build();

        when(balanceService.calculateBalances(1L)).thenReturn(List.of(b1, b2));

        mockMvc.perform(get("/trips/1/balances"))
                .andExpect(status().isOk())
                .andExpect(view().name("balances"))
                .andExpect(model().attributeExists("trip", "balances", "totalNetBalance", "allTrips"));
    }

    @Test
    void testSettlementPage() throws Exception {
        SettlementResultResponse settlementResponse = SettlementResultResponse.builder()
                .tripId(1L)
                .settlements(List.of())
                .verified(true)
                .build();

        when(settlementService.getSettlements(1L)).thenReturn(settlementResponse);

        mockMvc.perform(get("/trips/1/settlement"))
                .andExpect(status().isOk())
                .andExpect(view().name("settlement"))
                .andExpect(model().attributeExists("trip", "settlement", "allTrips"));
    }

    @Test
    void testActivityPage() throws Exception {
        HistoryResponse h1 = HistoryResponse.builder()
                .eventType("TRIP_CREATED")
                .description("Trip created")
                .performedBy("Adhithya")
                .timestamp(LocalDateTime.now())
                .entityType("Trip")
                .entityId(1L)
                .build();

        AuditLogResponse a1 = AuditLogResponse.builder()
                .id(1L)
                .action("TRIP_CREATED")
                .entityType("Trip")
                .entityId(1L)
                .details("Trip created")
                .changedBy("Adhithya")
                .changedAt(LocalDateTime.now())
                .build();

        when(auditService.getHistory(1L)).thenReturn(List.of(h1));
        when(auditService.getAuditLogs(1L)).thenReturn(List.of(a1));

        mockMvc.perform(get("/trips/1/activity"))
                .andExpect(status().isOk())
                .andExpect(view().name("activity"))
                .andExpect(model().attributeExists("trip", "history", "auditLogs", "allTrips"));
    }
}
