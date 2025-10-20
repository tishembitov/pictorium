package ru.tishembitov.pictorium.tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tishembitov.pictorium.exception.ResourceNotFoundException;
import ru.tishembitov.pictorium.pin.Pin;
import ru.tishembitov.pictorium.pin.PinRepository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final PinRepository pinRepository;
    private final TagMapper tagMapper;

    @Transactional
    public Set<Tag> resolveTagsByNames(Set<String> tagNames) {
        log.debug("Resolving tags: {}", tagNames);

        if (tagNames == null || tagNames.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Tag> tags = tagNames.stream()
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .map(String::toLowerCase)
                .map(this::findOrCreateTag)
                .collect(Collectors.toSet());

        log.debug("Resolved {} tags", tags.size());
        return tags;
    }

    public Page<TagResponse> findAll(Pageable pageable) {
        log.debug("Fetching all tags with pagination: {}", pageable);

        Page<Tag> tags = tagRepository.findAll(pageable);
        return tags.map(tagMapper::toResponse);
    }

    public TagResponse findById(UUID id) {
        log.debug("Fetching tag by id: {}", id);

        Tag tag = findTagById(id);
        return tagMapper.toResponse(tag);
    }

    public List<TagResponse> findByPinId(UUID pinId) {
        log.debug("Fetching tags for pin {}", pinId);

        validatePinExists(pinId);
        List<Tag> tags = tagRepository.findByPinId(pinId);

        return tagMapper.toResponseList(tags);
    }

    public List<TagResponse> searchByName(String query, int limit) {
        log.debug("Searching tags by query: {} with limit: {}", query, limit);

        Pageable pageable = PageRequest.of(0, limit);
        List<Tag> tags = tagRepository.findByNameContainingIgnoreCase(query, pageable);

        return tagMapper.toResponseList(tags);
    }

    public List<CategoryResponse> getCategories(int limit) {
        log.debug("Fetching categories with limit: {}", limit);

        List<CategoryResponse> result = new ArrayList<>();

        pinRepository.findTopByOrderByCreatedAtDesc()
                .map(tagMapper::toEverythingCategory)
                .ifPresent(result::add);

        tagRepository.findAllWithPins().stream()
                .limit(limit)
                .map(tagMapper::toCategory)
                .filter(Objects::nonNull)
                .forEach(result::add);

        return result;
    }

    private Tag findOrCreateTag(String name) {
        return tagRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    log.debug("Creating new tag: {}", name);
                    Tag newTag = Tag.builder()
                            .name(name)
                            .build();
                    return tagRepository.save(newTag);
                });
    }

    private Tag findTagById(UUID tagId) {
        return tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + tagId));
    }

    private void validatePinExists(UUID pinId) {
        if (!pinRepository.existsById(pinId)) {
            throw new ResourceNotFoundException("Pin not found with id: " + pinId);
        }
    }
}