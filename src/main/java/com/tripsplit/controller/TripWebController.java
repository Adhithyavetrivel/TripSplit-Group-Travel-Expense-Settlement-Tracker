package com.tripsplit.controller;

import com.tripsplit.dto.*;
import com.tripsplit.entity.Trip;
import com.tripsplit.repository.TripRepository;
import com.tripsplit.service.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class TripWebController {

    private final TripService tripService;
    private final ExpenseService expenseService;
    private final BalanceService balanceService;
    private final SettlementService settlementService;
    private final AuditService auditService;
    private final TripRepository tripRepository;

    public TripWebController(TripService tripService,
                             ExpenseService expenseService,
                             BalanceService balanceService,
                             SettlementService settlementService,
                             AuditService auditService,
                             TripRepository tripRepository) {
        this.tripService = tripService;
        this.expenseService = expenseService;
        this.balanceService = balanceService;
        this.settlementService = settlementService;
        this.auditService = auditService;
        this.tripRepository = tripRepository;
    }

    @GetMapping("/")
    public String index() {
        List<Trip> trips = tripRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        if (!trips.isEmpty()) {
            return "redirect:/dashboard?tripId=" + trips.get(0).getId();
        }
        return "redirect:/trips/create";
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) Long tripId, Model model) {
        List<Trip> allTrips = tripRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        if (allTrips.isEmpty()) {
            return "redirect:/trips/create";
        }

        if (tripId == null || allTrips.stream().noneMatch(t -> t.getId().equals(tripId))) {
            return "redirect:/dashboard?tripId=" + allTrips.get(0).getId();
        }

        TripResponse trip = tripService.getTrip(tripId);
        SettlementResultResponse settlement = settlementService.getSettlements(tripId);

        model.addAttribute("trip", trip);
        model.addAttribute("settlement", settlement);
        model.addAttribute("allTrips", allTrips);
        model.addAttribute("activePage", "dashboard");
        return "dashboard";
    }

    @GetMapping("/trips/create")
    public String createTrip(Model model) {
        List<Trip> allTrips = tripRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        model.addAttribute("allTrips", allTrips);
        model.addAttribute("activePage", "create-trip");
        return "create-trip";
    }

    @GetMapping("/trips/{tripId}/expenses")
    public String expenses(
            @PathVariable Long tripId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "expenseDate") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            Model model) {
        TripResponse trip = tripService.getTrip(tripId);
        PageResponse<ExpenseResponse> expensePage = expenseService.getExpenses(tripId, page, size, sort, direction);
        List<Trip> allTrips = tripRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));

        model.addAttribute("trip", trip);
        model.addAttribute("expensePage", expensePage);
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);
        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);
        model.addAttribute("allTrips", allTrips);
        model.addAttribute("activePage", "expenses");
        return "expenses";
    }

    @GetMapping("/trips/{tripId}/balances")
    public String balances(@PathVariable Long tripId, Model model) {
        TripResponse trip = tripService.getTrip(tripId);
        List<BalanceResponse> balances = balanceService.calculateBalances(tripId);
        List<Trip> allTrips = tripRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));

        BigDecimal totalNetBalance = balances.stream()
                .map(BalanceResponse::getNetBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("trip", trip);
        model.addAttribute("balances", balances);
        model.addAttribute("totalNetBalance", totalNetBalance);
        model.addAttribute("allTrips", allTrips);
        model.addAttribute("activePage", "balances");
        return "balances";
    }

    @GetMapping("/trips/{tripId}/settlement")
    public String settlement(@PathVariable Long tripId, Model model) {
        TripResponse trip = tripService.getTrip(tripId);
        SettlementResultResponse settlement = settlementService.getSettlements(tripId);
        List<Trip> allTrips = tripRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));

        model.addAttribute("trip", trip);
        model.addAttribute("settlement", settlement);
        model.addAttribute("allTrips", allTrips);
        model.addAttribute("activePage", "settlement");
        return "settlement";
    }

    @GetMapping("/trips/{tripId}/activity")
    public String activity(@PathVariable Long tripId, Model model) {
        TripResponse trip = tripService.getTrip(tripId);
        List<HistoryResponse> history = auditService.getHistory(tripId);
        List<AuditLogResponse> auditLogs = auditService.getAuditLogs(tripId);
        List<Trip> allTrips = tripRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));

        model.addAttribute("trip", trip);
        model.addAttribute("history", history);
        model.addAttribute("auditLogs", auditLogs);
        model.addAttribute("allTrips", allTrips);
        model.addAttribute("activePage", "activity");
        return "activity";
    }
}
