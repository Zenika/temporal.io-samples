package com.zenika.samples.temporal.bank.implementations;

import com.zenika.samples.temporal.bank.BankAccountActivities;
import io.temporal.failure.ApplicationFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class BankAccountActivitiesImpl implements BankAccountActivities {

    private final Map<String, Double> accountBalances = new HashMap<>();
    private final Set<String> blockedAccounts = new HashSet<>();

    public BankAccountActivitiesImpl() {
        // Simulating bank accounts
        accountBalances.put("A123", 500.0); // Account with funds
        accountBalances.put("B456", 200.0); // Normal account
        accountBalances.put("C789", 0.0);   // Account with no funds

        // Simulating a blocked account
        blockedAccounts.add("B456");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(BankAccountActivitiesImpl.class);


    @Override
    public void debitAccount(String accountId, double amount) {
        failRegularly();
        double balance = getBalance(accountId);
        if (balance < amount) {
            throw ApplicationFailure.newNonRetryableFailure(
                "Insufficient funds in account " + accountId,
                INSUFFICIENT_FUNDS,
                Map.of("balance", balance, "amount", amount));
        }
        accountBalances.put(accountId, balance - amount);
        LOGGER.info("💸 Debit: {} from account {}", amount, accountId);
    }

    @Override
    public void creditAccount(String accountId, double amount) {
        failRegularly();
        if (blockedAccounts.contains(accountId)) {
            throw ApplicationFailure.newNonRetryableFailure("Account " + accountId + " is blocked!", ACCOUNT_BLOCKED, null);
        }
        accountBalances.put(accountId, accountBalances.getOrDefault(accountId, 0.0) + amount);
        LOGGER.info("💰 Credit: {} to account {}", amount, accountId);
    }

    @Override
    public void refundAccount(String accountId, double amount) {
        failRegularly();
        accountBalances.put(accountId, accountBalances.getOrDefault(accountId, 0.0) + amount);
        LOGGER.info("🔄 Refund: {} to account {}", amount, accountId);
    }

    @Override
    public Double getBalance(String accountId) {
        return accountBalances.getOrDefault(accountId, 0.0);
    }

    private final AtomicInteger activityCounter = new AtomicInteger(0);

    // fail once in two
    private void failRegularly() {
        if (activityCounter.getAndIncrement() % 2 == 0) {
            throw new RuntimeException("Simulated failure");
        }
    }


}
