package com.sequenceiq.it.cloudbreak.util.wait.service;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public abstract class ExceptionCheckerTask<T> implements StatusCheckerTask<T> {

    @Override
    public void handleException(Exception e) {
        throw new TestFailException(e.getMessage());
    }

}
