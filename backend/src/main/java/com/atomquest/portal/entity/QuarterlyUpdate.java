package com.atomquest.portal.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "quarterly_updates", uniqueConstraints = {
        @UniqueConstraint(name = "uk_goal_quarter", columnNames = {"goal_id", "quarter"})
})
public class QuarterlyUpdate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Quarter quarter;

    @Column(nullable = false)
    private Double achievement;

    @Column(length = 1000)
    private String comment;

    @Column(nullable = false)
    private Double progressScore;

    @Enumerated(EnumType.STRING)
    private ProgressStatus status;

    private LocalDate completionDate;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
