package ru.tishembitov.pictorium.suggest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggestResponse {

    private List<Suggestion> suggestions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Suggestion {
        private String text;
        private SuggestionType type;
        private String imageId; // для превью
        private Float score;
    }

    public enum SuggestionType {
        PIN_TITLE, TAG, USERNAME
    }
}