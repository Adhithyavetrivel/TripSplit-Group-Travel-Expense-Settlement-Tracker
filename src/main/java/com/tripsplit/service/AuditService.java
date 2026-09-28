package com.tripsplit.service;

import com.tripsplit.dto.AuditLogResponse;
import com.tripsplit.dto.HistoryResponse;
import com.tripsplit.entity.AuditLog;
import com.tripsplit.entity.Trip;
import com.tripsplit.mapper.TripMapper;
import com.tripsplit.repository.AuditLogRepository;
import com.tripsplit.repository.TripRepository;
import com.tripsplit.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final TripRepository tripRepository;
    private final TripMapper tripMapper;

    public AuditService(AuditLogRepository auditLogRepository, TripRepository tripRepository, TripMapper tripMapper) {
        this.auditLogRepository = auditLogRepository;
        this.tripRepository = tripRepository;
        this.tripMapper = tripMapper;
    }

    @Transactional
    public void log(Trip trip, String action, String entityType, Long entityId, String details, String changedBy) {
        AuditLog auditLog = AuditLog.builder()
                .trip(trip)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .changedBy(changedBy != null ? changedBy : "SYSTEM")
                .build();
        auditLogRepository.save(auditLog);
    }

    public List<AuditLogResponse> getAuditLogs(Long tripId) {
        tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));
        return auditLogRepository.findByTripIdOrderByChangedAtDesc(tripId).stream()
                .map(tripMapper::toAuditLogResponse)
                .collect(Collectors.toList());
    }

    public List<HistoryResponse> getHistory(Long tripId) {
        tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));
        List<AuditLog> logs = auditLogRepository.findByTripIdOrderByChangedAtDesc(tripId);
        return logs.stream()
                .map(log -> HistoryResponse.builder()
                        .eventType(log.getAction())
                        .description(log.getDetails())
                        .performedBy(log.getChangedBy())
                        .timestamp(log.getChangedAt())
                        .entityId(log.getEntityId())
                        .entityType(log.getEntityType())
                        .build())
                .collect(Collectors.toList());
    }
}
