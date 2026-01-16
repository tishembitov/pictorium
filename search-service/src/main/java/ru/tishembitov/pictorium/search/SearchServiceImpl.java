package ru.tishembitov.pictorium.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.tishembitov.pictorium.analytics.SearchAnalyticsService;
import ru.tishembitov.pictorium.document.BoardDocument;
import ru.tishembitov.pictorium.document.PinDocument;
import ru.tishembitov.pictorium.document.UserDocument;
import ru.tishembitov.pictorium.exception.ResourceNotFoundException;
import ru.tishembitov.pictorium.exception.SearchException;
import ru.tishembitov.pictorium.personalization.PersonalizationBoosts;
import ru.tishembitov.pictorium.personalization.PersonalizationService;
import ru.tishembitov.pictorium.repository.PinSearchRepository;
import ru.tishembitov.pictorium.search.dto.*;
import ru.tishembitov.pictorium.search.mapper.SearchMapper;
import ru.tishembitov.pictorium.util.SecurityUtils;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final PinSearchRepository pinSearchRepository;
    private final SearchMapper searchMapper;
    private final PersonalizationService personalizationService;
    private final SearchAnalyticsService analyticsService;

    @Value("${index.pins.name:pins}")
    private String pinsIndex;

    @Value("${index.users.name:users}")
    private String usersIndex;

    @Value("${index.boards.name:boards}")
    private String boardsIndex;

    @Value("${search.highlight.pre-tag:<em>}")
    private String highlightPreTag;

    @Value("${search.highlight.post-tag:</em>}")
    private String highlightPostTag;

    @Value("${search.fuzzy.max-expansions:50}")
    private int fuzzyMaxExpansions;

    @Value("${search.fuzzy.prefix-length:2}")
    private int fuzzyPrefixLength;

    // ======================== Универсальный поиск ========================

    @Override
    public UniversalSearchResponse searchAll(ru.tishembitov.pictorium.search.dto.SearchRequest request) {
        long startTime = System.currentTimeMillis();

        int limitPerType = Math.min(request.getSize(), 10);

        try {
            String userId = SecurityUtils.getCurrentUserId().orElse(null);
            PersonalizationBoosts boosts = getBoostsIfEnabled(request, userId);

            var pinsFuture = searchPinsInternal(request, 0, limitPerType, boosts);
            var usersFuture = searchUsersInternal(request, 0, limitPerType);
            var boardsFuture = searchBoardsInternal(request, 0, limitPerType);

            long took = System.currentTimeMillis() - startTime;

            // Логируем для аналитики
            long totalResults = pinsFuture.totalHits() + usersFuture.totalHits() + boardsFuture.totalHits();
            analyticsService.logSearch(request, totalResults, took, userId);

            log.info("Universal search completed: query='{}', took={}ms", request.getQuery(), took);

            return UniversalSearchResponse.builder()
                    .pins(pinsFuture.results())
                    .users(usersFuture.results())
                    .boards(boardsFuture.results())
                    .totalPins(pinsFuture.totalHits())
                    .totalUsers(usersFuture.totalHits())
                    .totalBoards(boardsFuture.totalHits())
                    .hasMorePins(pinsFuture.totalHits() > limitPerType)
                    .hasMoreUsers(usersFuture.totalHits() > limitPerType)
                    .hasMoreBoards(boardsFuture.totalHits() > limitPerType)
                    .took(took)
                    .query(request.getQuery())
                    .build();

        } catch (Exception e) {
            log.error("Universal search failed: query='{}'", request.getQuery(), e);
            throw new SearchException("Search failed: " + e.getMessage(), e);
        }
    }

    // ======================== Поиск пинов ========================

    @Override
    public ru.tishembitov.pictorium.search.dto.SearchResponse<PinSearchResult> searchPins(
            ru.tishembitov.pictorium.search.dto.SearchRequest request) {

        long startTime = System.currentTimeMillis();

        try {
            String userId = SecurityUtils.getCurrentUserId().orElse(null);
            PersonalizationBoosts boosts = getBoostsIfEnabled(request, userId);

            SearchResult<PinSearchResult> result = searchPinsInternal(
                    request,
                    request.getPage() * request.getSize(),
                    request.getSize(),
                    boosts
            );

            long took = System.currentTimeMillis() - startTime;

            log.info("Pin search completed: query='{}', hits={}, took={}ms, personalized={}",
                    request.getQuery(), result.totalHits(), took, !boosts.isEmpty());

            // Логируем для аналитики
            analyticsService.logSearch(request, result.totalHits(), took, userId);

            return searchMapper.toSearchResponse(
                    result.results(),
                    result.totalHits(),
                    request.getPage(),
                    request.getSize(),
                    took,
                    request.getQuery()
            );

        } catch (Exception e) {
            log.error("Pin search failed: query='{}'", request.getQuery(), e);
            throw new SearchException("Search failed: " + e.getMessage(), e);
        }
    }

    private SearchResult<PinSearchResult> searchPinsInternal(
            ru.tishembitov.pictorium.search.dto.SearchRequest request,
            int from,
            int size,
            PersonalizationBoosts boosts) throws IOException {

        Query query = buildPinQueryWithBoosts(request, boosts);

        SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
                .index(pinsIndex)
                .query(query)
                .from(from)
                .size(size);

        addPinSorting(searchBuilder, request);

        if (Boolean.TRUE.equals(request.getHighlight())) {
            addPinHighlighting(searchBuilder);
        }

        SearchResponse<PinDocument> response = elasticsearchClient.search(
                searchBuilder.build(),
                PinDocument.class
        );

        List<PinSearchResult> results = response.hits().hits().stream()
                .map(this::mapPinHitToResult)
                .filter(Objects::nonNull)
                .toList();

        long totalHits = response.hits().total() != null
                ? response.hits().total().value()
                : 0;

        return new SearchResult<>(results, totalHits);
    }

    // ======================== Поиск пользователей ========================

    @Override
    public ru.tishembitov.pictorium.search.dto.SearchResponse<UserSearchResult> searchUsers(
            ru.tishembitov.pictorium.search.dto.SearchRequest request) {

        long startTime = System.currentTimeMillis();

        try {
            SearchResult<UserSearchResult> result = searchUsersInternal(
                    request, request.getPage() * request.getSize(), request.getSize());

            long took = System.currentTimeMillis() - startTime;

            log.info("User search completed: query='{}', hits={}, took={}ms",
                    request.getQuery(), result.totalHits(), took);

            String userId = SecurityUtils.getCurrentUserId().orElse(null);
            analyticsService.logSearch(request, result.totalHits(), took, userId);

            return searchMapper.toSearchResponse(
                    result.results(),
                    result.totalHits(),
                    request.getPage(),
                    request.getSize(),
                    took,
                    request.getQuery()
            );

        } catch (Exception e) {
            log.error("User search failed: query='{}'", request.getQuery(), e);
            throw new SearchException("Search failed: " + e.getMessage(), e);
        }
    }

    private SearchResult<UserSearchResult> searchUsersInternal(
            ru.tishembitov.pictorium.search.dto.SearchRequest request, int from, int size) throws IOException {

        Query query = buildUserQuery(request);

        SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
                .index(usersIndex)
                .query(query)
                .from(from)
                .size(size);

        // Сортировка для пользователей
        switch (request.getSortBy()) {
            case POPULAR -> searchBuilder.sort(s -> s.field(f -> f
                    .field("followerCount")
                    .order(SortOrder.Desc)));
            case RECENT -> searchBuilder.sort(s -> s.field(f -> f
                    .field("createdAt")
                    .order(SortOrder.Desc)));
            default -> searchBuilder.sort(s -> s.score(sc -> sc.order(SortOrder.Desc)));
        }

        if (Boolean.TRUE.equals(request.getHighlight())) {
            searchBuilder.highlight(h -> h
                    .preTags(highlightPreTag)
                    .postTags(highlightPostTag)
                    .fields("username", HighlightField.of(hf -> hf.numberOfFragments(0)))
                    .fields("description", HighlightField.of(hf -> hf
                            .numberOfFragments(2)
                            .fragmentSize(150)))
            );
        }

        SearchResponse<UserDocument> response = elasticsearchClient.search(
                searchBuilder.build(),
                UserDocument.class
        );

        List<UserSearchResult> results = response.hits().hits().stream()
                .map(this::mapUserHitToResult)
                .filter(Objects::nonNull)
                .toList();

        long totalHits = response.hits().total() != null
                ? response.hits().total().value()
                : 0;

        return new SearchResult<>(results, totalHits);
    }

    // ======================== Поиск досок ========================

    @Override
    public ru.tishembitov.pictorium.search.dto.SearchResponse<BoardSearchResult> searchBoards(
            ru.tishembitov.pictorium.search.dto.SearchRequest request) {

        long startTime = System.currentTimeMillis();

        try {
            SearchResult<BoardSearchResult> result = searchBoardsInternal(
                    request, request.getPage() * request.getSize(), request.getSize());

            long took = System.currentTimeMillis() - startTime;

            log.info("Board search completed: query='{}', hits={}, took={}ms",
                    request.getQuery(), result.totalHits(), took);

            String userId = SecurityUtils.getCurrentUserId().orElse(null);
            analyticsService.logSearch(request, result.totalHits(), took, userId);

            return searchMapper.toSearchResponse(
                    result.results(),
                    result.totalHits(),
                    request.getPage(),
                    request.getSize(),
                    took,
                    request.getQuery()
            );

        } catch (Exception e) {
            log.error("Board search failed: query='{}'", request.getQuery(), e);
            throw new SearchException("Search failed: " + e.getMessage(), e);
        }
    }

    private SearchResult<BoardSearchResult> searchBoardsInternal(
            ru.tishembitov.pictorium.search.dto.SearchRequest request, int from, int size) throws IOException {

        Query query = buildBoardQuery(request);

        SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
                .index(boardsIndex)
                .query(query)
                .from(from)
                .size(size);

        // Сортировка для досок
        switch (request.getSortBy()) {
            case POPULAR -> searchBuilder.sort(s -> s.field(f -> f
                    .field("pinCount")
                    .order(SortOrder.Desc)));
            case RECENT -> searchBuilder.sort(s -> s.field(f -> f
                    .field("createdAt")
                    .order(SortOrder.Desc)));
            default -> searchBuilder.sort(s -> s.score(sc -> sc.order(SortOrder.Desc)));
        }

        if (Boolean.TRUE.equals(request.getHighlight())) {
            searchBuilder.highlight(h -> h
                    .preTags(highlightPreTag)
                    .postTags(highlightPostTag)
                    .fields("title", HighlightField.of(hf -> hf.numberOfFragments(0)))
            );
        }

        SearchResponse<BoardDocument> response = elasticsearchClient.search(
                searchBuilder.build(),
                BoardDocument.class
        );

        List<BoardSearchResult> results = response.hits().hits().stream()
                .map(this::mapBoardHitToResult)
                .filter(Objects::nonNull)
                .toList();

        long totalHits = response.hits().total() != null
                ? response.hits().total().value()
                : 0;

        return new SearchResult<>(results, totalHits);
    }

    // ======================== Похожие пины ========================

    @Override
    public ru.tishembitov.pictorium.search.dto.SearchResponse<PinSearchResult> findSimilarPins(
            String pinId, int limit) {

        long startTime = System.currentTimeMillis();

        PinDocument sourcePin = pinSearchRepository.findById(pinId)
                .orElseThrow(() -> new ResourceNotFoundException("Pin not found: " + pinId));

        Set<String> tags = sourcePin.getTags();

        if (tags == null || tags.isEmpty()) {
            return searchMapper.toSearchResponse(
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

            List<PinSearchResult> results = response.hits().hits().stream()
                    .map(this::mapPinHitToResult)
                    .filter(Objects::nonNull)
                    .toList();

            long totalHits = response.hits().total() != null
                    ? response.hits().total().value()
                    : 0;

            long took = System.currentTimeMillis() - startTime;

            log.info("Similar pins found: sourceId={}, results={}, took={}ms",
                    pinId, results.size(), took);

            return searchMapper.toSearchResponse(results, totalHits, 0, limit, took, null);

        } catch (IOException e) {
            log.error("Failed to find similar pins: pinId={}", pinId, e);
            throw new SearchException("Failed to find similar pins", e);
        }
    }

    // ======================== Автодополнение ========================

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

            // Поиск по пинам
            addPinSuggestions(query, suggestions, seen, limit);

            // Поиск по пользователям
            addUserSuggestions(query, suggestions, seen, limit);

            // Сортируем по score
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

            // Заголовки
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

            // Теги
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

    // ======================== Trending ========================

    @Override
    public List<String> getTrendingSearches(int limit) {
        try {
            SearchResponse<PinDocument> response = elasticsearchClient.search(
                    s -> s.index(pinsIndex)
                            .size(0)
                            .aggregations("top_tags", a -> a
                                    .terms(t -> t.field("tags").size(limit))),
                    PinDocument.class
            );

            var tagsAgg = response.aggregations().get("top_tags");
            if (tagsAgg != null && tagsAgg.isSterms()) {
                return tagsAgg.sterms().buckets().array().stream()
                        .map(b -> b.key().stringValue())
                        .toList();
            }

            return Collections.emptyList();

        } catch (IOException e) {
            log.error("Failed to get trending searches", e);
            return Collections.emptyList();
        }
    }

    // ======================== Query Builders ========================

    private Query buildPinQueryWithBoosts(
            ru.tishembitov.pictorium.search.dto.SearchRequest request,
            PersonalizationBoosts boosts) {

        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // Основной запрос
        if (request.getQuery() != null && !request.getQuery().isBlank()) {
            Query textQuery = buildTextQuery(
                    request.getQuery(),
                    request.getFuzzy(),
                    List.of("title^3", "description^1", "tags^2", "authorUsername^1")
            );
            boolBuilder.must(textQuery);
        }

        // Фильтры
        addFilters(boolBuilder, request);

        // Персонализационные бусты
        if (!boosts.isEmpty()) {
            List<Query> shouldClauses = new ArrayList<>();

            // Буст по тегам
            for (Map.Entry<String, Float> entry : boosts.getTagBoosts().entrySet()) {
                String tagValue = entry.getKey();
                Float boostValue = entry.getValue();
                shouldClauses.add(TermQuery.of(t -> t
                        .field("tags")
                        .value(tagValue)
                        .boost(boostValue)
                )._toQuery());
            }

            // Буст по авторам
            for (Map.Entry<String, Float> entry : boosts.getAuthorBoosts().entrySet()) {
                String authorValue = entry.getKey();
                Float boostValue = entry.getValue();
                shouldClauses.add(TermQuery.of(t -> t
                        .field("authorId")
                        .value(authorValue)
                        .boost(boostValue)
                )._toQuery());
            }

            if (!shouldClauses.isEmpty()) {
                for (Query shouldQuery : shouldClauses) {
                    boolBuilder.should(shouldQuery);
                }
            }
        }

        return boolBuilder.build()._toQuery();
    }

    private Query buildUserQuery(ru.tishembitov.pictorium.search.dto.SearchRequest request) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        if (request.getQuery() != null && !request.getQuery().isBlank()) {
            Query textQuery = buildTextQuery(
                    request.getQuery(),
                    request.getFuzzy(),
                    List.of("username^3", "description^1")
            );
            boolBuilder.must(textQuery);
        }

        return boolBuilder.build()._toQuery();
    }

    private Query buildBoardQuery(ru.tishembitov.pictorium.search.dto.SearchRequest request) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        if (request.getQuery() != null && !request.getQuery().isBlank()) {
            Query textQuery = buildTextQuery(
                    request.getQuery(),
                    request.getFuzzy(),
                    List.of("title^3", "username^1")
            );
            boolBuilder.must(textQuery);
        }

        if (request.getAuthorId() != null) {
            boolBuilder.filter(TermQuery.of(t -> t
                    .field("userId")
                    .value(request.getAuthorId())
            )._toQuery());
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

    private void addFilters(BoolQuery.Builder boolBuilder,
                            ru.tishembitov.pictorium.search.dto.SearchRequest request) {
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            for (String tag : request.getTags()) {
                boolBuilder.filter(TermQuery.of(t -> t
                        .field("tags")
                        .value(tag.toLowerCase())
                )._toQuery());
            }
        }

        if (request.getAuthorId() != null) {
            boolBuilder.filter(TermQuery.of(t -> t
                    .field("authorId")
                    .value(request.getAuthorId())
            )._toQuery());
        }

        addDateRangeFilter(boolBuilder, request);
    }

    private void addDateRangeFilter(BoolQuery.Builder boolBuilder,
                                    ru.tishembitov.pictorium.search.dto.SearchRequest request) {
        if (request.getCreatedFrom() != null) {
            boolBuilder.filter(RangeQuery.of(r -> r
                    .date(d -> d
                            .field("createdAt")
                            .gte(request.getCreatedFrom().toString())
                    )
            )._toQuery());
        }

        if (request.getCreatedTo() != null) {
            boolBuilder.filter(RangeQuery.of(r -> r
                    .date(d -> d
                            .field("createdAt")
                            .lte(request.getCreatedTo().toString())
                    )
            )._toQuery());
        }
    }

    private void addPinSorting(SearchRequest.Builder builder,
                               ru.tishembitov.pictorium.search.dto.SearchRequest request) {

        SortOrder order = request.getSortOrder() ==
                ru.tishembitov.pictorium.search.dto.SearchRequest.SortOrder.ASC
                ? SortOrder.Asc : SortOrder.Desc;

        switch (request.getSortBy()) {
            case RECENT -> builder.sort(s -> s.field(f -> f
                    .field("createdAt")
                    .order(order)));

            case POPULAR -> builder.sort(s -> s.field(f -> f
                    .field("viewCount")
                    .order(order)));

            case LIKES -> builder.sort(s -> s.field(f -> f
                    .field("likeCount")
                    .order(order)));

            case SAVES -> builder.sort(s -> s.field(f -> f
                    .field("saveCount")
                    .order(order)));

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

    // ======================== Helper Methods ========================

    private PersonalizationBoosts getBoostsIfEnabled(
            ru.tishembitov.pictorium.search.dto.SearchRequest request,
            String userId) {
        if (Boolean.FALSE.equals(request.getPersonalized()) || userId == null) {
            return PersonalizationBoosts.empty();
        }
        return personalizationService.getBoostsForUser(userId);
    }

    // ======================== Mappers ========================

    private PinSearchResult mapPinHitToResult(Hit<PinDocument> hit) {
        PinDocument doc = hit.source();
        if (doc == null) return null;

        Map<String, List<String>> highlights = new HashMap<>();
        if (hit.highlight() != null) {
            highlights.putAll(hit.highlight());
        }

        return PinSearchResult.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .description(doc.getDescription())
                .tags(doc.getTags())
                .authorId(doc.getAuthorId())
                .authorUsername(doc.getAuthorUsername())
                .imageId(doc.getImageId())
                .thumbnailId(doc.getThumbnailId())
                .likeCount(doc.getLikeCount())
                .saveCount(doc.getSaveCount())
                .commentCount(doc.getCommentCount())
                .originalWidth(doc.getOriginalWidth())
                .originalHeight(doc.getOriginalHeight())
                .createdAt(doc.getCreatedAt())
                .highlights(highlights)
                .score(hit.score() != null ? hit.score().floatValue() : null)
                .build();
    }

    private UserSearchResult mapUserHitToResult(Hit<UserDocument> hit) {
        UserDocument doc = hit.source();
        if (doc == null) return null;

        Map<String, List<String>> highlights = new HashMap<>();
        if (hit.highlight() != null) {
            highlights.putAll(hit.highlight());
        }

        return UserSearchResult.builder()
                .id(doc.getId())
                .username(doc.getUsername())
                .description(doc.getDescription())
                .imageId(doc.getImageId())
                .bannerImageId(doc.getBannerImageId())
                .followerCount(doc.getFollowerCount())
                .followingCount(doc.getFollowingCount())
                .pinCount(doc.getPinCount())
                .createdAt(doc.getCreatedAt())
                .highlights(highlights)
                .score(hit.score() != null ? hit.score().floatValue() : null)
                .build();
    }

    private BoardSearchResult mapBoardHitToResult(Hit<BoardDocument> hit) {
        BoardDocument doc = hit.source();
        if (doc == null) return null;

        Map<String, List<String>> highlights = new HashMap<>();
        if (hit.highlight() != null) {
            highlights.putAll(hit.highlight());
        }

        return BoardSearchResult.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .userId(doc.getUserId())
                .username(doc.getUsername())
                .pinCount(doc.getPinCount())
                .previewImageId(doc.getPreviewImageId())
                .createdAt(doc.getCreatedAt())
                .highlights(highlights)
                .score(hit.score() != null ? hit.score().floatValue() : null)
                .build();
    }

    // ======================== Helper Records ========================

    private record SearchResult<T>(List<T> results, long totalHits) {}
}