package ru.tishembitov.pictorium.pin;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.tishembitov.pictorium.analytic.SearchAnalyticsService;
import ru.tishembitov.pictorium.exception.ResourceNotFoundException;
import ru.tishembitov.pictorium.exception.SearchException;
import ru.tishembitov.pictorium.personalization.PersonalizationBoosts;
import ru.tishembitov.pictorium.personalization.PersonalizationService;
import ru.tishembitov.pictorium.search.SearchCriteria;
import ru.tishembitov.pictorium.search.SearchResult;
import ru.tishembitov.pictorium.search.SearchResultBuilder;
import ru.tishembitov.pictorium.util.SecurityUtils;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PinSearchServiceImpl implements PinSearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final PinSearchRepository pinSearchRepository;
    private final PinSearchMapper pinMapper;
    private final SearchResultBuilder resultBuilder;
    private final PersonalizationService personalizationService;
    private final SearchAnalyticsService analyticsService;

    @Value("${index.pins.name:pins}")
    private String pinsIndex;

    @Value("${search.highlight.pre-tag:<em>}")
    private String highlightPreTag;

    @Value("${search.highlight.post-tag:</em>}")
    private String highlightPostTag;

    @Value("${search.fuzzy.max-expansions:50}")
    private int fuzzyMaxExpansions;

    @Value("${search.fuzzy.prefix-length:2}")
    private int fuzzyPrefixLength;

    @Override
    public SearchResult<PinSearchResult> searchPins(SearchCriteria criteria) {
        String userId = SecurityUtils.getCurrentUserId().orElse(null);
        return searchPins(criteria, userId);
    }

    @Override
    public SearchResult<PinSearchResult> searchPins(SearchCriteria criteria, String userId) {
        long startTime = System.currentTimeMillis();

        try {
            PersonalizationBoosts boosts = getBoostsIfEnabled(criteria, userId);

            InternalSearchResult<PinSearchResult> result = searchPinsInternal(
                    criteria,
                    criteria.getPage() * criteria.getSize(),
                    criteria.getSize(),
                    boosts
            );

            long took = System.currentTimeMillis() - startTime;

            log.info("Pin search completed: query='{}', hits={}, took={}ms, personalized={}",
                    criteria.getQuery(), result.totalHits(), took, !boosts.isEmpty());

            analyticsService.logSearch(criteria, result.totalHits(), took, userId);

            return resultBuilder.build(
                    result.results(),
                    result.totalHits(),
                    criteria.getPage(),
                    criteria.getSize(),
                    took,
                    criteria.getQuery()
            );

        } catch (Exception e) {
            log.error("Pin search failed: query='{}'", criteria.getQuery(), e);
            throw new SearchException("Search failed: " + e.getMessage(), e);
        }
    }

    @Override
    public InternalSearchResult<PinSearchResult> searchPinsInternal(
            SearchCriteria criteria,
            int from,
            int size,
            PersonalizationBoosts boosts) {

        try {
            Query query = buildPinQueryWithBoosts(criteria, boosts);

            SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
                    .index(pinsIndex)
                    .query(query)
                    .from(from)
                    .size(size);

            addPinSorting(searchBuilder, criteria);

            if (Boolean.TRUE.equals(criteria.getHighlight())) {
                addPinHighlighting(searchBuilder);
            }

            SearchResponse<PinDocument> response = elasticsearchClient.search(
                    searchBuilder.build(),
                    PinDocument.class
            );

            List<PinSearchResult> results = pinMapper.toSearchResultList(response.hits().hits());

            long totalHits = response.hits().total() != null
                    ? response.hits().total().value()
                    : 0;

            return new InternalSearchResult<>(results, totalHits);

        } catch (IOException e) {
            throw new SearchException("Failed to search pins", e);
        }
    }

    @Override
    public SearchResult<PinSearchResult> findSimilarPins(String pinId, int limit) {
        long startTime = System.currentTimeMillis();

        PinDocument sourcePin = pinSearchRepository.findById(pinId)
                .orElseThrow(() -> new ResourceNotFoundException("Pin not found: " + pinId));

        Set<String> tags = sourcePin.getTags();

        if (tags == null || tags.isEmpty()) {
            return resultBuilder.build(
                    Collections.emptyList(), 0, 0, limit,
                    System.currentTimeMillis() - startTime, null);
        }

        try {
            Query mltQuery = MoreLikeThisQuery.of(mlt -> mlt
                    .fields("title", "description", "tags")
                    .like(l -> l.document(d -> d.index(pinsIndex).id(pinId)))
                    .minTermFreq(1)
                    .minDocFreq(1)
                    .maxQueryTerms(25)
            )._toQuery();

            Query excludeSource = BoolQuery.of(b -> b
                    .must(mltQuery)
                    .mustNot(IdsQuery.of(ids -> ids.values(pinId))._toQuery())
            )._toQuery();

            SearchResponse<PinDocument> response = elasticsearchClient.search(
                    s -> s.index(pinsIndex)
                            .query(excludeSource)
                            .size(limit),
                    PinDocument.class
            );

            List<PinSearchResult> results = pinMapper.toSearchResultList(response.hits().hits());

            long totalHits = response.hits().total() != null
                    ? response.hits().total().value()
                    : 0;

            long took = System.currentTimeMillis() - startTime;

            log.info("Similar pins found: sourceId={}, results={}, took={}ms",
                    pinId, results.size(), took);

            return resultBuilder.build(results, totalHits, 0, limit, took, null);

        } catch (IOException e) {
            log.error("Failed to find similar pins: pinId={}", pinId, e);
            throw new SearchException("Failed to find similar pins", e);
        }
    }

    // === Private methods ===

    private Query buildPinQueryWithBoosts(SearchCriteria criteria, PersonalizationBoosts boosts) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        if (criteria.getQuery() != null && !criteria.getQuery().isBlank()) {
            Query textQuery = buildTextQuery(
                    criteria.getQuery(),
                    criteria.getFuzzy(),
                    List.of("title^3", "description^1", "tags^2", "authorUsername^1")
            );
            boolBuilder.must(textQuery);
        }

        addFilters(boolBuilder, criteria);

        if (!boosts.isEmpty()) {
            for (Map.Entry<String, Float> entry : boosts.getTagBoosts().entrySet()) {
                boolBuilder.should(TermQuery.of(t -> t
                        .field("tags")
                        .value(entry.getKey())
                        .boost(entry.getValue())
                )._toQuery());
            }

            for (Map.Entry<String, Float> entry : boosts.getAuthorBoosts().entrySet()) {
                boolBuilder.should(TermQuery.of(t -> t
                        .field("authorId")
                        .value(entry.getKey())
                        .boost(entry.getValue())
                )._toQuery());
            }
        }

        return boolBuilder.build()._toQuery();
    }

    private Query buildTextQuery(String query, Boolean fuzzy, List<String> fields) {
        if (Boolean.TRUE.equals(fuzzy)) {
            return MultiMatchQuery.of(m -> m
                    .query(query)
                    .fields(fields)
                    .fuzziness("AUTO")
                    .prefixLength(fuzzyPrefixLength)
                    .maxExpansions(fuzzyMaxExpansions)
                    .type(TextQueryType.BestFields)
            )._toQuery();
        } else {
            return MultiMatchQuery.of(m -> m
                    .query(query)
                    .fields(fields)
                    .type(TextQueryType.BestFields)
            )._toQuery();
        }
    }

    private void addFilters(BoolQuery.Builder boolBuilder, SearchCriteria criteria) {
        if (criteria.getTags() != null && !criteria.getTags().isEmpty()) {
            for (String tag : criteria.getTags()) {
                boolBuilder.filter(TermQuery.of(t -> t
                        .field("tags")
                        .value(tag.toLowerCase())
                )._toQuery());
            }
        }

        if (criteria.getAuthorId() != null) {
            boolBuilder.filter(TermQuery.of(t -> t
                    .field("authorId")
                    .value(criteria.getAuthorId())
            )._toQuery());
        }

        if (criteria.getCreatedFrom() != null) {
            boolBuilder.filter(RangeQuery.of(r -> r
                    .date(d -> d.field("createdAt").gte(criteria.getCreatedFrom().toString()))
            )._toQuery());
        }

        if (criteria.getCreatedTo() != null) {
            boolBuilder.filter(RangeQuery.of(r -> r
                    .date(d -> d.field("createdAt").lte(criteria.getCreatedTo().toString()))
            )._toQuery());
        }
    }

    private void addPinSorting(SearchRequest.Builder builder, SearchCriteria criteria) {
        SortOrder order = criteria.getSortOrder() == SearchCriteria.SortOrder.ASC
                ? SortOrder.Asc : SortOrder.Desc;

        switch (criteria.getSortBy()) {
            case RECENT -> builder.sort(s -> s.field(f -> f.field("createdAt").order(order)));
            case POPULAR -> builder.sort(s -> s.field(f -> f.field("viewCount").order(order)));
            case LIKES -> builder.sort(s -> s.field(f -> f.field("likeCount").order(order)));
            case SAVES -> builder.sort(s -> s.field(f -> f.field("saveCount").order(order)));
            case RELEVANCE -> builder.sort(s -> s.score(sc -> sc.order(order)));
        }
    }

    private void addPinHighlighting(SearchRequest.Builder builder) {
        builder.highlight(h -> h
                .preTags(highlightPreTag)
                .postTags(highlightPostTag)
                .fields("title", HighlightField.of(hf -> hf.numberOfFragments(0)))
                .fields("description", HighlightField.of(hf -> hf
                        .numberOfFragments(2)
                        .fragmentSize(150)))
        );
    }

    private PersonalizationBoosts getBoostsIfEnabled(SearchCriteria criteria, String userId) {
        if (Boolean.FALSE.equals(criteria.getPersonalized()) || userId == null) {
            return PersonalizationBoosts.empty();
        }
        return personalizationService.getBoostsForUser(userId);
    }
}