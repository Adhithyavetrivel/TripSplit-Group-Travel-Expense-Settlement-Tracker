package com.tripsplit.repository;

import com.tripsplit.entity.ExpenseParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExpenseParticipantRepository extends JpaRepository<ExpenseParticipant, Long> {
    List<ExpenseParticipant> findByExpenseId(Long expenseId);
    void deleteByExpenseId(Long expenseId);
}
