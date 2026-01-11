package ru.tishembitov.pictorium.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tishembitov.pictorium.chat.Chat;
import ru.tishembitov.pictorium.chat.ChatService;
import ru.tishembitov.pictorium.kafka.ChatEvent;
import ru.tishembitov.pictorium.kafka.ChatEventPublisher;
import ru.tishembitov.pictorium.kafka.ChatEventType;
import ru.tishembitov.pictorium.presence.PresenceService;
import ru.tishembitov.pictorium.util.SecurityUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;
    private final ChatService chatService;
    private final PresenceService presenceService;
    private final ChatEventPublisher eventPublisher;

    @Override
    public MessageResponse sendMessage(UUID chatId, String content, MessageType type, String imageId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        Chat chat = chatService.getChatEntityById(chatId);

        validateParticipant(chat, currentUserId);

        String receiverId = chat.getOtherParticipantId(currentUserId);

        Message message = messageMapper.toEntity(
                chat, currentUserId, receiverId, content, type, imageId
        );

        Message saved = messageRepository.save(message);
        log.info("Message sent: {} in chat {} from {} to {}",
                saved.getId(), chatId, currentUserId, receiverId);

        MessageResponse response = messageMapper.toResponse(saved);

        // Проверяем, находится ли получатель в этом чате
        boolean recipientInChat = presenceService.isUserInChat(receiverId, chatId);

        if (!recipientInChat) {
            // Отправляем событие в Kafka для Notification Service
            eventPublisher.publish(ChatEvent.builder()
                    .type(ChatEventType.NEW_MESSAGE)
                    .chatId(chatId)
                    .messageId(saved.getId())
                    .senderId(currentUserId)
                    .receiverId(receiverId)
                    .content(getPreviewContent(content, type))
                    .messageType(type)
                    .build());
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> getChatMessages(UUID chatId, Pageable pageable) {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        Chat chat = chatService.getChatEntityById(chatId);
        validateParticipant(chat, currentUserId);

        return messageRepository.findByChatId(chatId, pageable)
                .map(messageMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getAllChatMessages(UUID chatId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        Chat chat = chatService.getChatEntityById(chatId);
        validateParticipant(chat, currentUserId);

        return messageMapper.toResponseList(
                messageRepository.findAllByChatIdOrdered(chatId)
        );
    }

    @Override
    public int markAsRead(UUID chatId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        Chat chat = chatService.getChatEntityById(chatId);
        validateParticipant(chat, currentUserId);

        int updated = messageRepository.markMessagesAsRead(
                chatId, currentUserId, MessageState.SENT, MessageState.READ
        );

        if (updated > 0) {
            String senderId = chat.getOtherParticipantId(currentUserId);

            // Отправляем событие о прочтении
            eventPublisher.publish(ChatEvent.builder()
                    .type(ChatEventType.MESSAGES_READ)
                    .chatId(chatId)
                    .senderId(currentUserId)
                    .receiverId(senderId)
                    .build());

            log.info("Marked {} messages as read in chat {} by user {}",
                    updated, chatId, currentUserId);
        }

        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public int getUnreadCount(UUID chatId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        return messageRepository.countUnreadMessages(chatId, currentUserId, MessageState.SENT);
    }

    private void validateParticipant(Chat chat, String userId) {
        if (chat.isParticipant(userId)) {
            throw new AccessDeniedException("You are not a participant of this chat");
        }
    }

    private String getPreviewContent(String content, MessageType type) {
        if (type != MessageType.TEXT) {
            return "📎 " + type.name().toLowerCase();
        }
        if (content == null) return "";
        return content.length() > 100 ? content.substring(0, 100) + "..." : content;
    }
}