package com.tripsplit.service;

import com.tripsplit.dto.BalanceResponse;
import com.tripsplit.entity.Expense;
import com.tripsplit.entity.ExpenseParticipant;
import com.tripsplit.entity.Participant;
import com.tripsplit.entity.Trip;
import com.tripsplit.exception.ResourceNotFoundException;
import com.tripsplit.exception.SettlementException;
import com.tripsplit.repository.ExpenseRepository;
import com.tripsplit.repository.TripRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BalanceService {

    private final TripRepository tripRepository;
    private final ExpenseRepository expenseRepository;

    public BalanceService(TripRepository tripRepository, ExpenseRepository expenseRepository) {
        this.tripRepository = tripRepository;
        this.expenseRepository = expenseRepository;
    }

    @Cacheable(value = "balances", key = "#tripId")
    @Transactional(readOnly = true)
    public List<BalanceResponse> calculateBalances(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        List<Participant> participants = trip.getParticipants();
        List<Expense> expenses = expenseRepository.findByTripId(tripId);

        // totalPaid: sum of expenses where participant is the payer
        Map<Long, BigDecimal> totalPaid = new HashMap<>();
        // totalOwed: sum of owed amounts across all expenses
        Map<Long, BigDecimal> totalOwed = new HashMap<>();

        for (Participant p : participants) {
            totalPaid.put(p.getId(), BigDecimal.ZERO);
            totalOwed.put(p.getId(), BigDecimal.ZERO);
        }

        for (Expense expense : expenses) {
            Long payerId = expense.getPayer().getId();
            totalPaid.merge(payerId, expense.getAmount(), BigDecimal::add);

            for (ExpenseParticipant ep : expense.getExpenseParticipants()) {
                Long participantId = ep.getParticipant().getId();
                totalOwed.merge(participantId, ep.getOwedAmount(), BigDecimal::add);
            }
        }

        List<BalanceResponse> balances = new ArrayList<>();
        BigDecimal sumNet = BigDecimal.ZERO;

        for (Participant p : participants) {
            BigDecimal paid = totalPaid.getOrDefault(p.getId(), BigDecimal.ZERO);
            BigDecimal owed = totalOwed.getOrDefault(p.getId(), BigDecimal.ZERO);
            BigDecimal net = paid.subtract(owed).setScale(2, RoundingMode.HALF_UP);
            sumNet = sumNet.add(net);

            String status;
            if (net.compareTo(BigDecimal.ZERO) > 0) {
                status = "RECEIVE";
            } else if (net.compareTo(BigDecimal.ZERO) < 0) {
                status = "PAY";
            } else {
                status = "SETTLED";
            }

            balances.add(BalanceResponse.builder()
                    .participantId(p.getId())
                    .participantName(p.getName())
                    .totalPaid(paid.setScale(2, RoundingMode.HALF_UP))
                    .totalOwed(owed.setScale(2, RoundingMode.HALF_UP))
                    .netBalance(net)
                    .status(status)
                    .build());
        }

        // Verify invariant: sum of net balances must be zero
        if (sumNet.compareTo(BigDecimal.ZERO) != 0) {
            throw new SettlementException(
                    "Balance invariant violated: sum of net balances is " + sumNet + ", expected 0");
        }

        return balances;
    }
}
