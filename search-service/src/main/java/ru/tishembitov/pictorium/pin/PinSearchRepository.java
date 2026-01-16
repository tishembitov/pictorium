package ru.tishembitov.pictorium.pin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository
public interface PinSearchRepository extends ElasticsearchRepository<PinDocument, String> {

    Page<PinDocument> findByAuthorId(String authorId, Pageable pageable);

    Page<PinDocument> findByTagsContaining(String tag, Pageable pageable);

    @Query("""
        {
          "bool": {
            "should": [
              { "match": { "title": { "query": "?0", "boost": 3 } } },
              { "match": { "description": { "query": "?0", "boost": 1 } } },
              { "match": { "tags.text": { "query": "?0", "boost": 2 } } },
              { "match": { "authorUsername": { "query": "?0", "boost": 1 } } }
            ],
            "minimum_should_match": 1
          }
        }
        """)
    Page<PinDocument> searchByQuery(String query, Pageable pageable);

    List<PinDocument> findByTagsIn(Collection<Set<String>> tags);
}