package ru.tishembitov.pictorium.analytic;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchAnalyticsScheduler {

    private final TrendingQueryRepository trendingRepository;
    private final ElasticsearchClient elasticsearchClient;

    @Value("${index.search-analytics.name:search_analytics}")
    private String analyticsIndex;

    @Scheduled(cron = "0 0 * * * *") // Каждый час
    public void recalculateTrendingScores() {
        log.info("Recalculating trending scores...");

        Instant now = Instant.now();
        Instant hourAgo = now.minus(1, ChronoUnit.HOURS);

        try {
            var hourlyResponse = elasticsearchClient.search(s -> s
                            .index(analyticsIndex)
                            .size(0)
                            .query(q -> q.range(r -> r
                                    .date(d -> d
                                            .field("timestamp")
                                            .gte(hourAgo.toString())
                                    )
                            ))
                            .aggregations("queries", a -> a
                                    .terms(t -> t.field("normalizedQuery").size(100))
                                    .aggregations("unique_users", sub -> sub
                                            .cardinality(c -> c.field("userId"))
                                    )
                            ),
                    SearchAnalyticsDocument.class
            );

            var queriesAgg = hourlyResponse.aggregations().get("queries");
            if (queriesAgg != null && queriesAgg.isSterms()) {
                for (StringTermsBucket bucket : queriesAgg.sterms().buckets().array()) {
                    String query = bucket.key().stringValue();
                    long hourlyCount = bucket.docCount();
                    long uniqueUsers = bucket.aggregations().get("unique_users").cardinality().value();

                    double score = hourlyCount * Math.log(uniqueUsers + 1) * 2.0;

                    trendingRepository.findByNormalizedQuery(query)
                            .ifPresent(doc -> {
                                doc.setTrendingScore(score);
                                doc.setLastUpdated(now);
                                trendingRepository.save(doc);
                            });
                }
            }

            log.info("Trending scores recalculated");

        } catch (IOException e) {
            log.error("Failed to recalculate trending scores", e);
        }
    }

    @Scheduled(cron = "0 0 3 * * *") // Каждый день в 3:00
    public void cleanupOldAnalytics() {
        Instant threshold = Instant.now().minus(30, ChronoUnit.DAYS);

        try {
            elasticsearchClient.deleteByQuery(d -> d
                    .index(analyticsIndex)
                    .query(q -> q.range(r -> r
                            .date(dt -> dt
                                    .field("timestamp")
                                    .lt(threshold.toString())
                            )
                    ))
            );

            log.info("Old analytics cleaned up (before {})", threshold);

        } catch (IOException e) {
            log.error("Failed to cleanup old analytics", e);
        }
    }
}