package ru.tishembitov.pictorium.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import ru.tishembitov.pictorium.document.TrendingQueryDocument;

import java.util.Optional;

@Repository
public interface TrendingQueryRepository extends ElasticsearchRepository<TrendingQueryDocument, String> {

    Page<TrendingQueryDocument> findAllByOrderByTrendingScoreDesc(Pageable pageable);

    Optional<TrendingQueryDocument> findByNormalizedQuery(String normalizedQuery);
}