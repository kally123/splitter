package com.splitter.expense.exception;

import java.util.UUID;

/**
 * Exception thrown when an expense is not found.
 */
public class ExpenseNotFoundException extends RuntimeException {

    public ExpenseNotFoundException(UUID expenseId) {
        super(String.format("Expense not found with ID: %s", expenseId));
    }
}
