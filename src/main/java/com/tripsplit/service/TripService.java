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

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final ParticipantRepository participantRepository;
    private final ExpenseRepository expenseRepository;
    private final BalanceService balanceService;
    private final AuditService auditService;
    private final TripMapper tripMapper;

    public TripService(TripRepository tripRepository, ParticipantRepository participantRepository,
                       ExpenseRepository expenseRepository, BalanceService balanceService,
                       AuditService auditService, TripMapper tripMapper) {
        this.tripRepository = tripRepository;
        this.participantRepository = participantRepository;
        this.expenseRepository = expenseRepository;
        this.balanceService = balanceService;
        this.auditService = auditService;
        this.tripMapper = tripMapper;
    }

    @Transactional
    public TripResponse createTrip(TripRequest request, String changedBy) {
        // Validate dates
        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date must be on or after start date");
        }

        Trip trip = tripMapper.toEntity(request);
        trip = tripRepository.save(trip);

        // Add participants
        if (request.getParticipants() != null) {
            for (ParticipantRequest pr : request.getParticipants()) {
                Participant participant = Participant.builder()
                        .name(pr.getName())
                        .email(pr.getEmail())
                        .trip(trip)
                        .build();
                trip.getParticipants().add(participant);
            }
            trip = tripRepository.save(trip);
        }

        auditService.log(trip, "TRIP_CREATED", "Trip", trip.getId(),
                "Trip '" + trip.getName() + "' created with " + trip.getParticipants().size() + " participants",
                changedBy);

        return tripMapper.toResponse(trip, 0, BigDecimal.ZERO, List.of());
    }

    @Transactional(readOnly = true)
    public TripResponse getTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        int expenseCount = expenseRepository.countByTripId(tripId);
        BigDecimal totalExpenses = expenseRepository.sumAmountByTripId(tripId);
        List<BalanceResponse> balances = balanceService.calculateBalances(tripId);

        return tripMapper.toResponse(trip, expenseCount, totalExpenses, balances);
    }

    @CacheEvict(value = "balances", key = "#tripId")
    @Transactional
    public ParticipantResponse addParticipant(Long tripId, ParticipantRequest request, String changedBy) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        // Check for duplicate email within the same trip
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (participantRepository.existsByTripIdAndEmail(tripId, request.getEmail())) {
                throw new BadRequestException("Participant with email '" + request.getEmail() + "' already exists in this trip");
            }
        }

        Participant participant = Participant.builder()
                .name(request.getName())
                .email(request.getEmail())
                .trip(trip)
                .build();
        participant = participantRepository.save(participant);

        auditService.log(trip, "PARTICIPANT_ADDED", "Participant", participant.getId(),
                "Participant '" + participant.getName() + "' added to trip", changedBy);

        return tripMapper.toParticipantResponse(participant);
    }
}
