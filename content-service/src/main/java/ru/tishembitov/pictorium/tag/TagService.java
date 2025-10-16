package ru.tishembitov.pictorium.tag;

import java.util.Set;

public interface TagService {
    Tag getOrCreateTag(String name);
    Set<Tag> getOrCreateTags(Set<String> tagNames);
}
