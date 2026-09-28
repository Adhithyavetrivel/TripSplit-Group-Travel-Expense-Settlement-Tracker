package com.tripsplit.service;

import com.tripsplit.dto.*;
import com.tripsplit.entity.Participant;
import com.tripsplit.entity.Trip;
import com.tripsplit.exception.BadRequestException;
import com.tripsplit.exception.ResourceNotFoundException;
import com.tripsplit.mapper.TripMapper;
import com.tripsplit.repository.ExpenseRepository;
import com.tripsplit.repository.ParticipantRepository;
import com.tripsplit.repository.TripRepository;
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
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;
    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private BalanceService balanceService;
    @Mock
    private AuditService auditService;
    @Spy
    private TripMapper tripMapper = new TripMapper();

    @InjectMocks
    private TripService tripService;

    private TripRequest tripRequest;
    private Trip trip;

    @BeforeEach
    void setUp() {
        tripRequest = TripRequest.builder()
                .name("Goa Trip")
                .description("College friends trip")
                .destination("Goa")
                .startDate(LocalDate.of(2026, 10, 10))
                .endDate(LocalDate.of(2026, 10, 13))
                .participants(List.of(
                        ParticipantRequest.builder().name("Alice").email("alice@example.com").build(),
                        ParticipantRequest.builder().name("Bob").email("bob@example.com").build()
                ))
                .build();

        trip = Trip.builder()
                .id(1L)
                .name("Goa Trip")
                .description("College friends trip")
                .destination("Goa")
                .startDate(LocalDate.of(2026, 10, 10))
                .endDate(LocalDate.of(2026, 10, 13))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .participants(new ArrayList<>())
                .expenses(new ArrayList<>())
                .settlements(new ArrayList<>())
                .auditLogs(new ArrayList<>())
                .build();
    }

    @Test
    void createTrip_shouldCreateSuccessfully() {
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> {
            Trip t = inv.getArgument(0);
            t.setId(1L);
            t.setCreatedAt(LocalDateTime.now());
            t.setUpdatedAt(LocalDateTime.now());
            return t;
        });

        TripResponse response = tripService.createTrip(tripRequest, "TestUser");

        assertNotNull(response);
        assertEquals("Goa Trip", response.getName());
        verify(tripRepository, atLeastOnce()).save(any(Trip.class));
        verify(auditService).log(any(), eq("TRIP_CREATED"), eq("Trip"), any(), anyString(), eq("TestUser"));
    }

    @Test
    void createTrip_invalidDates_shouldThrow() {
        tripRequest.setStartDate(LocalDate.of(2026, 10, 15));
        tripRequest.setEndDate(LocalDate.of(2026, 10, 10));

        assertThrows(BadRequestException.class, () -> tripService.createTrip(tripRequest, "TestUser"));
    }

    @Test
    void getTrip_shouldReturnTrip() {
        trip.getParticipants().add(Participant.builder().id(1L).name("Alice").email("alice@example.com").trip(trip).createdAt(LocalDateTime.now()).build());
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(expenseRepository.countByTripId(1L)).thenReturn(3);
        when(expenseRepository.sumAmountByTripId(1L)).thenReturn(BigDecimal.valueOf(5000));
        when(balanceService.calculateBalances(1L)).thenReturn(List.of());

        TripResponse response = tripService.getTrip(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(3, response.getExpenseCount());
        assertEquals(BigDecimal.valueOf(5000), response.getTotalExpenses());
    }

    @Test
    void getTrip_notFound_shouldThrow() {
        when(tripRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> tripService.getTrip(99L));
    }

    @Test
    void addParticipant_shouldAddSuccessfully() {
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(participantRepository.existsByTripIdAndEmail(1L, "dave@example.com")).thenReturn(false);
        when(participantRepository.save(any(Participant.class))).thenAnswer(inv -> {
            Participant p = inv.getArgument(0);
            p.setId(5L);
            p.setCreatedAt(LocalDateTime.now());
            return p;
        });

        ParticipantRequest request = ParticipantRequest.builder().name("Dave").email("dave@example.com").build();
        ParticipantResponse response = tripService.addParticipant(1L, request, "TestUser");

        assertNotNull(response);
        assertEquals("Dave", response.getName());
    }

    @Test
    void addParticipant_duplicateEmail_shouldThrow() {
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(participantRepository.existsByTripIdAndEmail(1L, "alice@example.com")).thenReturn(true);

        ParticipantRequest request = ParticipantRequest.builder().name("Alice2").email("alice@example.com").build();
        assertThrows(BadRequestException.class, () -> tripService.addParticipant(1L, request, "TestUser"));
    }
}
