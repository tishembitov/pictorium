package ru.tishembitov.pictorium.analytic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.Instant;

@Document(indexName = "#{@environment.getProperty('index.trending-queries.name', 'trending_queries')}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendingQueryDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String query;

    @Field(type = FieldType.Keyword)
    private String normalizedQuery;

    @Field(type = FieldType.Long)
    @Builder.Default
    private Long searchCount = 0L;

    @Field(type = FieldType.Long)
    @Builder.Default
    private Long uniqueUsers = 0L;

    @Field(type = FieldType.Double)
    @Builder.Default
    private Double trendingScore = 0.0;

    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant lastUpdated;

    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant createdAt;
}