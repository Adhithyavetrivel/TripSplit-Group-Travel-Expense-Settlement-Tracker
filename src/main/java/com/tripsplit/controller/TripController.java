package com.tripsplit.controller;

import com.tripsplit.dto.*;
import com.tripsplit.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trips")
@Tag(name = "Trips", description = "Trip management APIs")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping
    @Operation(summary = "Create a new trip")
    public ResponseEntity<TripResponse> createTrip(
            @Valid @RequestBody TripRequest request,
            @RequestHeader(value = "X-User", required = false, defaultValue = "SYSTEM") String user) {
        TripResponse response = tripService.createTrip(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{tripId}")
    @Operation(summary = "Get trip details")
    public ResponseEntity<TripResponse> getTrip(@PathVariable Long tripId) {
        TripResponse response = tripService.getTrip(tripId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{tripId}/participants")
    @Operation(summary = "Add a participant to a trip")
    public ResponseEntity<ParticipantResponse> addParticipant(
            @PathVariable Long tripId,
            @Valid @RequestBody ParticipantRequest request,
            @RequestHeader(value = "X-User", required = false, defaultValue = "SYSTEM") String user) {
        ParticipantResponse response = tripService.addParticipant(tripId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
