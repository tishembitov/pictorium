package ru.tishembitov.pictorium.board;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardSearchRepository extends ElasticsearchRepository<BoardDocument, String> {

    @Query("""
        {
          "bool": {
            "should": [
              { "match": { "title": { "query": "?0", "boost": 3 } } },
              { "match": { "username": { "query": "?0", "boost": 1 } } }
            ],
            "minimum_should_match": 1
          }
        }
        """)
    Page<BoardDocument> searchByQuery(String query, Pageable pageable);

    Page<BoardDocument> findByUserId(String userId, Pageable pageable);
}