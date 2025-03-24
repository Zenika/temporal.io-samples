package com.zenika.samples.temporal;

import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;

public class Exceptions {
    public static boolean hasCause(ActivityFailure e, String failureType) {
        var cause = e.getCause();
        return switch (cause) {
            case ApplicationFailure applicationFailure -> failureType.equals(applicationFailure.getType());
            default -> false;
        };
    }
}
