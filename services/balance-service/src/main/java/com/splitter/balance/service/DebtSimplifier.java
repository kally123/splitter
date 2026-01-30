package com.splitter.balance.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * Service for simplifying debts using graph-based algorithm.
 * Minimizes the number of transactions needed to settle all debts.
 */
@Component
public class DebtSimplifier {

    /**
     * Simplify debts to minimize the number of transactions.
     *
     * @param debts List of debts as (fromUserId, toUserId, amount)
     * @return Simplified list of debts
     */
    public List<Debt> simplify(List<Debt> debts) {
        if (debts == null || debts.isEmpty()) {
            return Collections.emptyList();
        }

        // Calculate net balance for each person
        Map<UUID, BigDecimal> netBalances = new HashMap<>();
        
        for (Debt debt : debts) {
            netBalances.merge(debt.fromUserId(), debt.amount().negate(), BigDecimal::add);
            netBalances.merge(debt.toUserId(), debt.amount(), BigDecimal::add);
        }

        // Separate into creditors (positive balance) and debtors (negative balance)
        List<UserAmount> creditors = new ArrayList<>();
        List<UserAmount> debtors = new ArrayList<>();

        for (Map.Entry<UUID, BigDecimal> entry : netBalances.entrySet()) {
            BigDecimal balance = entry.getValue();
            if (balance.compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(new UserAmount(entry.getKey(), balance));
            } else if (balance.compareTo(BigDecimal.ZERO) < 0) {
                debtors.add(new UserAmount(entry.getKey(), balance.negate()));
            }
        }

        // Sort by amount (descending) for more efficient matching
        creditors.sort((a, b) -> b.amount().compareTo(a.amount()));
        debtors.sort((a, b) -> b.amount().compareTo(a.amount()));

        // Match debtors with creditors
        List<Debt> simplified = new ArrayList<>();
        int i = 0, j = 0;

        while (i < debtors.size() && j < creditors.size()) {
            UserAmount debtor = debtors.get(i);
            UserAmount creditor = creditors.get(j);

            BigDecimal transferAmount = debtor.amount().min(creditor.amount());
            
            if (transferAmount.compareTo(BigDecimal.ZERO) > 0) {
                simplified.add(new Debt(debtor.userId(), creditor.userId(), transferAmount));
            }

            BigDecimal newDebtorAmount = debtor.amount().subtract(transferAmount);
            BigDecimal newCreditorAmount = creditor.amount().subtract(transferAmount);

            debtors.set(i, new UserAmount(debtor.userId(), newDebtorAmount));
            creditors.set(j, new UserAmount(creditor.userId(), newCreditorAmount));

            if (newDebtorAmount.compareTo(BigDecimal.ZERO) == 0) i++;
            if (newCreditorAmount.compareTo(BigDecimal.ZERO) == 0) j++;
        }

        return simplified;
    }

    /**
     * Record representing a debt from one user to another.
     */
    public record Debt(UUID fromUserId, UUID toUserId, BigDecimal amount) {}

    /**
     * Internal record for user-amount pairs.
     */
    private record UserAmount(UUID userId, BigDecimal amount) {}
}
