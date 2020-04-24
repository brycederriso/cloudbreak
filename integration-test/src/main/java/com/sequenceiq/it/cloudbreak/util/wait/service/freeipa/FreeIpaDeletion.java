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

public class FreeIpaDeletion extends ExceptionCheckerTask<FreeIpaWaitObject> {

    public static final int FREEIPA_RETRYING_INTERVAL = 5000;

    public static final int FREEIPA_RETRYING_COUNT = 900;

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaDeletion.class);

    @Inject
    private FreeIPAClient freeIPAClient;

    @Inject
    private EnvironmentClient environmentClient;

    public FreeIpaDeletion(FreeIPAClient freeIPAClient) {
        this.freeIPAClient = freeIPAClient;
    }

    @Override
    public boolean checkStatus(FreeIpaWaitObject FreeIpaWaitObject) {
        String environmentCrn = FreeIpaWaitObject.getEnvironmentCrn();
        try {
            LOGGER.info("Checking the state of FreeIpa termination progress for environment: '{}'", environmentCrn);
            DescribeFreeIpaResponse freeIpa = freeIPAClient.getFreeIpaClient().getFreeIpaV1Endpoint().describe(environmentCrn);
            if (freeIpa != null) {
                if (freeIpa.getStatus() == Status.DELETE_FAILED) {
                    throw new TestFailException("FreeIpa deletion operation failed: " + freeIpa.getStatusReason());
                }
                if (!freeIpa.getStatus().isSuccessfullyDeleted()) {
                    return false;
                }
            } else {
                LOGGER.info("FreeIpa was not found.");
                return true;
            }
        } catch (Exception e) {
            throw new TestFailException("FreeIpa deletion operation failed: " + e.getMessage() + "\n" + e);
        }
        return true;
    }

    @Override
    public void handleTimeout(FreeIpaWaitObject FreeIpaWaitObject) {
        try {
            String environmentCrn = FreeIpaWaitObject.getEnvironmentCrn();
            DescribeFreeIpaResponse freeIpa = freeIPAClient.getFreeIpaClient().getFreeIpaV1Endpoint().describe(environmentCrn);
            if (freeIpa == null) {
                throw new TestFailException("FreeIpa was not found for environment: " + environmentCrn);
            }
            throw new TestFailException(String.format("Wait operation timed out, FreeIpa deletion failed. FreeIpa status: '%s' "
                    + "statusReason: '%s'", freeIpa.getStatus(), freeIpa.getStatusReason()));
        } catch (Exception e) {
            throw new TestFailException("Wait operation timed out, FreeIpa deletion failed. Also failed to get FreeIpa status: "
                    + e.getMessage() + "\n" + e);
        }
    }

    @Override
    public String successMessage(FreeIpaWaitObject FreeIpaWaitObject) {
        return "FreeIpa deletion successfully finished.";
    }

    @Override
    public boolean exitWaiting(FreeIpaWaitObject FreeIpaWaitObject) {
        try {
            String environmentCrn = FreeIpaWaitObject.getEnvironmentCrn();
            DescribeFreeIpaResponse freeIpa = freeIPAClient.getFreeIpaClient().getFreeIpaV1Endpoint().describe(environmentCrn);
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
            LOGGER.error("Failed to describe FreeIpa cluster due to API client exception: {}.", clientException.getMessage());
        } catch (Exception e) {
            LOGGER.error("Exception occurred during describing FreeIpa for environment '{}'.", FreeIpaWaitObject.getEnvironmentCrn(), e);
            return true;
        }
        return false;
    }
}
