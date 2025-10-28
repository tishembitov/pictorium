package ru.tishembitov.pictorium.tag;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping
    public ResponseEntity<Page<TagResponse>> getAllTags(Pageable pageable) {
        return ResponseEntity.ok(tagService.findAll(pageable));
    }

    @GetMapping("/{tagId}")
    public ResponseEntity<TagResponse> getById(@PathVariable UUID tagId) {
        return ResponseEntity.ok(tagService.findById(tagId));
    }

    @GetMapping("/pins/{pinId}")
    public ResponseEntity<List<TagResponse>> getPinTags(@PathVariable UUID pinId) {
        return ResponseEntity.ok(tagService.findByPinId(pinId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<TagResponse>> searchTags(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(tagService.searchByName(q, limit));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getCategories(@RequestParam(defaultValue = "8") int limit) {
        return ResponseEntity.ok(tagService.getCategories(limit));
    }
}