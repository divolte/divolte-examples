/*
 * Copyright 2015 GoDataDriven B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.divolte.exaples.webapp;

import io.dropwizard.Configuration;

import javax.annotation.ParametersAreNullableByDefault;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@ParametersAreNullableByDefault
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
