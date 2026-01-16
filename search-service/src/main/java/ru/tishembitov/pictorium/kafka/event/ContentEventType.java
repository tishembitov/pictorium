package ru.tishembitov.pictorium.kafka.event;

public enum ContentEventType {
    PIN_LIKED,
    PIN_COMMENTED,
    PIN_SAVED,
    COMMENT_LIKED,
    COMMENT_REPLIED,

    PIN_CREATED,
    PIN_UPDATED,
    PIN_DELETED,

    BOARD_CREATED,
    BOARD_UPDATED,
    BOARD_DELETED,
    BOARD_PIN_ADDED,
    BOARD_PIN_REMOVED
}