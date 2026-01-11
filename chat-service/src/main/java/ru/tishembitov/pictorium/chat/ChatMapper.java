package ru.tishembitov.pictorium.chat;

import org.mapstruct.*;
import ru.tishembitov.pictorium.message.Message;
import ru.tishembitov.pictorium.message.MessageState;
import ru.tishembitov.pictorium.message.MessageType;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ChatMapper {

    @Mapping(target = "recipientId", expression = "java(chat.getOtherParticipantId(currentUserId))")
    @Mapping(target = "lastMessage", expression = "java(getLastMessagePreview(chat))")
    @Mapping(target = "lastMessageTime", expression = "java(getLastMessageTime(chat))")
    @Mapping(target = "unreadCount", expression = "java(countUnreadMessages(chat, currentUserId))")
    ChatResponse toResponse(Chat chat, @Context String currentUserId);

    default List<ChatResponse> toResponseList(List<Chat> chats, String currentUserId) {
        return chats.stream()
                .map(chat -> toResponse(chat, currentUserId))
                .toList();
    }

    default String getLastMessagePreview(Chat chat) {
        if (chat.getMessages() == null || chat.getMessages().isEmpty()) {
            return null;
        }
        Message lastMessage = chat.getMessages().get(0);
        if (lastMessage.getType() != MessageType.TEXT) {
            return "📎 Attachment";
        }
        String content = lastMessage.getContent();
        return content != null && content.length() > 50
                ? content.substring(0, 50) + "..."
                : content;
    }

    default java.time.Instant getLastMessageTime(Chat chat) {
        if (chat.getMessages() == null || chat.getMessages().isEmpty()) {
            return chat.getCreatedAt();
        }
        return chat.getMessages().get(0).getCreatedAt();
    }

    default int countUnreadMessages(Chat chat, String currentUserId) {
        if (chat.getMessages() == null) {
            return 0;
        }
        return (int) chat.getMessages().stream()
                .filter(m -> m.getReceiverId().equals(currentUserId))
                .filter(m -> m.getState() == MessageState.SENT)
                .count();
    }
}