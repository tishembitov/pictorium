package ru.tishembitov.pictorium.board;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record SavePinToBoardsRequest(
        @NotEmpty(message = "At least one board must be selected")
        List<UUID> boardIds
) {}