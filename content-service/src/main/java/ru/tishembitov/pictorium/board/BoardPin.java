package ru.tishembitov.pictorium.board;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import ru.tishembitov.pictorium.pin.Pin;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "board_pins",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_board_pin",
                        columnNames = {"board_id", "pin_id"}
                )
        },
        indexes = {
                @Index(name = "idx_board_pins_board", columnList = "board_id"),
                @Index(name = "idx_board_pins_pin", columnList = "pin_id"),
                @Index(name = "idx_board_pins_added", columnList = "board_id, added_at DESC")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class BoardPin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pin_id", nullable = false)
    private Pin pin;

    @CreatedDate
    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;
}