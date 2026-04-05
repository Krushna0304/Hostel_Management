package com.krunity.HostelManagment.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Table(name = "floors",
        uniqueConstraints = {
           @UniqueConstraint(columnNames = {"hostel_id", "floor_number"})
})
@Builder
public class Floor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "floor_id", updatable = false)
    private UUID floorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hostel_id", nullable = false)
    private Hostel hostel;

    @Column(name = "floor_number", nullable = false)
    private Integer floorNumber;

    @Column(name = "total_rooms",columnDefinition = "INTEGER DEFAULT 0")
    private Integer totalRooms;

    @PrePersist
    public void prePersist() {
        if (totalRooms == null) {
            totalRooms = 0;
        }
    }



}
