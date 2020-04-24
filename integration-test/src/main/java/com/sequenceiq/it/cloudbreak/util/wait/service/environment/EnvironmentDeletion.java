package com.sequenceiq.it.cloudbreak.util.wait.service.environment;

import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.ARCHIVED;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.CREATE_FAILED;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.DELETE_FAILED;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionCheckerTask;

public class EnvironmentDeletion extends ExceptionCheckerTask<EnvironmentWaitObject> {

    public static final int ENVIRONMENT_RETRYING_INTERVAL = 5000;

    public static final int ENVIRONMENT_RETRYING_COUNT = 900;

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentDeletion.class);

    @Inject
    private EnvironmentClient environmentClient;

    public EnvironmentDeletion(EnvironmentClient environmentClient) {
        this.environmentClient = environmentClient;
    }

    @Override
    public boolean checkStatus(EnvironmentWaitObject environmentWaitObject) {
        String environmentCrn = environmentWaitObject.getEnvironmentCrn();
        try {
            LOGGER.info("Checking the state of environment termination progress: '{}'", environmentCrn);
            DetailedEnvironmentResponse environment = environmentClient.getEnvironmentClient().environmentV1Endpoint()
                    .getByCrn(environmentWaitObject.getEnvironmentCrn());
            if (environment != null) {
                if (environment.getEnvironmentStatus() == DELETE_FAILED) {
                    throw new TestFailException("Environment deletion operation failed: " + environment.getStatusReason());
                }
                if (!environment.getEnvironmentStatus().equals(ARCHIVED)) {
                    return false;
                }
            } else {
                LOGGER.info("Environment was not found.");
                return true;
            }
        } catch (Exception e) {
            throw new TestFailException("Environment deletion operation failed: " + e.getMessage() + "\n" + e);
        }
        return true;
    }

    @Override
    public void handleTimeout(EnvironmentWaitObject environmentWaitObject) {
        try {
            String environmentCrn = environmentWaitObject.getEnvironmentCrn();
            DetailedEnvironmentResponse environment = environmentClient.getEnvironmentClient().environmentV1Endpoint()
                    .getByCrn(environmentWaitObject.getEnvironmentCrn());
            if (environment == null) {
                throw new TestFailException("Environment was not found: " + environmentCrn);
            }
            throw new TestFailException(String.format("Wait operation timed out, environment deletion failed. environment status: '%s' "
                    + "statusReason: '%s'", environment.getEnvironmentStatus(), environment.getStatusReason()));
        } catch (Exception e) {
            throw new TestFailException("Wait operation timed out, environment deletion failed. Also failed to get environment status: "
                    + e.getMessage() + "\n" + e);
        }
    }

    @Override
    public String successMessage(EnvironmentWaitObject environmentWaitObject) {
        return "FreeIpa deletion successfully finished.";
    }

    @Override
    public boolean exitWaiting(EnvironmentWaitObject environmentWaitObject) {
        try {
            String environmentCrn = environmentWaitObject.getEnvironmentCrn();
            DetailedEnvironmentResponse environment = environmentClient.getEnvironmentClient().environmentV1Endpoint()
                    .getByCrn(environmentWaitObject.getEnvironmentCrn());
            if (environment == null) {
                LOGGER.info("Environment was not found '{}'. Exit waiting", environmentCrn);
                return false;
            }
            EnvironmentStatus status = environment.getEnvironmentStatus();
            if (status == DELETE_FAILED || status == CREATE_FAILED) {
                return false;
            }
            return status.isFailed();
        } catch (ProcessingException clientException) {
            LOGGER.error("Failed to describe FreeIpa cluster due to API client exception: {}.", clientException.getMessage());
        } catch (Exception e) {
            LOGGER.error("Exception occurred during describing FreeIpa for environment '{}'.", environmentWaitObject.getEnvironmentCrn(), e);
            return true;
        }
        return false;
    }
}
