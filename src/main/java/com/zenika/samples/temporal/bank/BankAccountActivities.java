package com.zenika.samples.temporal.bank;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface BankAccountActivities {
    void debitAccount(String accountId, double amount);

    void creditAccount(String accountId, double amount);

    void refundAccount(String accountId, double amount);

    String INSUFFICIENT_FUNDS = "InsufficientFunds";
    String ACCOUNT_BLOCKED = "AccountBlocked";

    Double getBalance(String accountId);
}