package ru.tishembitov.pictorium.message;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ru.tishembitov.pictorium.chat.Chat;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MessageMapper {

    @Mapping(target = "chatId", source = "chat.id")
    MessageResponse toResponse(Message message);

    List<MessageResponse> toResponseList(List<Message> messages);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "chat", source = "chat")
    @Mapping(target = "senderId", source = "senderId")
    @Mapping(target = "receiverId", source = "receiverId")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "imageId", source = "imageId")
    @Mapping(target = "state", constant = "SENT")
    @Mapping(target = "createdAt", ignore = true)
    Message toEntity(Chat chat, String senderId, String receiverId,
                     String content, MessageType type, String imageId);
}