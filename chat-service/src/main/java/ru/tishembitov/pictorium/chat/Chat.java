package ru.tishembitov.pictorium.chat;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import ru.tishembitov.pictorium.message.Message;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chats", indexes = {
        @Index(name = "idx_chat_participants", columnList = "senderId, recipientId"),
        @Index(name = "idx_chat_sender", columnList = "senderId"),
        @Index(name = "idx_chat_recipient", columnList = "recipientId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String senderId;

    @Column(nullable = false)
    private String recipientId;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    @Builder.Default
    private List<Message> messages = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;


    public boolean isParticipant(String userId) {
        return senderId.equals(userId) || recipientId.equals(userId);
    }

    public boolean isNotParticipant(String userId) {
        return !isParticipant(userId);
    }

    public String getOtherParticipantId(String userId) {
        return senderId.equals(userId) ? recipientId : senderId;
    }
}