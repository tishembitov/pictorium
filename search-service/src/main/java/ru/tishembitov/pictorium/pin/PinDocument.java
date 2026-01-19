package ru.tishembitov.pictorium.pin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Document(indexName = "#{@environment.getProperty('index.pins.name', 'pins')}")
@Setting(settingPath = "/elasticsearch/settings/pins-settings.json")
@Mapping(mappingPath = "/elasticsearch/mappings/pins-mapping.json")
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PinDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Keyword)
    @Builder.Default
    private Set<String> tags = new HashSet<>();

    @Field(type = FieldType.Keyword)
    private String authorId;

    @Field(type = FieldType.Text)
    private String authorUsername;

    @Field(type = FieldType.Keyword)
    private String imageId;

    @Field(type = FieldType.Keyword)
    private String thumbnailId;

    @Field(type = FieldType.Integer)
    @Builder.Default
    private Integer likeCount = 0;

    @Field(type = FieldType.Integer)
    @Builder.Default
    private Integer saveCount = 0;

    @Field(type = FieldType.Integer)
    @Builder.Default
    private Integer commentCount = 0;

    @Field(type = FieldType.Integer)
    @Builder.Default
    private Integer viewCount = 0;

    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant createdAt;

    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant updatedAt;

    @Field(type = FieldType.Integer)
    private Integer originalWidth;

    @Field(type = FieldType.Integer)
    private Integer originalHeight;

    @Field(type = FieldType.Dense_Vector, dims = 128)
    private float[] embedding;
}