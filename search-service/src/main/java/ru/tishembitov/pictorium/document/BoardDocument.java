package ru.tishembitov.pictorium.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.Instant;

@Document(indexName = "#{@environment.getProperty('index.boards.name', 'boards')}")
@Setting(settingPath = "/elasticsearch/settings/boards-settings.json")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "autocomplete", searchAnalyzer = "standard")
    private String title;

    @Field(type = FieldType.Keyword)
    private String userId;

    @Field(type = FieldType.Text)
    private String username;

    @Field(type = FieldType.Integer)
    @Builder.Default
    private Integer pinCount = 0;

    @Field(type = FieldType.Keyword)
    private String previewImageId;

    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant createdAt;

    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant updatedAt;
}