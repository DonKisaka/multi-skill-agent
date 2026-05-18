package com.donald.multi_skill_agent.repository;

import com.donald.multi_skill_agent.model.Category;
import com.donald.multi_skill_agent.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByCategory(Category category);

    List<Expense> findByDateBetween(LocalDate start, LocalDate end);

    List<Expense> findByAmountGreaterThan(BigDecimal amount);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.category = :category")
    BigDecimal sumByCategory(@Param("category") Category category);
}
