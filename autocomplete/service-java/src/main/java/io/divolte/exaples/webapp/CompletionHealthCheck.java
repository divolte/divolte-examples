package io.divolte.exaples.webapp;

import static io.divolte.exaples.webapp.CompletionResource.*;

import java.util.stream.StreamSupport;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;

import com.codahale.metrics.health.HealthCheck;

public class CompletionHealthCheck extends HealthCheck {
    private static final int MAX_ES_RESPONSE_TIME = 300;
    private final Client esClient;

    public CompletionHealthCheck(final Client esClient) {
        this.esClient = esClient;
    }

    @Override
    protected Result check() throws Exception {
        try {
            final SuggestResponse response = completionRequest(esClient, "b").get(TimeValue.timeValueMillis(MAX_ES_RESPONSE_TIME));

            final long numResults = StreamSupport
            .stream(response.getSuggest().spliterator(), false)
            .flatMap((suggestions) -> suggestions.getEntries().stream())
            .flatMap((entries) -> entries.getOptions().stream())
            .count();

            if (numResults == 0) {
                return Result.unhealthy("Zero suggestions returned for single character completion request.");
            }
        } catch(ElasticsearchException ee) {
            // Also occurs in case of timeout
            return Result.unhealthy(ee.getMessage());
        }

        return Result.healthy();
    }
}
