package ru.tishembitov.pictorium.suggest;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.PrefixQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.tishembitov.pictorium.pin.PinDocument;
import ru.tishembitov.pictorium.user.UserDocument;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuggestServiceImpl implements SuggestService {

    private final ElasticsearchClient elasticsearchClient;

    @Value("${index.pins.name:pins}")
    private String pinsIndex;

    @Value("${index.users.name:users}")
    private String usersIndex;

    @Override
    public SuggestResponse suggest(String query, int limit) {
        if (query == null || query.trim().length() < 2) {
            return SuggestResponse.builder()
                    .suggestions(Collections.emptyList())
                    .build();
        }

        try {
            Set<String> seen = new HashSet<>();
            List<SuggestResponse.Suggestion> suggestions = new ArrayList<>();

            addPinSuggestions(query, suggestions, seen, limit);
            addUserSuggestions(query, suggestions, seen, limit);

            suggestions.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));

            return SuggestResponse.builder()
                    .suggestions(suggestions.stream().limit(limit).toList())
                    .build();

        } catch (IOException e) {
            log.error("Suggest failed: query='{}'", query, e);
            return SuggestResponse.builder()
                    .suggestions(Collections.emptyList())
                    .build();
        }
    }

    private void addPinSuggestions(String query, List<SuggestResponse.Suggestion> suggestions,
                                   Set<String> seen, int limit) throws IOException {
        Query prefixQuery = BoolQuery.of(b -> b
                .should(
                        PrefixQuery.of(p -> p
                                .field("title.autocomplete")
                                .value(query.toLowerCase())
                                .boost(3.0f)
                        )._toQuery(),
                        PrefixQuery.of(p -> p
                                .field("tags")
                                .value(query.toLowerCase())
                                .boost(2.0f)
                        )._toQuery()
                )
                .minimumShouldMatch("1")
        )._toQuery();

        SearchResponse<PinDocument> response = elasticsearchClient.search(
                s -> s.index(pinsIndex)
                        .query(prefixQuery)
                        .size(limit)
                        .source(src -> src.filter(f -> f
                                .includes("title", "tags", "thumbnailId"))),
                PinDocument.class
        );

        for (Hit<PinDocument> hit : response.hits().hits()) {
            PinDocument doc = hit.source();
            if (doc == null) continue;

            if (doc.getTitle() != null &&
                    doc.getTitle().toLowerCase().contains(query.toLowerCase()) &&
                    seen.add(doc.getTitle().toLowerCase())) {

                suggestions.add(SuggestResponse.Suggestion.builder()
                        .text(doc.getTitle())
                        .type(SuggestResponse.SuggestionType.PIN_TITLE)
                        .imageId(doc.getThumbnailId())
                        .score(hit.score() != null ? hit.score().floatValue() : 0f)
                        .build());
            }

            if (doc.getTags() != null) {
                for (String tag : doc.getTags()) {
                    if (tag.toLowerCase().startsWith(query.toLowerCase()) &&
                            seen.add("tag:" + tag.toLowerCase())) {

                        suggestions.add(SuggestResponse.Suggestion.builder()
                                .text(tag)
                                .type(SuggestResponse.SuggestionType.TAG)
                                .score(hit.score() != null ? hit.score().floatValue() * 0.8f : 0f)
                                .build());
                    }
                }
            }
        }
    }

    private void addUserSuggestions(String query, List<SuggestResponse.Suggestion> suggestions,
                                    Set<String> seen, int limit) throws IOException {
        Query prefixQuery = PrefixQuery.of(p -> p
                .field("username")
                .value(query.toLowerCase())
        )._toQuery();

        SearchResponse<UserDocument> response = elasticsearchClient.search(
                s -> s.index(usersIndex)
                        .query(prefixQuery)
                        .size(limit / 2)
                        .source(src -> src.filter(f -> f
                                .includes("username", "imageId"))),
                UserDocument.class
        );

        for (Hit<UserDocument> hit : response.hits().hits()) {
            UserDocument doc = hit.source();
            if (doc == null) continue;

            if (doc.getUsername() != null &&
                    seen.add("user:" + doc.getUsername().toLowerCase())) {

                suggestions.add(SuggestResponse.Suggestion.builder()
                        .text(doc.getUsername())
                        .type(SuggestResponse.SuggestionType.USERNAME)
                        .imageId(doc.getImageId())
                        .score(hit.score() != null ? hit.score().floatValue() * 0.7f : 0f)
                        .build());
            }
        }
    }
}
