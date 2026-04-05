package com.krunity.HostelManagment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "hostels")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Hostel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "hostel_id", updatable = false, nullable = false)
    private UUID hostelId;

    @Column(name = "hostel_name", length = 100, nullable = false)
    private String hostelName;

    @Column(name = "hostel_address", columnDefinition = "TEXT", nullable = false)
    private String hostelAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
