package com.sequenceiq.authorization.service;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.RightUtils;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Service
public class UmsAccountAuthorizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UmsAccountAuthorizationService.class);

    @Inject
    private GrpcUmsClient umsClient;

    public void checkRightOfUser(String userCrn, AuthorizationResourceType resourceType, AuthorizationResourceAction action) {
        String right = RightUtils.getRight(resourceType, action);
        String unauthorizedMessage = String.format("You have no right to perform %s in account %s.", right, Crn.fromString(userCrn).getAccountId());
        checkRightOfUser(userCrn, resourceType, action, unauthorizedMessage);
    }

    public Boolean hasRightOfUser(String userCrn, String resourceType, String action) {
        Optional<AuthorizationResourceType> resourceEnum = AuthorizationResourceType.getByName(resourceType);
        Optional<AuthorizationResourceAction> actionEnum = AuthorizationResourceAction.getByName(action);
        if (!resourceEnum.isPresent() || !actionEnum.isPresent()) {
            throw new BadRequestException("Resource or action cannot be found by request!");
        }
        if (!hasRightOfUser(userCrn, resourceEnum.get(), actionEnum.get())) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    private void checkRightOfUser(String userCrn, AuthorizationResourceType resourceType, AuthorizationResourceAction action, String unauthorizedMessage) {
        if (!hasRightOfUser(userCrn, resourceType, action)) {
            LOGGER.error(unauthorizedMessage);
            throw new AccessDeniedException(unauthorizedMessage);
        }
    }

    private boolean hasRightOfUser(String userCrn, AuthorizationResourceType resourceType, AuthorizationResourceAction action) {
        return umsClient.checkRight(userCrn, userCrn, RightUtils.getRight(resourceType, action), getRequestId());
    }

    // Checks that the calling actor is either performing an action against themselves or have the right
    public void checkCallerIsSelfOrHasRight(String actorCrnStr, String targetUserCrnStr, AuthorizationResourceType resource,
            AuthorizationResourceAction action) {
        Crn actorCrn = Crn.safeFromString(actorCrnStr);
        Crn targetUserCrn = Crn.safeFromString(targetUserCrnStr);
        if (actorCrn.equals(targetUserCrn)) {
            return;
        }
        String right = RightUtils.getRight(resource, action);
        if (!actorCrn.getAccountId().equals(targetUserCrn.getAccountId())) {
            String unauthorizedMessage = "Unauthorized to run this operation in a different account";
            LOGGER.error(unauthorizedMessage);
            throw new AccessDeniedException(unauthorizedMessage);
        }
        if (!umsClient.checkRight(GrpcUmsClient.INTERNAL_ACTOR_CRN, actorCrn.toString(), RightUtils.getRight(resource, action), getRequestId())) {
            String unauthorizedMessage = String.format("You have no right to perform %s on user %s.", right, targetUserCrnStr);
            LOGGER.error(unauthorizedMessage);
            throw new AccessDeniedException(unauthorizedMessage);
        }
    }

    protected Optional<String> getRequestId() {
        String requestId = MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString());
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        return Optional.of(requestId);
    }
}
