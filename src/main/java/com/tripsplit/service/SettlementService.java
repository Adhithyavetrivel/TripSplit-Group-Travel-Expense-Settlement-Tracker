package com.tripsplit.service;

import com.tripsplit.dto.BalanceResponse;
import com.tripsplit.dto.SettlementResponse;
import com.tripsplit.dto.SettlementResultResponse;
import com.tripsplit.entity.Participant;
import com.tripsplit.entity.Settlement;
import com.tripsplit.entity.Trip;
import com.tripsplit.exception.ResourceNotFoundException;
import com.tripsplit.exception.SettlementException;
import com.tripsplit.mapper.TripMapper;
import com.tripsplit.repository.ParticipantRepository;
import com.tripsplit.repository.SettlementRepository;
import com.tripsplit.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SettlementService {

    private final TripRepository tripRepository;
    private final SettlementRepository settlementRepository;
    private final ParticipantRepository participantRepository;
    private final BalanceService balanceService;
    private final AuditService auditService;
    private final TripMapper tripMapper;

    public SettlementService(TripRepository tripRepository, SettlementRepository settlementRepository,
                              ParticipantRepository participantRepository, BalanceService balanceService,
                              AuditService auditService, TripMapper tripMapper) {
        this.tripRepository = tripRepository;
        this.settlementRepository = settlementRepository;
        this.participantRepository = participantRepository;
        this.balanceService = balanceService;
        this.auditService = auditService;
        this.tripMapper = tripMapper;
    }

    @Transactional(readOnly = true)
    public SettlementResultResponse getSettlements(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        List<Settlement> settlements = settlementRepository.findByTripId(tripId);
        List<SettlementResponse> responses = settlements.stream()
                .map(tripMapper::toSettlementResponse)
                .collect(Collectors.toList());

        List<BalanceResponse> balances = balanceService.calculateBalances(tripId);
        boolean verified = verifySettlements(balances, settlements);

        return SettlementResultResponse.builder()
                .tripId(tripId)
                .settlements(responses)
                .verified(verified)
                .build();
    }

    @Transactional
    public SettlementResultResponse generateSettlements(Long tripId, String changedBy) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        // Delete old settlements
        settlementRepository.deleteByTripId(tripId);

        // Calculate balances
        List<BalanceResponse> balances = balanceService.calculateBalances(tripId);

        // Separate debtors and creditors
        List<BalanceResponse> debtors = new ArrayList<>();
        List<BalanceResponse> creditors = new ArrayList<>();

        for (BalanceResponse b : balances) {
            if (b.getNetBalance().compareTo(BigDecimal.ZERO) < 0) {
                debtors.add(b);
            } else if (b.getNetBalance().compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(b);
            }
        }

        // Sort for deterministic results: largest debts/credits first
        debtors.sort((a, b) -> a.getNetBalance().compareTo(b.getNetBalance()));
        creditors.sort((a, b) -> b.getNetBalance().compareTo(a.getNetBalance()));

        List<Settlement> settlements = new ArrayList<>();
        Map<Long, BigDecimal> debtorAmounts = new HashMap<>();
        Map<Long, BigDecimal> creditorAmounts = new HashMap<>();

        for (BalanceResponse d : debtors) {
            debtorAmounts.put(d.getParticipantId(), d.getNetBalance().abs());
        }
        for (BalanceResponse c : creditors) {
            creditorAmounts.put(c.getParticipantId(), c.getNetBalance());
        }

        int di = 0, ci = 0;
        while (di < debtors.size() && ci < creditors.size()) {
            BalanceResponse debtor = debtors.get(di);
            BalanceResponse creditor = creditors.get(ci);

            BigDecimal debtAmount = debtorAmounts.get(debtor.getParticipantId());
            BigDecimal creditAmount = creditorAmounts.get(creditor.getParticipantId());

            BigDecimal transferAmount = debtAmount.min(creditAmount).setScale(2, RoundingMode.HALF_UP);

            if (transferAmount.compareTo(BigDecimal.ZERO) > 0) {
                Participant fromParticipant = participantRepository.findById(debtor.getParticipantId())
                        .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));
                Participant toParticipant = participantRepository.findById(creditor.getParticipantId())
                        .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));

                Settlement settlement = Settlement.builder()
                        .trip(trip)
                        .fromParticipant(fromParticipant)
                        .toParticipant(toParticipant)
                        .amount(transferAmount)
                        .build();
                settlements.add(settlement);
            }

            BigDecimal remaining = debtAmount.subtract(creditAmount);
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                debtorAmounts.put(debtor.getParticipantId(), remaining);
                creditorAmounts.put(creditor.getParticipantId(), BigDecimal.ZERO);
                ci++;
            } else if (remaining.compareTo(BigDecimal.ZERO) < 0) {
                debtorAmounts.put(debtor.getParticipantId(), BigDecimal.ZERO);
                creditorAmounts.put(creditor.getParticipantId(), remaining.abs());
                di++;
            } else {
                debtorAmounts.put(debtor.getParticipantId(), BigDecimal.ZERO);
                creditorAmounts.put(creditor.getParticipantId(), BigDecimal.ZERO);
                di++;
                ci++;
            }
        }

        // Save settlements
        settlements = settlementRepository.saveAll(settlements);

        // Verify settlements
        boolean verified = verifySettlements(balances, settlements);
        if (!verified) {
            throw new SettlementException("Settlement verification failed: not all balances are cleared");
        }

        // Audit log
        auditService.log(trip, "SETTLEMENT_GENERATED", "Settlement", null,
                "Generated " + settlements.size() + " settlement transactions", changedBy);

        List<SettlementResponse> responses = settlements.stream()
                .map(tripMapper::toSettlementResponse)
                .collect(Collectors.toList());

        return SettlementResultResponse.builder()
                .tripId(tripId)
                .settlements(responses)
                .verified(verified)
                .build();
    }

    private boolean verifySettlements(List<BalanceResponse> balances, List<Settlement> settlements) {
        // Simulate applying settlements
        Map<Long, BigDecimal> finalBalances = new HashMap<>();
        for (BalanceResponse b : balances) {
            finalBalances.put(b.getParticipantId(), b.getNetBalance());
        }

        for (Settlement s : settlements) {
            Long fromId = s.getFromParticipant().getId();
            Long toId = s.getToParticipant().getId();
            BigDecimal amount = s.getAmount();

            // From (debtor) pays, so their balance goes up (less negative)
            finalBalances.merge(fromId, amount, BigDecimal::add);
            // To (creditor) receives, so their balance goes down (less positive)
            finalBalances.merge(toId, amount.negate(), BigDecimal::add);
        }

        // All final balances should be zero
        for (BigDecimal balance : finalBalances.values()) {
            if (balance.setScale(2, RoundingMode.HALF_UP).compareTo(BigDecimal.ZERO) != 0) {
                return false;
            }
        }
        return true;
    }
}
