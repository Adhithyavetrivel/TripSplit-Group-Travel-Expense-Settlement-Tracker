package com.tripsplit.controller;

import com.tripsplit.dto.BalanceResponse;
import com.tripsplit.service.BalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips/{tripId}/balances")
@Tag(name = "Balances", description = "Balance calculation APIs")
public class BalanceController {

    private final BalanceService balanceService;

    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping
    @Operation(summary = "Get current balances for a trip")
    public ResponseEntity<List<BalanceResponse>> getBalances(@PathVariable Long tripId) {
        List<BalanceResponse> balances = balanceService.calculateBalances(tripId);
        return ResponseEntity.ok(balances);
    }
}
