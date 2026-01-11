package ru.tishembitov.pictorium.chat;

import java.util.List;
import java.util.UUID;

public interface ChatService {

    ChatResponse getOrCreateChat(String recipientId);

    List<ChatResponse> getMyChats();

    ChatResponse getChatById(UUID chatId);

    void deleteChat(UUID chatId);

    Chat getChatEntityById(UUID chatId);
}