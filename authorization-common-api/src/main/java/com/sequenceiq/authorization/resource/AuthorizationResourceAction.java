package com.sequenceiq.authorization.resource;

import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

public enum AuthorizationResourceAction {
    CHANGE_CREDENTIAL("changeCredential", ActionType.RESOURCE_DEPENDENT),
    EDIT("edit", ActionType.RESOURCE_DEPENDENT),
    START("start", ActionType.RESOURCE_DEPENDENT),
    STOP("stop", ActionType.RESOURCE_DEPENDENT),
    DELETE("delete", ActionType.RESOURCE_DEPENDENT),
    DESCRIBE("describe", ActionType.RESOURCE_DEPENDENT),
    ACCESS_ENVIRONMENT("accessEnvironment", ActionType.RESOURCE_DEPENDENT),
    ADMIN_FREEIPA("adminFreeIPA", ActionType.RESOURCE_DEPENDENT),
    CREATE("create", ActionType.RESOURCE_INDEPENDENT),
    GET_KEYTAB("getKeytab", ActionType.RESOURCE_INDEPENDENT),
    // deprecated actions, please do not use them
    RD_READ("rdRead", ActionType.RESOURCE_DEPENDENT),
    RD_WRITE("rdWrite", ActionType.RESOURCE_DEPENDENT),
    READ("read", ActionType.RESOURCE_INDEPENDENT),
    WRITE("write", ActionType.RESOURCE_INDEPENDENT);

    private final String action;

    private final ActionType actionType;

    AuthorizationResourceAction(String action, ActionType actionType) {
        this.action = action;
        this.actionType = actionType;
    }

    public String getAction() {
        return action;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public static Optional<AuthorizationResourceAction> getByName(String name) {
        return Arrays.stream(AuthorizationResourceAction.values())
                .filter(resource -> StringUtils.equals(resource.getAction(), name))
                .findAny();
    }

    public enum ActionType {
        RESOURCE_DEPENDENT,
        RESOURCE_INDEPENDENT;
    }
}
