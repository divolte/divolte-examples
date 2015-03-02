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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion.Entry.Option;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@Path("/api/complete")
@Produces(MediaType.APPLICATION_JSON)
public class CompletionResource {
    private final Client esClient;

    public CompletionResource(Client esClient) {
        this.esClient = esClient;
    }

    @GET
    public void complete(@QueryParam("q") final String q, @Suspended final AsyncResponse response) {
        completionRequest(esClient, q).execute(actionListener(
                        (suggestionResponse) -> response.resume(new CompletionResponse(suggestionResponse)),
                        (exception) -> response.resume(exception)
                        ));
    }

    public static SuggestRequestBuilder completionRequest(final Client esClient, final String q) {
        return esClient.prepareSuggest("suggestion").addSuggestion(
                new CompletionSuggestionBuilder("suggest")
                    .text(q)
                    .field("suggest")
                );
    }

    public static final class CompletionResponse {
        @JsonProperty(required = true)
        public final List<CompletionOption> searches;

        @JsonProperty(value = "top_hits", required = true)
        public final List<CompletionOption> topHits;

        @SuppressWarnings("unchecked")
        public CompletionResponse(SuggestResponse response) {
            /*
             * We request only one suggestion, so it's safe to take all responses
             * and flatMap out the options of the entries into a list.
             */
            this.searches = StreamSupport
                    .stream(response.getSuggest().spliterator(), false)
                    .flatMap((suggestions) -> suggestions.getEntries().stream())
                    .flatMap((entries) -> entries.getOptions().stream())
                    .map((option) -> new CompletionOption(option.getText().string(), null))
                    .collect(Collectors.toList());

            this.topHits = StreamSupport
                    .stream(response.getSuggest().spliterator(), false)
                    .map((s) -> (CompletionSuggestion) s)
                    .flatMap((suggestions) -> suggestions.getEntries().stream())
                    // we need to explicitly state the type here, because the signature uses a supertype that doesn't have a getPayloadAsMap()
                    .<Option>flatMap((entries) -> entries.getOptions().stream())
                    .findFirst()
                    // Casting required; it's Map's all the way down
                    .map((o) ->
                        ((List<Map<String,String>>) o.getPayloadAsMap().get("top_hits"))
                        .stream()
                        .map((hit) -> new CompletionOption(hit.get("name"), hit.get("link")))
                        .collect(Collectors.toList())
                    )
                    .orElseGet(Collections::emptyList);
        }

        @ParametersAreNonnullByDefault
        @JsonInclude(Include.NON_NULL)
        public static final class CompletionOption {
            @JsonProperty(value = "value", required = true)
            public final String value;

            @Nullable
            @JsonProperty("link")
            public final String link;

            public CompletionOption(final String value, @Nullable final String link) {
                this.value = value;
                this.link = link;
            }
        }
    }

    public static ActionListener<SuggestResponse> actionListener(final Consumer<SuggestResponse> onSuccess, final Consumer<Throwable> onFailure) {
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

