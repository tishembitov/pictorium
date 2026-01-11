package ru.tishembitov.pictorium.presence;

import java.util.Map;

public record UserPresenceResponse(
        Map<String, Boolean> onlineStatus
) {}