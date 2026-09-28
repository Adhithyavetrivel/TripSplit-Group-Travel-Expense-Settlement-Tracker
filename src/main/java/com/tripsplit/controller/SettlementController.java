package com.tripsplit.controller;

import com.tripsplit.dto.SettlementResultResponse;
import com.tripsplit.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trips/{tripId}")
@Tag(name = "Settlements", description = "Settlement management APIs")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping("/settlement")
    @Operation(summary = "Get current settlement for a trip")
    public ResponseEntity<SettlementResultResponse> getSettlement(@PathVariable Long tripId) {
        SettlementResultResponse response = settlementService.getSettlements(tripId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/settlements/generate")
    @Operation(summary = "Generate settlements for a trip")
    public ResponseEntity<SettlementResultResponse> generateSettlement(
            @PathVariable Long tripId,
            @RequestHeader(value = "X-User", required = false, defaultValue = "SYSTEM") String user) {
        SettlementResultResponse response = settlementService.generateSettlements(tripId, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
