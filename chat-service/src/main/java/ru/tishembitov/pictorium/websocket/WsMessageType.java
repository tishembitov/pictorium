package ru.tishembitov.pictorium.websocket;

public enum WsMessageType {
    // Client → Server
    SEND_MESSAGE,
    TYPING_START,
    TYPING_STOP,
    MARK_READ,
    JOIN_CHAT,
    LEAVE_CHAT,
    HEARTBEAT,

    // Server → Client
    NEW_MESSAGE,
    USER_TYPING,
    USER_STOPPED_TYPING,
    MESSAGES_READ,
    USER_ONLINE,
    USER_OFFLINE,
    ERROR
}