package ru.tishembitov.pictorium.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.Instant;

@Document(indexName = "#{@environment.getProperty('index.search-analytics.name', 'search_analytics')}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchAnalyticsDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String query;

    @Field(type = FieldType.Keyword)
    private String normalizedQuery;

    @Field(type = FieldType.Keyword)
    private String userId;

    @Field(type = FieldType.Keyword)
    private String searchType;

    @Field(type = FieldType.Long)
    private Long resultsCount;

    @Field(type = FieldType.Long)
    private Long took;

    @Field(type = FieldType.Boolean)
    private Boolean hasResults;

    @Field(type = FieldType.Keyword)
    private String sessionId;

    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant timestamp;
}