package com.sequenceiq.authorization.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.authorization.annotation.CheckPermissionByEnvironmentCrn;
import com.sequenceiq.authorization.annotation.EnvironmentCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;

@RunWith(MockitoJUnitRunner.class)
public class EnvironmentCrnPermissionCheckerTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    @Mock
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Mock
    private ResourceBasedCrnProvider resourceBasedCrnProvider;

    @Spy
    private List<ResourceBasedCrnProvider> resourceBasedCrnProviders = new ArrayList<ResourceBasedCrnProvider>();

    @InjectMocks
    private EnvironmentCrnPermissionChecker underTest;

    @Test
    public void testCheckPermissions() {
        resourceBasedCrnProviders.add(resourceBasedCrnProvider);
        when(commonPermissionCheckingUtils.proceed(any(), any(), anyLong())).thenReturn(null);
        doNothing().when(commonPermissionCheckingUtils).checkPermissionForUserOnResource(any(), any(), anyString(), anyString());
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn("resource");
        when(resourceBasedCrnProvider.getResourceCrnByEnvironmentCrn(any())).thenReturn(USER_CRN);
        when(resourceBasedCrnProvider.getResourceType()).thenReturn(AuthorizationResourceType.CREDENTIAL);

        CheckPermissionByEnvironmentCrn rawMethodAnnotation = new CheckPermissionByEnvironmentCrn() {

            @Override
            public AuthorizationResourceAction action() {
                return AuthorizationResourceAction.WRITE;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return CheckPermissionByEnvironmentCrn.class;
            }
        };

        underTest.populateResourceBasedCrnProviderMap();
        underTest.checkPermissions(rawMethodAnnotation, AuthorizationResourceType.CREDENTIAL, USER_CRN, null, null, 0L);

        verify(commonPermissionCheckingUtils).proceed(any(), any(), anyLong());
        verify(commonPermissionCheckingUtils).getParameter(any(), any(), eq(EnvironmentCrn.class), eq(String.class));
        verify(commonPermissionCheckingUtils, times(0)).checkPermissionForUser(any(), any(), anyString());
        verify(commonPermissionCheckingUtils)
                .checkPermissionForUserOnResource(eq(AuthorizationResourceType.CREDENTIAL), eq(AuthorizationResourceAction.WRITE), eq(USER_CRN), anyString());
    }
}
