package com.sequenceiq.it.cloudbreak.util.wait.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WaitService<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(WaitService.class);

    public Map<String, String> waitWithTimeout(StatusCheckerTask<T> statusCheckerTask, T t, long interval, int maxAttempts, int maxFailure) {
        return waitWithTimeout(statusCheckerTask, t, interval, new TimeoutChecker(maxAttempts), maxFailure);
    }

    public Map<String, String> waitWithTimeout(StatusCheckerTask<T> statusCheckerTask, T t, long interval, TimeoutChecker timeoutChecker,
            int maxFailure) {
        boolean success = false;
        boolean timeout = false;
        int attempts = 0;
        int failures = 0;
        Exception actual = null;
        boolean exit = statusCheckerTask.exitWaiting(t);
        while (!timeout && !exit) {
            LOGGER.debug("Waiting round {}.", attempts);
            try {
                success = statusCheckerTask.checkStatus(t);
            } catch (Exception ex) {
                LOGGER.debug("Exception occurred during waiting: {}", ex.getMessage(), ex);
                failures++;
                actual = ex;
            }
            if (failures >= maxFailure) {
                LOGGER.debug("Waiting failure reached the limit which was {}, wait will drop the last exception.", maxFailure);
                statusCheckerTask.handleException(actual);
                return Map.of(WaitResult.FAILURE.name(), actual.getMessage());
            } else if (success) {
                LOGGER.debug(statusCheckerTask.successMessage(t));
                return Map.of(WaitResult.SUCCESS.name(), actual.getMessage());
            }
            sleep(interval);
            attempts++;
            timeout = timeoutChecker.checkTimeout();
            exit = statusCheckerTask.exitWaiting(t);
        }
        if (timeout) {
            LOGGER.debug("Wait timeout.");
            statusCheckerTask.handleTimeout(t);
            return Map.of(WaitResult.TIMEOUT.name(), actual.getMessage());
        }
        LOGGER.debug("Wait exiting.");
        return Map.of(WaitResult.EXIT.name(), actual.getMessage());
    }

    public String waitWithFailure(StatusCheckerTask<T> statusCheckerTask, T t, int interval, int maxAttempts) {
        return waitWithTimeout(statusCheckerTask, t, interval, maxAttempts, 1).entrySet().stream().findFirst().get().getKey();
    }

    private void sleep(long duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted exception occurred during waiting.", e);
            Thread.currentThread().interrupt();
        }
    }
}
