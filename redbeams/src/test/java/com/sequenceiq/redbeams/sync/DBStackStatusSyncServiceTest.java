package com.sequenceiq.redbeams.sync;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.dto.Credential;
import com.sequenceiq.redbeams.service.CredentialService;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class DBStackStatusSyncServiceTest {

    private static final String DB_INSTANCE_IDENTIFIER = "db instance identifier";

    private static final String ENVIRONMENT_ID = "environment id";

    private static final Long DB_STACK_ID = 1234L;

    @Mock
    private CredentialService credentialService;

    @Mock
    private CredentialToCloudCredentialConverter credentialConverter;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudConnector<Object> cloudConnector;

    @Mock
    private Authenticator authenticator;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private ResourceConnector<Object> resourceConnector;

    @Mock
    private Credential credential;

    @Mock
    private DBStack dbStack;

    @Mock
    private DatabaseServer databaseServer;

    @Mock
    private Crn crn;

    private ArgumentCaptor<CloudContext> cloudContextArgumentCaptor;

    @InjectMocks
    private DBStackStatusSyncService victim;

    private final ExternalDatabaseStatus externalDatabaseStatus;

    private final DetailedDBStackStatus newDetailedDBStackStatus;

    private final Status savedStatus;

    public DBStackStatusSyncServiceTest(Status savedStatus, ExternalDatabaseStatus externalDatabaseStatus, DetailedDBStackStatus newDetailedDBStackStatus) {
        this.savedStatus = savedStatus;
        this.externalDatabaseStatus = externalDatabaseStatus;
        this.newDetailedDBStackStatus = newDetailedDBStackStatus;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                //Status should not be updated as saved and current are the same
                { Status.AVAILABLE, ExternalDatabaseStatus.STARTED, null },
                { Status.AVAILABLE, ExternalDatabaseStatus.STARTED, null },
                //ExternalDatabaseStatus should be converted to the correct DetailedDBStackStatus and update should be applied
                { Status.STOPPED, ExternalDatabaseStatus.STARTED, DetailedDBStackStatus.STARTED },
                { Status.AVAILABLE, ExternalDatabaseStatus.START_IN_PROGRESS, DetailedDBStackStatus.START_IN_PROGRESS },
                { Status.AVAILABLE, ExternalDatabaseStatus.STOPPED, DetailedDBStackStatus.STOPPED },
                { Status.AVAILABLE, ExternalDatabaseStatus.STOP_IN_PROGRESS, DetailedDBStackStatus.STOP_IN_PROGRESS },
                //UPDATE_IN_PROGRESS status covers all non handled statuses. In this case the DetailedDBStackStatus should be UNKNOWN
                { Status.AVAILABLE, ExternalDatabaseStatus.UPDATE_IN_PROGRESS, DetailedDBStackStatus.UNKNOWN },
        });
    }

    @Before
    public void initTests() {
        MockitoAnnotations.initMocks(this);
        cloudContextArgumentCaptor = ArgumentCaptor.forClass(CloudContext.class);

        when(dbStack.getEnvironmentId()).thenReturn(ENVIRONMENT_ID);
        when(dbStack.getDatabaseServer()).thenReturn(databaseServer);
        when(databaseServer.getName()).thenReturn(DB_INSTANCE_IDENTIFIER);
        when(credentialService.getCredentialByEnvCrn(ENVIRONMENT_ID)).thenReturn(credential);
        when(credentialConverter.convert(credential)).thenReturn(cloudCredential);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(authenticator.authenticate(cloudContextArgumentCaptor.capture(), Mockito.eq(cloudCredential))).thenReturn(authenticatedContext);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
    }

    @Test
    public void testStatusUpdate() throws Exception {
        when(resourceConnector.getDatabaseServerStatus(authenticatedContext, DB_INSTANCE_IDENTIFIER)).thenReturn(externalDatabaseStatus);
        when(dbStack.getId()).thenReturn(DB_STACK_ID);
        when(dbStack.getStatus()).thenReturn(savedStatus);
        when(dbStack.getOwnerCrn()).thenReturn(crn);

        victim.sync(dbStack);

        if (newDetailedDBStackStatus != null) {
            verify(dbStackStatusUpdater).updateStatus(DB_STACK_ID, newDetailedDBStackStatus);
        } else {
            verifyZeroInteractions(dbStackStatusUpdater);
        }
    }
}