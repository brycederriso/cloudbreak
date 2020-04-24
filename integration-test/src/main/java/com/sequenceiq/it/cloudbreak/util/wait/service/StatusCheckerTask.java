package com.sequenceiq.it.cloudbreak.util.wait.service;

public interface StatusCheckerTask<T> {

    boolean checkStatus(T t);

    void handleTimeout(T t);

    String successMessage(T t);

    boolean exitWaiting(T t);

    void handleException(Exception e);
}
