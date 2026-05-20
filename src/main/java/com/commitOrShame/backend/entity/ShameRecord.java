package com.commitOrShame.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "shame_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShameRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "shamed_user_id", nullable = false)
    private User shamedUser;

    private LocalDate shameDate;
    private int missedStreak;        // streak they had before breaking it
    private boolean notificationSent;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}