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
@Table(name = "goals", indexes = {
        @Index(name = "idx_goals_employee", columnList = "employee_id"),
        @Index(name = "idx_goals_status", columnList = "status")
})
public class Goal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @Column(nullable = false)
    private String title;

    @Column(length = 1200)
    private String description;

    @Column(nullable = false)
    private String thrustArea;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UomType uomType;

    @Column(nullable = false)
    private Double target;

    private Double achievement;

    @Column(nullable = false)
    private Integer weightage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalStatus status;

    @Column(nullable = false)
    private boolean locked;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_goal_id")
    private SharedGoal sharedGoal;

    @Column(nullable = false)
    private LocalDate deadline;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = GoalStatus.DRAFT;
        }
    }
}
