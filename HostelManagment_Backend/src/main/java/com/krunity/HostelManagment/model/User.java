package com.krunity.HostelManagment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id",unique = true, nullable = false,updatable = false)
    private UUID userId;

    @Column(name = "display_name",nullable = false,length = 100)
    private String displayName;

    @Column(name = "username",unique = true,nullable = false,length = 50)
    private String username;

    @Column(name = "password_hash",nullable = false)
    private String passwordHash;

    @Column(name = "phone_number", nullable = false,unique = true,length = 15)
    private String phoneNumber;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "is_active",columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean isActive;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
