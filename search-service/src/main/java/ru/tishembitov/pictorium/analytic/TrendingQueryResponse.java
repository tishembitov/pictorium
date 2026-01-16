package ru.tishembitov.pictorium.analytic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendingQueryResponse {
    private String query;
    private Long searchCount;
}