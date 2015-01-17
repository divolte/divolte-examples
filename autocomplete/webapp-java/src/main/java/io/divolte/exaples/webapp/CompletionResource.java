package io.divolte.exaples.webapp;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@Path("/api/complete")
@Produces(MediaType.APPLICATION_JSON)
public class CompletionResource {
    private final Client esClient;

    public CompletionResource(Client esClient) {
        this.esClient = esClient;
    }

    @GET
    public void complete(@QueryParam("q") final String q, @Suspended final AsyncResponse response) {
        esClient.prepareSuggest("suggestion").addSuggestion(
                new CompletionSuggestionBuilder("suggest")
                    .text(q)
                    .field("suggest")
                ).execute(actionListener(
                        (sr) -> response.resume(new CompletionResponse(sr)),
                        (ex) -> response.resume(ex)
                        ));
    }

    public static final class CompletionResponse {
        @JsonProperty(required = true)
        public final List<CompletionOption> searches;

        @JsonProperty(value = "top_hits", required = true)
        public final CompletionOption[] topHits;

        public CompletionResponse(SuggestResponse response) {
            /*
             * We request only one suggestion, so it's safe to take all responses
             * and flatMap out the options of the entries into a list.
             */
            this.searches = StreamSupport
                    .stream(response.getSuggest().spliterator(), false)
                    .flatMap((s) -> s.getEntries().stream())
                    .flatMap((e) -> e.getOptions().stream())
                    .map((o) -> new CompletionOption(o.getText().string(), null))
                    .collect(Collectors.toList());

            this.topHits = new CompletionOption[] {
                    new CompletionOption("BooleanUtils", "http://www.google.com/"),
                    new CompletionOption("StringUtils", "http://www.google.com/")
            };
        }

        @JsonInclude(Include.NON_NULL)
        public static final class CompletionOption {
            @JsonProperty(value = "value", required = true)
            public final String value;

            @JsonProperty("link")
            public final String link;

            public CompletionOption(final String value, final String link) {
                this.value = value;
                this.link = link;
            }
        }
    }

    private ActionListener<SuggestResponse> actionListener(final Consumer<SuggestResponse> onSuccess, final Consumer<Throwable> onFailure) {
        return new ActionListener<SuggestResponse>() {
            @Override
            public void onResponse(SuggestResponse response) {
                onSuccess.accept(response);
            }

            @Override
            public void onFailure(Throwable e) {
                onFailure.accept(e);
            }
        };
    }
}

