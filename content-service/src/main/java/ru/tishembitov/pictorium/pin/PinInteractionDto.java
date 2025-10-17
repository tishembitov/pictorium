package ru.tishembitov.pictorium.pin;


public record PinInteractionDto(
        Boolean isLiked,
        Boolean isSaved
)
{
    static PinInteractionDto empty() {
        return new PinInteractionDto(false, false);
    }
}
