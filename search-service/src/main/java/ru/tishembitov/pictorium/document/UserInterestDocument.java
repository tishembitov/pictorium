package ru.tishembitov.pictorium.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Document(indexName = "#{@environment.getProperty('index.user-interests.name', 'user_interests')}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInterestDocument {

    @Id
    private String userId;

    @Field(type = FieldType.Object)
    @Builder.Default
    private Map<String, Double> tagWeights = new HashMap<>();

    @Field(type = FieldType.Keyword)
    @Builder.Default
    private Set<String> followedAuthors = new HashSet<>();

    @Field(type = FieldType.Object)
    @Builder.Default
    private Map<String, Integer> likedAuthors = new HashMap<>();

    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant updatedAt;
}