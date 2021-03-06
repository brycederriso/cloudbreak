package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Collections;
import java.util.Set;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.common.type.ScalingType;

import reactor.rx.Promise;

public class StackAndClusterUpscaleTriggerEvent extends StackScaleTriggerEvent {

    private final ScalingType scalingType;

    private final boolean singleMasterGateway;

    private final boolean kerberosSecured;

    private final boolean singleNodeCluster;

    private final ClusterManagerType clusterManagerType;

    public StackAndClusterUpscaleTriggerEvent(String selector, Long stackId, String instanceGroup, Integer adjustment, ScalingType scalingType) {
        super(selector, stackId, instanceGroup, adjustment, Collections.emptySet());
        this.scalingType = scalingType;
        singleMasterGateway = false;
        kerberosSecured = false;
        singleNodeCluster = false;
        clusterManagerType = ClusterManagerType.CLOUDERA_MANAGER;
    }

    public StackAndClusterUpscaleTriggerEvent(String selector, Long stackId, String instanceGroup, Integer adjustment, ScalingType scalingType,
            Set<String> hostNames, boolean singlePrimaryGateway, boolean kerberosSecured, Promise<AcceptResult> accepted, boolean singleNodeCluster,
            ClusterManagerType clusterManagerType) {
        super(selector, stackId, instanceGroup, adjustment, hostNames, accepted);
        this.scalingType = scalingType;
        singleMasterGateway = singlePrimaryGateway;
        this.kerberosSecured = kerberosSecured;
        this.singleNodeCluster = singleNodeCluster;
        this.clusterManagerType = clusterManagerType;
    }

    public ScalingType getScalingType() {
        return scalingType;
    }

    public boolean isSingleMasterGateway() {
        return singleMasterGateway;
    }

    public boolean isKerberosSecured() {
        return kerberosSecured;
    }

    public boolean isSingleNodeCluster() {
        return singleNodeCluster;
    }

    public ClusterManagerType getClusterManagerType() {
        return clusterManagerType;
    }

    @Override
    public StackAndClusterUpscaleTriggerEvent setRepair() {
        super.setRepair();
        return this;
    }
}
