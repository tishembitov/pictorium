package ru.tishembitov.pictorium.personalization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalizationBoosts {

    @Builder.Default
    private Map<String, Float> tagBoosts = Collections.emptyMap();

    @Builder.Default
    private Map<String, Float> authorBoosts = Collections.emptyMap();

    public static PersonalizationBoosts empty() {
        return PersonalizationBoosts.builder().build();
    }

    public boolean isEmpty() {
        return tagBoosts.isEmpty() && authorBoosts.isEmpty();
    }
}