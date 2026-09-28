package com.tripsplit.service;

import com.tripsplit.dto.*;
import com.tripsplit.entity.*;
import com.tripsplit.exception.BadRequestException;
import com.tripsplit.exception.ResourceNotFoundException;
import com.tripsplit.mapper.TripMapper;
import com.tripsplit.repository.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final TripRepository tripRepository;
    private final ParticipantRepository participantRepository;
    private final ExpenseParticipantRepository expenseParticipantRepository;
    private final SettlementRepository settlementRepository;
    private final AuditService auditService;
    private final TripMapper tripMapper;

    public ExpenseService(ExpenseRepository expenseRepository, TripRepository tripRepository,
                          ParticipantRepository participantRepository,
                          ExpenseParticipantRepository expenseParticipantRepository,
                          SettlementRepository settlementRepository,
                          AuditService auditService, TripMapper tripMapper) {
        this.expenseRepository = expenseRepository;
        this.tripRepository = tripRepository;
        this.participantRepository = participantRepository;
        this.expenseParticipantRepository = expenseParticipantRepository;
        this.settlementRepository = settlementRepository;
        this.auditService = auditService;
        this.tripMapper = tripMapper;
    }

    @CacheEvict(value = "balances", key = "#tripId")
    @Transactional
    public ExpenseResponse createExpense(Long tripId, ExpenseRequest request, String changedBy) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        // Validate payer belongs to trip
        Participant payer = participantRepository.findById(request.getPayerId())
                .orElseThrow(() -> new ResourceNotFoundException("Payer not found with id: " + request.getPayerId()));
        if (!payer.getTrip().getId().equals(tripId)) {
            throw new BadRequestException("Payer does not belong to this trip");
        }

        // Validate all participants belong to trip and no duplicates
        Set<Long> participantIds = new HashSet<>();
        List<Participant> expenseParticipants = new ArrayList<>();
        for (ExpenseParticipantRequest epr : request.getParticipants()) {
            if (!participantIds.add(epr.getParticipantId())) {
                throw new BadRequestException("Duplicate participant in expense: " + epr.getParticipantId());
            }
            Participant p = participantRepository.findById(epr.getParticipantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Participant not found with id: " + epr.getParticipantId()));
            if (!p.getTrip().getId().equals(tripId)) {
                throw new BadRequestException("Participant " + p.getName() + " does not belong to this trip");
            }
            expenseParticipants.add(p);
        }

        // Calculate shares
        List<BigDecimal> shares = calculateShares(request);

        // Create expense
        Expense expense = Expense.builder()
                .trip(trip)
                .description(request.getDescription())
                .amount(request.getAmount())
                .payer(payer)
                .expenseDate(request.getExpenseDate() != null ? request.getExpenseDate() : LocalDate.now())
                .build();
        expense = expenseRepository.save(expense);

        // Create expense participants
        for (int i = 0; i < expenseParticipants.size(); i++) {
            ExpenseParticipant ep = ExpenseParticipant.builder()
                    .expense(expense)
                    .participant(expenseParticipants.get(i))
                    .owedAmount(shares.get(i))
                    .build();
            expense.getExpenseParticipants().add(ep);
        }
        expense = expenseRepository.save(expense);

        // Invalidate old settlements
        settlementRepository.deleteByTripId(tripId);

        auditService.log(trip, "EXPENSE_CREATED", "Expense", expense.getId(),
                "Expense '" + expense.getDescription() + "' of " + expense.getAmount() + " created", changedBy);

        return tripMapper.toExpenseResponse(expense);
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> getExpenses(Long tripId, int page, int size, String sortBy, String direction) {
        tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        // Limit page size
        if (size > 50) size = 50;
        if (size < 1) size = 10;

        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Expense> expensePage = expenseRepository.findByTripId(tripId, pageable);

        List<ExpenseResponse> content = expensePage.getContent().stream()
                .map(tripMapper::toExpenseResponse)
                .collect(Collectors.toList());

        return PageResponse.<ExpenseResponse>builder()
                .content(content)
                .page(expensePage.getNumber())
                .size(expensePage.getSize())
                .totalElements(expensePage.getTotalElements())
                .totalPages(expensePage.getTotalPages())
                .build();
    }

    @CacheEvict(value = "balances", key = "#tripId")
    @Transactional
    public ExpenseResponse updateExpense(Long tripId, Long expenseId, ExpenseRequest request, String changedBy) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        Expense expense = expenseRepository.findByIdAndTripId(expenseId, tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + expenseId + " in trip: " + tripId));

        // Validate payer belongs to trip
        Participant payer = participantRepository.findById(request.getPayerId())
                .orElseThrow(() -> new ResourceNotFoundException("Payer not found with id: " + request.getPayerId()));
        if (!payer.getTrip().getId().equals(tripId)) {
            throw new BadRequestException("Payer does not belong to this trip");
        }

        // Validate all participants belong to trip and no duplicates
        Set<Long> participantIds = new HashSet<>();
        List<Participant> newParticipants = new ArrayList<>();
        for (ExpenseParticipantRequest epr : request.getParticipants()) {
            if (!participantIds.add(epr.getParticipantId())) {
                throw new BadRequestException("Duplicate participant in expense: " + epr.getParticipantId());
            }
            Participant p = participantRepository.findById(epr.getParticipantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Participant not found with id: " + epr.getParticipantId()));
            if (!p.getTrip().getId().equals(tripId)) {
                throw new BadRequestException("Participant " + p.getName() + " does not belong to this trip");
            }
            newParticipants.add(p);
        }

        List<BigDecimal> shares = calculateShares(request);

        // Update expense fields
        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount());
        expense.setPayer(payer);
        expense.setExpenseDate(request.getExpenseDate() != null ? request.getExpenseDate() : expense.getExpenseDate());

        // Clear old participants and add new
        expense.getExpenseParticipants().clear();
        expenseRepository.save(expense); // flush delete

        for (int i = 0; i < newParticipants.size(); i++) {
            ExpenseParticipant ep = ExpenseParticipant.builder()
                    .expense(expense)
                    .participant(newParticipants.get(i))
                    .owedAmount(shares.get(i))
                    .build();
            expense.getExpenseParticipants().add(ep);
        }
        expense = expenseRepository.save(expense);

        // Invalidate old settlements
        settlementRepository.deleteByTripId(tripId);

        auditService.log(trip, "EXPENSE_UPDATED", "Expense", expense.getId(),
                "Expense '" + expense.getDescription() + "' updated", changedBy);

        return tripMapper.toExpenseResponse(expense);
    }

    @CacheEvict(value = "balances", key = "#tripId")
    @Transactional
    public void deleteExpense(Long tripId, Long expenseId, String changedBy) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        Expense expense = expenseRepository.findByIdAndTripId(expenseId, tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + expenseId + " in trip: " + tripId));

        String description = expense.getDescription();
        expenseRepository.delete(expense);

        // Invalidate old settlements
        settlementRepository.deleteByTripId(tripId);

        auditService.log(trip, "EXPENSE_DELETED", "Expense", expenseId,
                "Expense '" + description + "' deleted", changedBy);
    }

    private List<BigDecimal> calculateShares(ExpenseRequest request) {
        List<ExpenseParticipantRequest> participants = request.getParticipants();
        boolean isCustomSplit = participants.stream().anyMatch(p -> p.getOwedAmount() != null);

        List<BigDecimal> shares = new ArrayList<>();

        if (isCustomSplit) {
            // All must have owedAmount
            BigDecimal sum = BigDecimal.ZERO;
            for (ExpenseParticipantRequest epr : participants) {
                if (epr.getOwedAmount() == null) {
                    throw new BadRequestException("All participants must have owedAmount for custom split");
                }
                if (epr.getOwedAmount().compareTo(BigDecimal.ZERO) < 0) {
                    throw new BadRequestException("Owed amount cannot be negative");
                }
                shares.add(epr.getOwedAmount().setScale(2, RoundingMode.HALF_UP));
                sum = sum.add(epr.getOwedAmount());
            }
            if (sum.setScale(2, RoundingMode.HALF_UP).compareTo(request.getAmount().setScale(2, RoundingMode.HALF_UP)) != 0) {
                throw new BadRequestException("Expense shares must equal total expense amount. Expected: "
                        + request.getAmount() + ", Got: " + sum);
            }
        } else {
            // Equal split
            int count = participants.size();
            BigDecimal equalShare = request.getAmount().divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
            BigDecimal allocated = equalShare.multiply(BigDecimal.valueOf(count));
            BigDecimal remainder = request.getAmount().subtract(allocated);

            for (int i = 0; i < count; i++) {
                BigDecimal share = equalShare;
                if (i == 0 && remainder.compareTo(BigDecimal.ZERO) != 0) {
                    share = share.add(remainder);
                }
                shares.add(share);
            }
        }
        return shares;
    }
}
