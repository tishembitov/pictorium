package ru.tishembitov.pictorium.analytic;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchAnalyticsRepository extends ElasticsearchRepository<SearchAnalyticsDocument, String> {
}