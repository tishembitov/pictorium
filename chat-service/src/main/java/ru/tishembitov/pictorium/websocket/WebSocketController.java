package ru.tishembitov.pictorium.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import ru.tishembitov.pictorium.chat.Chat;
import ru.tishembitov.pictorium.chat.ChatService;
import ru.tishembitov.pictorium.message.MessageResponse;
import ru.tishembitov.pictorium.message.MessageService;
import ru.tishembitov.pictorium.message.MessageType;
import ru.tishembitov.pictorium.presence.PresenceService;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final ChatService chatService;
    private final PresenceService presenceService;
    private final WebSocketSessionManager sessionManager;

    @MessageMapping("/chat")
    public void handleMessage(@Payload WsIncomingMessage incoming, Principal principal) {
        if (principal == null) {
            log.warn("Received message without principal");
            return;
        }

        String userId = principal.getName();
        UUID chatId = incoming.getChatId();

        try {
            switch (incoming.getType()) {
                case SEND_MESSAGE -> handleSendMessage(userId, incoming);
                case TYPING_START -> handleTypingStart(userId, chatId);
                case TYPING_STOP -> handleTypingStop(userId, chatId);
                case MARK_READ -> handleMarkRead(userId, chatId);
                case JOIN_CHAT -> handleJoinChat(userId, chatId);
                case LEAVE_CHAT -> handleLeaveChat(userId, chatId);
                case HEARTBEAT -> handleHeartbeat(userId);
                default -> log.warn("Unknown message type: {}", incoming.getType());
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
            sendToUser(userId, WsOutgoingMessage.error(e.getMessage()));
        }
    }

    private void handleSendMessage(String userId, WsIncomingMessage incoming) {
        MessageType type = incoming.getMessageType() != null
                ? incoming.getMessageType()
                : MessageType.TEXT;

        MessageResponse message = messageService.sendMessage(
                incoming.getChatId(),
                incoming.getContent(),
                type,
                incoming.getImageId()
        );

        WsOutgoingMessage outgoing = WsOutgoingMessage.newMessage(message);

        // Отправляем отправителю
        sendToUser(userId, outgoing);

        // Отправляем получателю (если он в чате)
        String recipientId = message.receiverId();
        if (sessionManager.isUserInChat(recipientId, incoming.getChatId())) {
            sendToUser(recipientId, outgoing);
        }

        log.debug("Message sent in chat {}: {} -> {}",
                incoming.getChatId(), userId, recipientId);
    }

    private void handleTypingStart(String userId, UUID chatId) {
        presenceService.startTyping(userId, chatId);

        Chat chat = chatService.getChatEntityById(chatId);
        String recipientId = chat.getOtherParticipantId(userId);

        if (sessionManager.isUserInChat(recipientId, chatId)) {
            sendToUser(recipientId, WsOutgoingMessage.typing(chatId, userId));
        }
    }

    private void handleTypingStop(String userId, UUID chatId) {
        presenceService.stopTyping(userId, chatId);

        Chat chat = chatService.getChatEntityById(chatId);
        String recipientId = chat.getOtherParticipantId(userId);

        if (sessionManager.isUserInChat(recipientId, chatId)) {
            sendToUser(recipientId, WsOutgoingMessage.stoppedTyping(chatId, userId));
        }
    }

    private void handleMarkRead(String userId, UUID chatId) {
        int count = messageService.markAsRead(chatId);

        if (count > 0) {
            Chat chat = chatService.getChatEntityById(chatId);
            String senderId = chat.getOtherParticipantId(userId);

            // Уведомляем отправителя, что его сообщения прочитаны
            if (sessionManager.isUserOnline(senderId)) {
                sendToUser(senderId, WsOutgoingMessage.messagesRead(chatId, userId));
            }
        }
    }

    private void handleJoinChat(String userId, UUID chatId) {
        sessionManager.joinChat(userId, chatId);
        presenceService.setActiveChat(userId, chatId);

        // Автоматически помечаем сообщения как прочитанные
        handleMarkRead(userId, chatId);

        log.debug("User {} joined chat {}", userId, chatId);
    }

    private void handleLeaveChat(String userId, UUID chatId) {
        sessionManager.leaveChat(userId, chatId);
        presenceService.removeActiveChat(userId);
        presenceService.stopTyping(userId, chatId);

        log.debug("User {} left chat {}", userId, chatId);
    }

    private void handleHeartbeat(String userId) {
        presenceService.updatePresence(userId);
    }

    private void sendToUser(String userId, WsOutgoingMessage message) {
        messagingTemplate.convertAndSendToUser(
                userId,
                "/queue/messages",
                message
        );
    }
}