package com.atomquest.portal.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "system_configs")
public class SystemConfig {
    @Id
    @Column(name = "config_key", nullable = false)
    private String key;

    @Column(nullable = false)
    private String value;

    @Column(length = 500)
    private String description;
}
