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

import static org.elasticsearch.common.settings.ImmutableSettings.*;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration.Dynamic;

import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

public class Webapp extends Application<WebappConfiguration>{

    @Override
    public void initialize(final Bootstrap<WebappConfiguration> bootstrap) {}

    @Override
    public void run(final WebappConfiguration configuration, final Environment environment) throws Exception {
        enableCrossOriginResourceSharing(environment);

        final TransportClient client = setupElasticSearchClient(configuration);
        registerHealthCheck(environment, client);
        registerAutocompleteResource(environment, client);
    }

    private void registerHealthCheck(final Environment environment, final TransportClient client) {
        final CompletionHealthCheck check = new CompletionHealthCheck(client);
        environment.healthChecks().register("completion", check);
    }

    private void registerAutocompleteResource(final Environment environment, final TransportClient client) {
        final CompletionResource completion = new CompletionResource(client);
        environment.jersey().register(completion);
    }

    private void enableCrossOriginResourceSharing(final Environment environment) {
        Dynamic filter = environment.servlets().addFilter("CORS", CrossOriginFilter.class);
        filter.setInitParameter("allowedOrigins", "*"); // allowed origins comma separated
        filter.setInitParameter("allowedHeaders", "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin");
        filter.setInitParameter("allowedMethods", "GET,PUT,POST,DELETE,OPTIONS,HEAD");
        filter.setInitParameter("preflightMaxAge", "5184000"); // 2 months
        filter.setInitParameter("allowCredentials", "true");
        filter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
    }

    private TransportClient setupElasticSearchClient(final WebappConfiguration configuration) {
        final Settings esSettings = settingsBuilder().put("cluster.name", configuration.esClusterName).build();
        final TransportClient client = new TransportClient(esSettings);
        configuration.esHosts.forEach((host) ->  client.addTransportAddress(new InetSocketTransportAddress(host, configuration.esPort)));
        return client;
    }

    public static void main(String[] args) throws Exception {
        new Webapp().run(args);
    }
}
