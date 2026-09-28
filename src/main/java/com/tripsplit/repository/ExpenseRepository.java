package com.tripsplit.repository;

import com.tripsplit.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    Page<Expense> findByTripId(Long tripId, Pageable pageable);
    List<Expense> findByTripId(Long tripId);
    Optional<Expense> findByIdAndTripId(Long id, Long tripId);
    int countByTripId(Long tripId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.trip.id = :tripId")
    BigDecimal sumAmountByTripId(@Param("tripId") Long tripId);
}
