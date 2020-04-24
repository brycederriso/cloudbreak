package com.sequenceiq.it.cloudbreak.util.wait.service.environment;

public class EnvironmentWaitObject {

    private final Long environmentId;

    private final String environmentCrn;

    public EnvironmentWaitObject(Long environmentId, String environmentCrn) {
        this.environmentId = environmentId;
        this.environmentCrn = environmentCrn;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public Long getEnvironmentId() {
        return environmentId;
    }
}
