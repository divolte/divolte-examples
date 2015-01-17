package io.divolte.exaples.webapp;

import io.dropwizard.Configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class WebappConfiguration extends Configuration {
    private static final String DEFAULT_CLUSTER_NAME = "elasticsearch";
    private static final String[] DEFAULT_HOSTS = new String[] { "localhost" };
    private static final int DEFAULT_PORT = 9300;

    public final String[] esHosts;
    public final int esPort;
    public final String esClusterName;

    @JsonCreator
    private WebappConfiguration(
            @JsonProperty("elasticsearch_hosts") final String[] esHosts,
            @JsonProperty("elasticsearch_port") final int esPort,
            @JsonProperty("elasticsearch_cluster_name") final String esClusterName) {
        this.esHosts = esHosts == null ? DEFAULT_HOSTS : esHosts;
        this.esPort = esPort == 0 ? DEFAULT_PORT : esPort;
        this.esClusterName = esClusterName == null ? DEFAULT_CLUSTER_NAME : esClusterName;
    }
}
