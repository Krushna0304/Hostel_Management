package com.krunity.HostelManagment.model;


import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Table(name = "rooms",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"hostel_id", "floor_id","room_number"})
        })
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "room_id",updatable = false)
    private UUID roomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hostel_id", nullable = false)
    private Hostel hostel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "floor_id", nullable = false)
    private Floor floor;

    @Column(name = "room_number",nullable = false)
    private String roomNumber;

    @Column(name = "room_details",columnDefinition = "TEXT")
    private String roomDetails;

    @Column(name = "total_beds",nullable = false)
    private Integer totalBeds;

    @Column(name = "available_beds",nullable = false)
    private Integer availableBeds;

    @Column(name = "is_active",nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean isActive;
}
