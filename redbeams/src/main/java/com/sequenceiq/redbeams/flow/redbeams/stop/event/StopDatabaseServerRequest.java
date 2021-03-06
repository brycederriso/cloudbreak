package com.sequenceiq.redbeams.flow.redbeams.stop.event;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

/**
 * A request for stop  a database server.
 */
public class StopDatabaseServerRequest extends RedbeamsEvent {

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final String dbInstanceIdentifier;

    public StopDatabaseServerRequest(CloudContext cloudContext, CloudCredential cloudCredential, String dbInstanceIdentifier) {
        super(cloudContext != null ? cloudContext.getId() : null);
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
        this.dbInstanceIdentifier = dbInstanceIdentifier;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public String getDbInstanceIdentifier() {
        return dbInstanceIdentifier;
    }

    public String toString() {
        return "StopDatabaseServerRequest{"
                + "cloudContext=" + cloudContext
                + ", cloudCredential=" + cloudCredential
                + ", dbInstanceIdentifier=" + dbInstanceIdentifier
                + '}';
    }
}
