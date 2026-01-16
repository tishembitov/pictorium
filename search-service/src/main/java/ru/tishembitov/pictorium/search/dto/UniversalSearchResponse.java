package ru.tishembitov.pictorium.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UniversalSearchResponse {

    private List<PinSearchResult> pins;
    private List<UserSearchResult> users;
    private List<BoardSearchResult> boards;

    private long totalPins;
    private long totalUsers;
    private long totalBoards;

    private long took;
    private String query;

    // Для "показать ещё"
    private boolean hasMorePins;
    private boolean hasMoreUsers;
    private boolean hasMoreBoards;
}