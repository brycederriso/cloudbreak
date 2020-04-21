package com.sequenceiq.periscope.monitor.handler;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cm.DataView;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterManager;
import com.sequenceiq.periscope.model.CloudInstanceType;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.security.TlsHttpClientConfigurationService;
import com.sequenceiq.periscope.utils.ClusterUtils;

@Service
public class ClouderaManagerCommunicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerCommunicator.class);

    @Inject
    private TlsHttpClientConfigurationService tlsHttpClientConfigurationService;

    @Inject
    private SecretService secretService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Cacheable(cacheNames = "cloudVMTypeCache", unless = "#result == null", key = "#cluster.id + #hostGroup")
    public Optional<CloudInstanceType> getCloudVMDetailsForHostGroup(Cluster cluster, String hostGroup, Set<String> hostGroupFqdns) {
        try {
            LOGGER.debug("Retrieving CloudVMType for cluster '{}', hostGroup '{}'", cluster.getStackCrn(), hostGroupFqdns);
            HttpClientConfig httpClientConfig = tlsHttpClientConfigurationService.buildTLSClientConfig(cluster.getStackCrn(),
                    cluster.getClusterManager().getHost(), cluster.getTunnel());
            ClusterManager cm = cluster.getClusterManager();
            String user = secretService.get(cm.getUser());
            String pass = secretService.get(cm.getPass());
            ApiClient client = clouderaManagerApiClientProvider.getClient(Integer.valueOf(cm.getPort()), user, pass, httpClientConfig);
            HostsResourceApi hostsResourceApi = clouderaManagerApiFactory.getHostsResourceApi(client);
            Optional<CloudInstanceType> cloudInstanceType = hostsResourceApi
                    .readHosts(null, null, DataView.SUMMARY.name()).getItems().stream()
                    .filter(apiHost -> hostGroupFqdns.contains(apiHost.getHostname()))
                    .findFirst()
                    .map(apiHost -> new CloudInstanceType(
                            hostGroup,
                            apiHost.getNumCores().intValue(),
                            ClusterUtils.memoryBytesToMB(apiHost.getTotalPhysMemBytes().longValue())));
            return cloudInstanceType;
        } catch (Exception ex) {
            LOGGER.info("Failed to retrieve CloudVMType for host group '{}', cluster '{}'. Original exception: {}", hostGroup, cluster.getStackCrn(), ex);
            return Optional.empty();
        }
    }
}
