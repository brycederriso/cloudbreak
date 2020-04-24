package com.sequenceiq.it.cloudbreak.util.wait.service.freeipa;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.FreeIPAClient;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionCheckerTask;

public class FreeIpaCreation extends ExceptionCheckerTask<FreeIpaWaitObject> {

    public static final int FREEIPA_RETRYING_INTERVAL = 25000;

    public static final int FREEIPA_RETRYING_COUNT = 900;

    public static final int FREEIPA_FAILURE_COUNT = 3;

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaCreation.class);
    
    @Inject
    private FreeIPAClient freeIPAClient;

    @Inject
    private EnvironmentClient environmentClient;

    public FreeIpaCreation(FreeIPAClient freeIPAClient) {
        this.freeIPAClient = freeIPAClient;
    }

    @Override
    public boolean checkStatus(FreeIpaWaitObject freeIpaWaitObject) {
        String environmentCrn = freeIpaWaitObject.getEnvironmentCrn();
        try {
            LOGGER.info("Checking the state of FreeIpa creation progress for environment: '{}'", environmentCrn);
            DescribeFreeIpaResponse freeIpa = freeIPAClient.getFreeIpaClient().getFreeIpaV1Endpoint().describe(environmentCrn);
            if (freeIpa == null) {
                throw new TestFailException("FreeIpa cluster not found for environment: " + environmentCrn);
            }
            if (freeIpa.getStatus().isDeletionInProgress() || freeIpa.getStatus().isSuccessfullyDeleted()) {
                LOGGER.error("FreeIpa '{}' '{}' is getting terminated (status:'{}'), waiting is cancelled.",
                        freeIpa.getName(),
                        freeIpa.getCrn(),
                        freeIpa.getStatus());
                throw new TestFailException("FreeIpa instance deleted under the creation process.");
            }
            if (freeIpa.getStatus().isFailed()) {
                LOGGER.error("FreeIpa '{}' '{}' is in failed state (status:'{}'), waiting is cancelled.",
                        freeIpa.getName(),
                        freeIpa.getCrn(),
                        freeIpa.getStatus());
                throw new TestFailException(String.format("FreeIpa creation failed. Status: '%s' statusReason: '%s'",
                        freeIpa.getStatus(), freeIpa.getStatusReason()));
            }
            if (freeIpa.getStatus().isAvailable()) {
                return true;
            }
        } catch (Exception e) {
            throw new TestFailException("FreeIpa creation failed: " + e.getMessage() + "\n" + e);
        }
        return false;
    }

    @Override
    public void handleTimeout(FreeIpaWaitObject freeIpaWaitObject) {
        try {
            DescribeFreeIpaResponse freeIpa = freeIPAClient.getFreeIpaClient().getFreeIpaV1Endpoint().describe(freeIpaWaitObject.getEnvironmentCrn());
            if (freeIpa == null) {
                throw new TestFailException("FreeIpa cluster was not found for environment: " + freeIpaWaitObject.getEnvironmentCrn());
            }
            throw new TestFailException(String.format("Wait operation timed out, freeIpa creation failed. FreeIpa status: '%s' "
                    + "statusReason: '%s'", freeIpa.getStatus(), freeIpa.getStatusReason()));
        } catch (Exception e) {
            throw new TestFailException("Wait operation timed out, freeIpa creation failed. Also failed to get freeIpa status: "
                    + e.getMessage() + "\n" + e);
        }
    }

    @Override
    public String successMessage(FreeIpaWaitObject freeIpaWaitObject) {
        return String.format("FreeIpa creation successfully finished '%s'", freeIpaWaitObject.getEnvironmentCrn());
    }

    @Override
    public boolean exitWaiting(FreeIpaWaitObject freeIpaWaitObject) {
        try {
            String environmentCrn = freeIpaWaitObject.getEnvironmentCrn();
            DescribeFreeIpaResponse freeIpa = freeIPAClient.getFreeIpaClient().getFreeIpaV1Endpoint().describe(freeIpaWaitObject.getEnvironmentCrn());
            if (freeIpa == null) {
                LOGGER.info("FreeIpa was not found for environment '{}'. Exit waiting", environmentCrn);
                return false;
            }
            Status status = freeIpa.getStatus();
            if (status == Status.DELETE_FAILED || status == Status.CREATE_FAILED) {
                return false;
            }
            return status.isFailed();
        } catch (ProcessingException clientException) {
            LOGGER.error("Failed to describe freeIpa cluster due to API client exception: {}.", clientException.getMessage());
        } catch (Exception e) {
            LOGGER.error("Exception occurred during describing freeIpa for environment '{}'.", freeIpaWaitObject.getEnvironmentCrn(), e);
            return true;
        }
        return false;
    }
}
