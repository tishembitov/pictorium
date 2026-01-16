package ru.tishembitov.pictorium.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import ru.tishembitov.pictorium.document.SearchAnalyticsDocument;

@Repository
public interface SearchAnalyticsRepository extends ElasticsearchRepository<SearchAnalyticsDocument, String> {
}