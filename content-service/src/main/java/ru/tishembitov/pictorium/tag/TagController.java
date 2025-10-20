package ru.tishembitov.pictorium.tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
        Page<TagResponse> tags = tagService.findAll(pageable);
        return ResponseEntity.ok().body(tags);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TagResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(tagService.findById(id));
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
        List<TagResponse> tags = tagService.searchByName(q, limit);
        return ResponseEntity.ok().body(tags);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getCategories(@RequestParam(defaultValue = "8") int limit) {
        List<CategoryResponse> categories = tagService.getCategories(limit);
        return ResponseEntity.ok().body(categories);
    }
}