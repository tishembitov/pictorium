package ru.tishembitov.pictorium.presence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PresenceService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PRESENCE_KEY_PREFIX = "presence:user:";
    private static final String ACTIVE_CHAT_KEY_PREFIX = "active-chat:user:";
    private static final String TYPING_KEY_PREFIX = "typing:chat:";

    @Value("${presence.offline-threshold-seconds:60}")
    private int offlineThresholdSeconds;

    @Value("${presence.typing-timeout-seconds:3}")
    private int typingTimeoutSeconds;

    public void updatePresence(String userId) {
        String key = PRESENCE_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, Instant.now().toEpochMilli());
        redisTemplate.expire(key, Duration.ofSeconds(offlineThresholdSeconds));
        log.debug("Presence updated for user: {}", userId);
    }

    public void removePresence(String userId) {
        redisTemplate.delete(PRESENCE_KEY_PREFIX + userId);
        redisTemplate.delete(ACTIVE_CHAT_KEY_PREFIX + userId);
        log.debug("Presence removed for user: {}", userId);
    }

    public boolean isUserOnline(String userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PRESENCE_KEY_PREFIX + userId));
    }

    public Map<String, Boolean> getOnlineStatus(Set<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> keys = userIds.stream()
                .map(id -> PRESENCE_KEY_PREFIX + id)
                .toList();

        List<Object> values = redisTemplate.opsForValue().multiGet(keys);

        Map<String, Boolean> result = new HashMap<>();
        int i = 0;
        for (String userId : userIds) {
            result.put(userId, values != null && values.get(i) != null);
            i++;
        }
        return result;
    }

    public void setActiveChat(String userId, UUID chatId) {
        String key = ACTIVE_CHAT_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, chatId.toString());
        log.debug("User {} entered chat {}", userId, chatId);
    }

    public void removeActiveChat(String userId) {
        redisTemplate.delete(ACTIVE_CHAT_KEY_PREFIX + userId);
        log.debug("User {} left active chat", userId);
    }

    public Optional<UUID> getActiveChat(String userId) {
        Object value = redisTemplate.opsForValue().get(ACTIVE_CHAT_KEY_PREFIX + userId);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value.toString()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public boolean isUserInChat(String userId, UUID chatId) {
        return getActiveChat(userId)
                .map(activeChatId -> activeChatId.equals(chatId))
                .orElse(false);
    }

    public void startTyping(String userId, UUID chatId) {
        String key = TYPING_KEY_PREFIX + chatId;
        redisTemplate.opsForSet().add(key, userId);
        redisTemplate.expire(key, Duration.ofSeconds(typingTimeoutSeconds));
        log.debug("User {} started typing in chat {}", userId, chatId);
    }

    public void stopTyping(String userId, UUID chatId) {
        String key = TYPING_KEY_PREFIX + chatId;
        redisTemplate.opsForSet().remove(key, userId);
        log.debug("User {} stopped typing in chat {}", userId, chatId);
    }

    public Set<String> getTypingUsers(UUID chatId) {
        String key = TYPING_KEY_PREFIX + chatId;
        Set<Object> members = redisTemplate.opsForSet().members(key);
        if (members == null) {
            return Collections.emptySet();
        }
        return members.stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
    }
}