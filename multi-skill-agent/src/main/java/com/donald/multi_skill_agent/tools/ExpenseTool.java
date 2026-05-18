package com.donald.multi_skill_agent.tools;

import com.donald.multi_skill_agent.model.Category;
import com.donald.multi_skill_agent.model.Expense;
import com.donald.multi_skill_agent.repository.ExpenseRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class ExpenseTool {

    private final ExpenseRepository expenseRepository;

    public ExpenseTool(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    @Tool(description = "Add a new expense")
    public Expense addExpense(
            @ToolParam(description = "Short description of the expense") String description,
            @ToolParam(description = "Amount spent") BigDecimal amount,
            @ToolParam(description = "Category: FOOD, TRANSPORT, ENTERTAINMENT, HEALTH, UTILITIES, SHOPPING, OTHER") Category
                    category,
            @ToolParam(description = "Date of expense in YYYY-MM-DD format") LocalDate date) {
        return expenseRepository.save(new Expense(null, description, amount, category, date));
    }

    @Tool(description = "Get all expenses")
    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll();
    }

    @Tool(description = "Get expenses by category")
    public List<Expense> getExpensesByCategory(
            @ToolParam(description = "Category: FOOD, TRANSPORT, ENTERTAINMENT, HEALTH, UTILITIES, SHOPPING, OTHER") Category
                    category) {
        return expenseRepository.findByCategory(category);
    }

    @Tool(description = "Get expenses between two dates")
    public List<Expense> getExpensesByDateRange(
            @ToolParam(description = "Start date in YYYY-MM-DD format") LocalDate start,
            @ToolParam(description = "End date in YYYY-MM-DD format") LocalDate end) {
        return expenseRepository.findByDateBetween(start, end);
    }

    @Tool(description = "Get expenses greater than a specific amount")
    public List<Expense> getExpensesGreaterThan(
            @ToolParam(description = "Minimum amount") BigDecimal amount) {
        return expenseRepository.findByAmountGreaterThan(amount);
    }

    @Tool(description = "Get total amount spent in a category")
    public BigDecimal getTotalByCategory(
            @ToolParam(description = "Category: FOOD, TRANSPORT, ENTERTAINMENT, HEALTH, UTILITIES, SHOPPING, OTHER") Category
                    category) {
        return expenseRepository.sumByCategory(category);
    }

    @Tool(description = "Delete an expense by ID")
    public String deleteExpense(
            @ToolParam(description = "ID of the expense to delete") Long id) {
        expenseRepository.deleteById(id);
        return "Expense " + id + " deleted.";
    }
}
