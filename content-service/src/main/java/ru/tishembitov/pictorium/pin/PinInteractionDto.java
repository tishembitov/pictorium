package ru.tishembitov.pictorium.pin;

public record PinInteractionDto(
        Boolean isLiked,
        Boolean isSaved,
        String lastSavedBoardName,
        Integer savedToBoardsCount
) {
    public static PinInteractionDto empty() {
        return new PinInteractionDto(false, false, null, 0);
    }

    public static PinInteractionDto notSaved(boolean isLiked) {
        return new PinInteractionDto(isLiked, false, null, 0);
    }

    public static PinInteractionDto saved(boolean isLiked, String boardName, int boardCount) {
        return new PinInteractionDto(isLiked, true, boardName, boardCount);
    }
}