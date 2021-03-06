package com.sequenceiq.authorization.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import javax.ws.rs.BadRequestException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;

@RunWith(MockitoJUnitRunner.class)
public class UmsAccountAuthorizationServiceTest {

    private static final String USER_ID = "userId";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:" + USER_ID;

    private static final String RESOURCE_CRN = "crn:cdp:datalake:us-west-1:1234:resource:1";

    private static final String RESOURCE_CRN2 = "crn:cdp:datalake:us-west-1:1234:resource:2";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private GrpcUmsClient umsClient;

    @InjectMocks
    private UmsAccountAuthorizationService underTest;

    @Test
    public void testCheckReadRight() {
        when(umsClient.checkRight(anyString(), anyString(), anyString(), any())).thenReturn(false);

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("You have no right to perform datalake/write in account 1234");

        underTest.checkRightOfUser(USER_CRN, AuthorizationResourceType.DATALAKE, AuthorizationResourceAction.WRITE);
    }

    @Test
    public void testCheckWriteRight() {
        when(umsClient.checkRight(anyString(), anyString(), anyString(), any())).thenReturn(false);

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("You have no right to perform datalake/read in account 1234");

        underTest.checkRightOfUser(USER_CRN, AuthorizationResourceType.DATALAKE, AuthorizationResourceAction.READ);
    }

    @Test
    public void testHasRightOfUserWithValidResourceTypeAndAction() {
        when(umsClient.checkRight(anyString(), anyString(), anyString(), any())).thenReturn(true);

        assertTrue(underTest.hasRightOfUser(USER_CRN, "datalake", "write"));

        when(umsClient.checkRight(anyString(), anyString(), anyString(), any())).thenReturn(false);

        assertFalse(underTest.hasRightOfUser(USER_CRN, "datalake", "write"));
    }

    @Test
    public void testHasRightOfUserWithInvalidResourceType() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Resource or action cannot be found by request!");

        underTest.hasRightOfUser(USER_CRN, "invalid", "write");

        verifyZeroInteractions(umsClient);
    }

    @Test
    public void testHasRightOfUserWithInvalidAction() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Resource or action cannot be found by request!");

        underTest.hasRightOfUser(USER_CRN, "datalake", "invalid");

        verifyZeroInteractions(umsClient);
    }

    @Test
    public void testCheckCallerIsSelfOrHasRightSameActor() {
        underTest.checkCallerIsSelfOrHasRight(USER_CRN, USER_CRN, AuthorizationResourceType.ENVIRONMENT, AuthorizationResourceAction.GET_KEYTAB);
    }

    @Test
    public void testCheckCallerIsSelfOrHasRightDifferent() {
        String userInDifferentAccount = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();
        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("Unauthorized to run this operation in a different account");

        underTest.checkCallerIsSelfOrHasRight(USER_CRN, userInDifferentAccount, AuthorizationResourceType.ENVIRONMENT, AuthorizationResourceAction.GET_KEYTAB);
    }

    @Test
    public void testActorAndTargetDifferentAndMissingRight() {
        String user2 = "crn:cdp:iam:us-west-1:1234:user:someOtherUserId";
        when(umsClient.checkRight(any(), any(), any(), any())).thenReturn(false);
        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage(String.format("You have no right to perform environments/getKeytab on user %s", user2));

        underTest.checkCallerIsSelfOrHasRight(USER_CRN, user2, AuthorizationResourceType.ENVIRONMENT, AuthorizationResourceAction.GET_KEYTAB);
    }

    @Test
    public void testActorAndTargetDifferentHasRequiredRight() {
        String user2 = "crn:cdp:iam:us-west-1:1234:user:someOtherUserId";
        when(umsClient.checkRight(any(), any(), any(), any())).thenReturn(true);

        underTest.checkCallerIsSelfOrHasRight(USER_CRN, user2, AuthorizationResourceType.ENVIRONMENT, AuthorizationResourceAction.GET_KEYTAB);
    }
}
