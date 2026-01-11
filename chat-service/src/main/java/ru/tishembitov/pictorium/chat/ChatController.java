package ru.tishembitov.pictorium.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/with/{recipientId}")
    public ResponseEntity<ChatResponse> getOrCreateChat(@PathVariable String recipientId) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(chatService.getOrCreateChat(recipientId));
    }

    @GetMapping
    public ResponseEntity<List<ChatResponse>> getMyChats() {
        return ResponseEntity.ok(chatService.getMyChats());
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<ChatResponse> getChatById(@PathVariable UUID chatId) {
        return ResponseEntity.ok(chatService.getChatById(chatId));
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<Void> deleteChat(@PathVariable UUID chatId) {
        chatService.deleteChat(chatId);
        return ResponseEntity.noContent().build();
    }
}