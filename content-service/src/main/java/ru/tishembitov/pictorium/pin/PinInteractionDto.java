package ru.tishembitov.pictorium.pin;

public record PinInteractionDto(
        Boolean isLiked,
        Boolean isSaved,
        Boolean isSavedToProfile,
        String savedToBoardName,
        Integer savedToBoardCount
) {
    public static PinInteractionDto empty() {
        return new PinInteractionDto(false, false, false, null, 0);
    }

    public static PinInteractionDto notSaved(boolean isLiked) {
        return new PinInteractionDto(isLiked, false, false, null, 0);
    }

    public static PinInteractionDto savedToProfile(boolean isLiked) {
        return new PinInteractionDto(isLiked, true, true, null, 0);
    }

    public static PinInteractionDto savedToBoards(
            boolean isLiked,
            boolean savedToProfile,
            String boardName,
            int boardCount
    ) {
        return new PinInteractionDto(isLiked, true, savedToProfile, boardName, boardCount);
    }
}