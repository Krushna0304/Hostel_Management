package com.krunity.HostelManagment.model;

import com.krunity.HostelManagment.enums.RoomAllotmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;
import java.sql.Date;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Table(name = "room_allotments")
@Builder
public class RoomAllotment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "allotment_id", updatable = false, nullable = false)
    private UUID allotmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant;

    @Column(nullable = false)
    private String agreementId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_plan_id",nullable = false)
    private TenantPaymentPlan paymentPlanId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposit_transaction_id",nullable = false)
    private Transaction depositTransactionId;

    @Column(name = "allotment_date", nullable = false)
    private Date allotmentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_allotment_status", nullable = false)
    private RoomAllotmentStatus roomAllotmentStatus;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

}



//Already Created room aggrement Plans


//Create Aggrement - choose plan

//After accepting allot Room in the SQL

//Create Payment Schedule in the SQL  ()

//Transaction Service Make payment requests(same table with status)





