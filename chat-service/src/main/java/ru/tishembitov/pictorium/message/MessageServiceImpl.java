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
        return sendMessageInternal(chatId, currentUserId, content, type, imageId);
    }

    @Override
    public MessageResponse sendMessage(UUID chatId, String senderId, String content, MessageType type, String imageId) {
        return sendMessageInternal(chatId, senderId, content, type, imageId);
    }

    private MessageResponse sendMessageInternal(UUID chatId, String senderId, String content, MessageType type, String imageId) {
        Chat chat = chatService.getChatEntityById(chatId);

        validateParticipant(chat, senderId);

        String receiverId = chat.getOtherParticipantId(senderId);

        Message message = messageMapper.toEntity(
                chat, senderId, receiverId, content, type, imageId
        );

        Message saved = messageRepository.save(message);
        log.info("Message saved: id={}, chatId={}, from={}, to={}, type={}",
                saved.getId(), chatId, senderId, receiverId, type);

        MessageResponse response = messageMapper.toResponse(saved);

        boolean recipientInChat = presenceService.isUserInChat(receiverId, chatId);

        if (!recipientInChat) {
            eventPublisher.publish(ChatEvent.builder()
                    .type(ChatEventType.NEW_MESSAGE.name())
                    .chatId(chatId)
                    .messageId(saved.getId())
                    .actorId(senderId)
                    .recipientId(receiverId)
                    .previewText(getPreviewContent(content, type))
                    .previewImageId(type == MessageType.IMAGE ? imageId : null)
                    .messageType(type.name())
                    .build());
            log.debug("Published NEW_MESSAGE event to Kafka for recipient {}", receiverId);
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> getChatMessages(UUID chatId, Pageable pageable) {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        Chat chat = chatService.getChatEntityById(chatId);
        validateParticipant(chat, currentUserId);

        Page<MessageResponse> result = messageRepository.findByChatId(chatId, pageable)
                .map(messageMapper::toResponse);

        log.debug("Retrieved {} messages for chat {} (page {}, size {})",
                result.getNumberOfElements(), chatId, pageable.getPageNumber(), pageable.getPageSize());

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getAllChatMessages(UUID chatId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        Chat chat = chatService.getChatEntityById(chatId);
        validateParticipant(chat, currentUserId);

        List<Message> messages = messageRepository.findAllByChatIdOrdered(chatId);
        log.debug("Retrieved all {} messages for chat {}", messages.size(), chatId);

        return messageMapper.toResponseList(messages);
    }

    @Override
    public int markAsRead(UUID chatId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        return markAsReadInternal(chatId, currentUserId);
    }

    @Override
    public int markAsRead(UUID chatId, String userId) {
        return markAsReadInternal(chatId, userId);
    }

    private int markAsReadInternal(UUID chatId, String userId) {
        Chat chat = chatService.getChatEntityById(chatId);
        validateParticipant(chat, userId);

        int updated = messageRepository.markMessagesAsRead(
                chatId, userId, MessageState.SENT, MessageState.READ
        );

        if (updated > 0) {
            String otherUserId = chat.getOtherParticipantId(userId);

            eventPublisher.publish(ChatEvent.builder()
                    .type(ChatEventType.MESSAGES_READ.name())
                    .chatId(chatId)
                    .actorId(userId)
                    .recipientId(otherUserId)
                    .build());

            log.info("Marked {} messages as read in chat {} by user {}", updated, chatId, userId);
        }

        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public int getUnreadCount(UUID chatId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        return getUnreadCountInternal(chatId, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public int getUnreadCount(UUID chatId, String userId) {
        return getUnreadCountInternal(chatId, userId);
    }

    private int getUnreadCountInternal(UUID chatId, String userId) {
        Chat chat = chatService.getChatEntityById(chatId);
        validateParticipant(chat, userId);

        return messageRepository.countUnreadMessages(chatId, userId, MessageState.SENT);
    }

    private void validateParticipant(Chat chat, String userId) {
        if (chat.isNotParticipant(userId)) {
            throw new AccessDeniedException("You are not a participant of this chat");
        }
    }

    private String getPreviewContent(String content, MessageType type) {
        if (type != MessageType.TEXT) {
            return "📎 " + type.name().toLowerCase();
        }
        if (content == null || content.isEmpty()) {
            return "";
        }
        return content.length() > 100 ? content.substring(0, 100) + "..." : content;
    }
}