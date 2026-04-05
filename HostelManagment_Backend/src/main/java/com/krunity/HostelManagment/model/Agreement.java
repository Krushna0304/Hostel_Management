package com.krunity.HostelManagment.model;

import com.krunity.HostelManagment.enums.AgreementStatus;
import com.krunity.HostelManagment.enums.AgreementType;
import com.krunity.HostelManagment.enums.PlanStatus;
import com.krunity.HostelManagment.model.plan.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "agreements")
public class Agreement {
    @Id
    private String id;
    
    private AgreementType type; // ROOM / WORKER
    private AgreementStatus status;
    
    private UUID userId;
    private UUID roomId; // null for worker
    
    // Legacy fields - kept for backward compatibility, but will be populated from planSnapshot
    private BigDecimal rent;
    private BigDecimal deposit;
    private BigDecimal cleaningCharges;
    private BigDecimal maintenanceCharges;
    private String lightBillPolicy;
    @Builder.Default
    private List<String> facilities = new ArrayList<>();
    private Boolean parkingAllowed;
    
    // Plan-based fields
    private String planId; // Reference to the selected plan
    private RoomAgreementPlan planSnapshot; // Complete snapshot of the plan at agreement creation time
    
    private LocalDate startDate;
    
    private String qrToken;
    private Instant qrExpiry;
    
    @Builder.Default
    private Boolean qrUsed = false;
    
    private Instant createdAt;
    private Instant activatedAt;


    @Entity
    @Table(name = "password_reset_tokens")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PasswordResetToken {

        @jakarta.persistence.Id
        @GeneratedValue(strategy = GenerationType.UUID)
        @Column(name = "token_id", updatable = false, nullable = false)
        private UUID tokenId;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id", nullable = false)
        private User user;

        @Column(name = "token", nullable = false, unique = true, length = 255)
        private String token;

        @Column(name = "expiry_date", nullable = false)
        private Instant expiryDate;

        @Column(name = "used", nullable = false)
        @Builder.Default
        private Boolean used = false;

        @CreationTimestamp
        @Column(name = "created_at", nullable = false, updatable = false)
        private Instant createdAt;
    }
}
