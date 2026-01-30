package com.splitter.expense.service;

import com.splitter.expense.model.Expense;
import com.splitter.expense.model.ExpenseShare;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for calculating expense splits.
 */
@Component
public class SplitCalculator {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * Calculate shares for an expense based on split type.
     */
    public List<ExpenseShare> calculateShares(
            UUID expenseId,
            BigDecimal totalAmount,
            Expense.SplitType splitType,
            List<UUID> participants,
            Map<UUID, BigDecimal> exactAmounts,
            Map<UUID, BigDecimal> percentages,
            Map<UUID, Integer> units) {

        return switch (splitType) {
            case EQUAL -> calculateEqualSplit(expenseId, totalAmount, participants);
            case EXACT -> calculateExactSplit(expenseId, totalAmount, participants, exactAmounts);
            case PERCENTAGE -> calculatePercentageSplit(expenseId, totalAmount, participants, percentages);
            case SHARES -> calculateSharesSplit(expenseId, totalAmount, participants, units);
        };
    }

    /**
     * Split expense equally among participants.
     */
    private List<ExpenseShare> calculateEqualSplit(
            UUID expenseId,
            BigDecimal totalAmount,
            List<UUID> participants) {

        int participantCount = participants.size();
        BigDecimal shareAmount = totalAmount.divide(
                BigDecimal.valueOf(participantCount), SCALE, ROUNDING_MODE);

        // Handle rounding difference
        BigDecimal calculatedTotal = shareAmount.multiply(BigDecimal.valueOf(participantCount));
        BigDecimal remainder = totalAmount.subtract(calculatedTotal);

        List<ExpenseShare> shares = new ArrayList<>();
        for (int i = 0; i < participants.size(); i++) {
            UUID userId = participants.get(i);
            BigDecimal amount = shareAmount;
            
            // Add remainder to first participant
            if (i == 0 && remainder.compareTo(BigDecimal.ZERO) != 0) {
                amount = amount.add(remainder);
            }

            shares.add(ExpenseShare.builder()
                    .expenseId(expenseId)
                    .userId(userId)
                    .shareAmount(amount)
                    .sharePercentage(BigDecimal.valueOf(100.0 / participantCount)
                            .setScale(SCALE, ROUNDING_MODE))
                    .shareUnits(1)
                    .build());
        }

        return shares;
    }

    /**
     * Split expense by exact amounts.
     */
    private List<ExpenseShare> calculateExactSplit(
            UUID expenseId,
            BigDecimal totalAmount,
            List<UUID> participants,
            Map<UUID, BigDecimal> exactAmounts) {

        // Validate that exact amounts sum to total
        BigDecimal sum = exactAmounts.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sum.compareTo(totalAmount) != 0) {
            throw new IllegalArgumentException(
                    String.format("Exact amounts sum (%s) does not match total (%s)", sum, totalAmount));
        }

        return participants.stream()
                .map(userId -> {
                    BigDecimal amount = exactAmounts.getOrDefault(userId, BigDecimal.ZERO);
                    BigDecimal percentage = amount.divide(totalAmount, 4, ROUNDING_MODE)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(SCALE, ROUNDING_MODE);

                    return ExpenseShare.builder()
                            .expenseId(expenseId)
                            .userId(userId)
                            .shareAmount(amount)
                            .sharePercentage(percentage)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Split expense by percentages.
     */
    private List<ExpenseShare> calculatePercentageSplit(
            UUID expenseId,
            BigDecimal totalAmount,
            List<UUID> participants,
            Map<UUID, BigDecimal> percentages) {

        // Validate that percentages sum to 100
        BigDecimal sum = percentages.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sum.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new IllegalArgumentException(
                    String.format("Percentages sum (%s) does not equal 100", sum));
        }

        List<ExpenseShare> shares = new ArrayList<>();
        BigDecimal allocatedAmount = BigDecimal.ZERO;

        for (int i = 0; i < participants.size(); i++) {
            UUID userId = participants.get(i);
            BigDecimal percentage = percentages.getOrDefault(userId, BigDecimal.ZERO);
            BigDecimal amount;

            // For the last participant, allocate remaining amount to avoid rounding issues
            if (i == participants.size() - 1) {
                amount = totalAmount.subtract(allocatedAmount);
            } else {
                amount = totalAmount.multiply(percentage)
                        .divide(BigDecimal.valueOf(100), SCALE, ROUNDING_MODE);
                allocatedAmount = allocatedAmount.add(amount);
            }

            shares.add(ExpenseShare.builder()
                    .expenseId(expenseId)
                    .userId(userId)
                    .shareAmount(amount)
                    .sharePercentage(percentage)
                    .build());
        }

        return shares;
    }

    /**
     * Split expense by shares/units.
     */
    private List<ExpenseShare> calculateSharesSplit(
            UUID expenseId,
            BigDecimal totalAmount,
            List<UUID> participants,
            Map<UUID, Integer> units) {

        int totalUnits = units.values().stream()
                .mapToInt(Integer::intValue)
                .sum();

        if (totalUnits == 0) {
            throw new IllegalArgumentException("Total units cannot be zero");
        }

        BigDecimal perUnitAmount = totalAmount.divide(
                BigDecimal.valueOf(totalUnits), SCALE, ROUNDING_MODE);

        List<ExpenseShare> shares = new ArrayList<>();
        BigDecimal allocatedAmount = BigDecimal.ZERO;

        for (int i = 0; i < participants.size(); i++) {
            UUID userId = participants.get(i);
            int userUnits = units.getOrDefault(userId, 0);
            BigDecimal amount;

            // For the last participant, allocate remaining amount
            if (i == participants.size() - 1) {
                amount = totalAmount.subtract(allocatedAmount);
            } else {
                amount = perUnitAmount.multiply(BigDecimal.valueOf(userUnits));
                allocatedAmount = allocatedAmount.add(amount);
            }

            BigDecimal percentage = BigDecimal.valueOf(userUnits)
                    .divide(BigDecimal.valueOf(totalUnits), 4, ROUNDING_MODE)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(SCALE, ROUNDING_MODE);

            shares.add(ExpenseShare.builder()
                    .expenseId(expenseId)
                    .userId(userId)
                    .shareAmount(amount)
                    .sharePercentage(percentage)
                    .shareUnits(userUnits)
                    .build());
        }

        return shares;
    }
}
