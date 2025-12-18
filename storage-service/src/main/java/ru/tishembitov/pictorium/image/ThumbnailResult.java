package ru.tishembitov.pictorium.image;

public record ThumbnailResult(
        byte[] data,
        int width,
        int height
) {}