package io.divolte.exaples.webapp;

import static org.elasticsearch.common.settings.ImmutableSettings.*;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.util.EnumSet;
import java.util.stream.Stream;

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
        addCorsHeadersFilter(environment);

        final TransportClient client = setupElasticSearchClient(configuration);
        registerAutocompleteResource(environment, client);
    }

    private void registerAutocompleteResource(final Environment environment, final TransportClient client) {
        // Register our resource
        final CompletionResource completion = new CompletionResource(client);
        environment.jersey().register(completion);
    }

    private void addCorsHeadersFilter(final Environment environment) {
        // Enable CORS
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
        Stream.of(configuration.esHosts).forEach((host) ->  client.addTransportAddress(new InetSocketTransportAddress(host, configuration.esPort)));
        return client;
    }

    public static void main(String[] args) throws Exception {
        new Webapp().run(args);
    }
}
