package ru.tishembitov.pictorium.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tishembitov.pictorium.exception.BadRequestException;
import ru.tishembitov.pictorium.exception.ResourceNotFoundException;
import ru.tishembitov.pictorium.util.SecurityUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final ChatMapper chatMapper;

    @Override
    public ChatResponse getOrCreateChat(String recipientId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();

        if (currentUserId.equals(recipientId)) {
            throw new BadRequestException("Cannot create chat with yourself");
        }

        Chat chat = chatRepository.findByParticipants(currentUserId, recipientId)
                .orElseGet(() -> createNewChat(currentUserId, recipientId));

        log.info("Chat retrieved/created: {} between {} and {}",
                chat.getId(), currentUserId, recipientId);

        return chatMapper.toResponse(chat, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatResponse> getMyChats() {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        List<Chat> chats = chatRepository.findAllByUserId(currentUserId);
        return chatMapper.toResponseList(chats, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public ChatResponse getChatById(UUID chatId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        Chat chat = getChatEntityById(chatId);
        validateParticipant(chat, currentUserId);
        return chatMapper.toResponse(chat, currentUserId);
    }

    @Override
    public void deleteChat(UUID chatId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        Chat chat = getChatEntityById(chatId);
        validateParticipant(chat, currentUserId);

        chatRepository.delete(chat);
        log.info("Chat {} deleted by user {}", chatId, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Chat getChatEntityById(UUID chatId) {
        return chatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found: " + chatId));
    }

    private Chat createNewChat(String senderId, String recipientId) {
        Chat chat = Chat.builder()
                .senderId(senderId)
                .recipientId(recipientId)
                .build();

        Chat saved = chatRepository.save(chat);
        log.info("New chat created: {} between {} and {}", saved.getId(), senderId, recipientId);
        return saved;
    }

    private void validateParticipant(Chat chat, String userId) {
        if (chat.isParticipant(userId)) {
            throw new AccessDeniedException("You are not a participant of this chat");
        }
    }
}
