package ru.tishembitov.pictorium.tag;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface TagService {

    Set<Tag> resolveTagsByNames(Set<String> tagNames);

    Page<TagResponse> findAll(Pageable pageable);

    List<TagResponse> findByPinId(UUID pinId);

    TagResponse findById(UUID tagId);

    List<CategoryResponse> getCategories(int limit);

    List<TagResponse> searchByName(String q, int limit);

}
