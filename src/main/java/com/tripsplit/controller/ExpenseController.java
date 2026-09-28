package com.tripsplit.controller;

import com.tripsplit.dto.*;
import com.tripsplit.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trips/{tripId}/expenses")
@Tag(name = "Expenses", description = "Expense management APIs")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    @Operation(summary = "Create a new expense")
    public ResponseEntity<ExpenseResponse> createExpense(
            @PathVariable Long tripId,
            @Valid @RequestBody ExpenseRequest request,
            @RequestHeader(value = "X-User", required = false, defaultValue = "SYSTEM") String user) {
        ExpenseResponse response = expenseService.createExpense(tripId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get expenses for a trip with pagination")
    public ResponseEntity<PageResponse<ExpenseResponse>> getExpenses(
            @PathVariable Long tripId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "expenseDate") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        PageResponse<ExpenseResponse> response = expenseService.getExpenses(tripId, page, size, sort, direction);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{expenseId}")
    @Operation(summary = "Update an expense")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @PathVariable Long tripId,
            @PathVariable Long expenseId,
            @Valid @RequestBody ExpenseRequest request,
            @RequestHeader(value = "X-User", required = false, defaultValue = "SYSTEM") String user) {
        ExpenseResponse response = expenseService.updateExpense(tripId, expenseId, request, user);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{expenseId}")
    @Operation(summary = "Delete an expense")
    public ResponseEntity<Void> deleteExpense(
            @PathVariable Long tripId,
            @PathVariable Long expenseId,
            @RequestHeader(value = "X-User", required = false, defaultValue = "SYSTEM") String user) {
        expenseService.deleteExpense(tripId, expenseId, user);
        return ResponseEntity.noContent().build();
    }
}
