package ru.tishembitov.pictorium.pin;

import java.util.UUID;

public record PinInteractionDto(
        Boolean isLiked,
        UUID lastSavedBoardId,
        String lastSavedBoardName,
        Integer savedToBoardsCount
) {
    public static PinInteractionDto empty() {
        return new PinInteractionDto(false, null, null, 0);
    }

    public static PinInteractionDto notSaved(boolean isLiked) {
        return new PinInteractionDto(isLiked, null, null, 0);
    }

    public static PinInteractionDto saved(boolean isLiked, UUID boardId, String boardName, int boardCount) {
        return new PinInteractionDto(isLiked, boardId, boardName, boardCount);
    }

    public boolean isSaved() {
        return savedToBoardsCount > 0;
    }
}