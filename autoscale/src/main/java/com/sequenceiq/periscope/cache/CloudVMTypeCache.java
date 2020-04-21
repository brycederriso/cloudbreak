package com.sequenceiq.periscope.cache;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cache.common.AbstractCacheDefinition;

@Component
public class CloudVMTypeCache extends AbstractCacheDefinition {

    public static final String CLOUD_VMTYPE_CACHE = "cloudVMTypeCache";

    private static final long MAX_ENTRIES = 10000L;

    private static final int TTL_IN_MINUTES = 30;

    @Override
    protected String getName() {
        return CLOUD_VMTYPE_CACHE;
    }

    @Override
    protected long getMaxEntries() {
        return MAX_ENTRIES;
    }

    @Override
    protected long getTimeToLiveSeconds() {
        return TimeUnit.MINUTES.toSeconds(TTL_IN_MINUTES);
    }
}
