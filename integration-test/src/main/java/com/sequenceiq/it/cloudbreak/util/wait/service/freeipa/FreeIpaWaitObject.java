package com.sequenceiq.it.cloudbreak.util.wait.service.freeipa;

public class FreeIpaWaitObject {

    private final Long environmentId;

    private final String environmentCrn;

    public FreeIpaWaitObject(Long environmentId, String environmentCrn) {
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
