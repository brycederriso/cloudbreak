package com.sequenceiq.it.cloudbreak.util.wait.service.environment;

import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.ARCHIVED;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.CLUSTER_DEFINITION_CLEANUP_PROGRESS;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.CREATE_FAILED;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.DATAHUB_CLUSTERS_DELETE_IN_PROGRESS;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.DATALAKE_CLUSTERS_DELETE_IN_PROGRESS;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.DELETE_FAILED;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.DELETE_INITIATED;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.FREEIPA_DELETE_IN_PROGRESS;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.IDBROKER_MAPPINGS_DELETE_IN_PROGRESS;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.NETWORK_DELETE_IN_PROGRESS;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.PUBLICKEY_DELETE_IN_PROGRESS;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.RDBMS_DELETE_IN_PROGRESS;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.S3GUARD_TABLE_DELETE_IN_PROGRESS;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.UMS_RESOURCE_DELETE_IN_PROGRESS;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionCheckerTask;

public class EnvironmentCreation extends ExceptionCheckerTask<EnvironmentWaitObject> {

    public static final int ENVIRONMENT_RETRYING_INTERVAL = 25000;

    public static final int ENVIRONMENT_RETRYING_COUNT = 900;

    public static final int ENVIRONMENT_FAILURE_COUNT = 3;

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentCreation.class);

    @Inject
    private EnvironmentClient environmentClient;

    public EnvironmentCreation(EnvironmentClient environmentClient) {
        this.environmentClient = environmentClient;
    }

    @Override
    public boolean checkStatus(EnvironmentWaitObject environmentWaitObject) {
        String environmentCrn = environmentWaitObject.getEnvironmentCrn();
        try {
            LOGGER.info("Checking the state of Environment creation progress: '{}'", environmentCrn);
            DetailedEnvironmentResponse environment = environmentClient.getEnvironmentClient().environmentV1Endpoint()
                    .getByCrn(environmentCrn);
            if (environment == null) {
                throw new TestFailException("Environment not found: " + environmentCrn);
            }
            if (isDeletionInProgress(environment.getEnvironmentStatus()) || environment.getEnvironmentStatus() == ARCHIVED) {
                LOGGER.error("Environment '{}' '{}' is getting terminated (status:'{}'), waiting is cancelled.",
                        environment.getName(),
                        environment.getCrn(),
                        environment.getEnvironmentStatus());
                throw new TestFailException("Environment instance deleted under the creation process.");
            }
            if (environment.getEnvironmentStatus().isFailed()) {
                LOGGER.error("Environment '{}' '{}' is in failed state (status:'{}'), waiting is cancelled.",
                        environment.getName(),
                        environment.getCrn(),
                        environment.getEnvironmentStatus());
                throw new TestFailException(String.format("Environment creation failed. Status: '%s' statusReason: '%s'",
                        environment.getEnvironmentStatus(), environment.getStatusReason()));
            }
            if (environment.getEnvironmentStatus().isAvailable()) {
                return true;
            }
        } catch (Exception e) {
            throw new TestFailException("Environment creation failed: " + e.getMessage() + "\n" + e);
        }
        return false;
    }

    @Override
    public void handleTimeout(EnvironmentWaitObject environmentWaitObject) {
        try {
            String environmentCrn = environmentWaitObject.getEnvironmentCrn();
            DetailedEnvironmentResponse environment = environmentClient.getEnvironmentClient().environmentV1Endpoint()
                    .getByCrn(environmentCrn);
            if (environment == null) {
                throw new TestFailException("Environment cluster was not found for environment: " + environmentCrn);
            }
            throw new TestFailException(String.format("Wait operation timed out, environment creation failed. Environment status: '%s' "
                    + "statusReason: '%s'", environment.getEnvironmentStatus(), environment.getStatusReason()));
        } catch (Exception e) {
            throw new TestFailException("Wait operation timed out, environment creation failed. Also failed to get environment status: "
                    + e.getMessage() + "\n" + e);
        }
    }

    @Override
    public String successMessage(EnvironmentWaitObject environmentWaitObject) {
        return String.format("Environment creation successfully finished '%s'", environmentWaitObject.getEnvironmentCrn());
    }

    @Override
    public boolean exitWaiting(EnvironmentWaitObject environmentWaitObject) {
        try {
            String environmentCrn = environmentWaitObject.getEnvironmentCrn();
            DetailedEnvironmentResponse environment = environmentClient.getEnvironmentClient().environmentV1Endpoint()
                    .getByCrn(environmentCrn);
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
            LOGGER.error("Failed to describe environment due to API client exception: {}.", clientException.getMessage());
        } catch (Exception e) {
            LOGGER.error("Exception occurred during describing environment '{}'.", environmentWaitObject.getEnvironmentCrn(), e);
            return true;
        }
        return false;
    }

    private boolean isDeletionInProgress(EnvironmentStatus environmentStatus) {
        Collection<EnvironmentStatus> DELETE_IN_PROGRESS_STATUSES = List.of(DELETE_INITIATED, NETWORK_DELETE_IN_PROGRESS, RDBMS_DELETE_IN_PROGRESS,
                FREEIPA_DELETE_IN_PROGRESS, CLUSTER_DEFINITION_CLEANUP_PROGRESS, UMS_RESOURCE_DELETE_IN_PROGRESS, IDBROKER_MAPPINGS_DELETE_IN_PROGRESS,
                S3GUARD_TABLE_DELETE_IN_PROGRESS, DATAHUB_CLUSTERS_DELETE_IN_PROGRESS, DATALAKE_CLUSTERS_DELETE_IN_PROGRESS, PUBLICKEY_DELETE_IN_PROGRESS);
        return DELETE_IN_PROGRESS_STATUSES.contains(environmentStatus);
    }
}
