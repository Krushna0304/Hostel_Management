package com.krunity.HostelManagment.model;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Table(name = "announcements")
@Builder
public class
Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "announcement_id", updatable = false, nullable = false)
    private UUID announcementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hostel_id", nullable = false)
    private Hostel hostel;

    @Column(name = "title",length = 100, nullable = false)
    private String title;

    @Column(name = "message",length = 500,nullable = false)
    private String message;

    @Column(name = "created_by",nullable = false,updatable = false)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
