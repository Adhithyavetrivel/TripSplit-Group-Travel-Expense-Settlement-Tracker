package com.tripsplit.controller;

import com.tripsplit.dto.AuditLogResponse;
import com.tripsplit.dto.HistoryResponse;
import com.tripsplit.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips/{tripId}")
@Tag(name = "Audit & History", description = "Audit log and history APIs")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Get audit logs for a trip")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogs(@PathVariable Long tripId) {
        List<AuditLogResponse> logs = auditService.getAuditLogs(tripId);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/history")
    @Operation(summary = "Get unified history for a trip")
    public ResponseEntity<List<HistoryResponse>> getHistory(@PathVariable Long tripId) {
        List<HistoryResponse> history = auditService.getHistory(tripId);
        return ResponseEntity.ok(history);
    }
}
