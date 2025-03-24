package com.zenika.samples.temporal.bank;

public record TransactionRequirement(String fromAccount, String toAccount, double amount) {
}