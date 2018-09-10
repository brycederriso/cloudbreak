package com.sequenceiq.cloudbreak.service.decorator;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigService;

@Service
public class ClusterProxyDecorator {

    @Inject
    private ProxyConfigService proxyConfigService;

    public Cluster prepareProxyConfig(Cluster subject, String proxyName) {
        if (StringUtils.isNotBlank(proxyName)) {
            ProxyConfig proxyConfig = proxyConfigService.getByNameForWorkspace(proxyName, subject.getWorkspace());
            subject.setProxyConfig(proxyConfig);
        }
        return subject;
    }
}
