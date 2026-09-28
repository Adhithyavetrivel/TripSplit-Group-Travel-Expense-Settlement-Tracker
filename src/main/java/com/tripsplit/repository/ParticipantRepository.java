package com.tripsplit.repository;

import com.tripsplit.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    List<Participant> findByTripId(Long tripId);
    Optional<Participant> findByTripIdAndEmail(Long tripId, String email);
    boolean existsByTripIdAndEmail(Long tripId, String email);
}
