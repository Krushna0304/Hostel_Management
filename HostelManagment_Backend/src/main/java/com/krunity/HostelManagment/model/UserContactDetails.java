package com.krunity.HostelManagment.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "user_contact_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserContactDetails {

    @Id
    @GeneratedValue(strategy =  GenerationType.UUID)
    @Column(name = "contact_details_id", updatable = false, nullable = false)
    private UUID contactDetailsId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "profession", length = 100)
    private String profession;

    @Column(name = "emergency_contact", length = 100)
    private String emergencyContact;

    @Column(name = "email", length = 100)
    private String email;
}
