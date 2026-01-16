package ru.tishembitov.pictorium.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import ru.tishembitov.pictorium.document.UserInterestDocument;

@Repository
public interface UserInterestRepository extends ElasticsearchRepository<UserInterestDocument, String> {
}