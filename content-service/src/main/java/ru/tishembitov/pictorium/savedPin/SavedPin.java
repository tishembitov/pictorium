package ru.tishembitov.pictorium.savedPin;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import ru.tishembitov.pictorium.pin.Pin;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "saved_pins",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_saved_pin_user_pin",
                        columnNames = {"user_id", "pin_id"}
                )
        },
        indexes = {
                @Index(name = "idx_saved_pins_user", columnList = "user_id"),
                @Index(name = "idx_saved_pins_user_created", columnList = "user_id, created_at DESC")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class SavedPin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pin_id", nullable = false)
    private Pin pin;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
