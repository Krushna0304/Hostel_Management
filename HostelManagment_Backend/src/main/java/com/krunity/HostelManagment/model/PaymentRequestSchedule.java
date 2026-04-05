package com.krunity.HostelManagment.model;

import com.krunity.HostelManagment.enums.TransactionStatus;
import jakarta.persistence.Column;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

public class PaymentRequestSchedule {

    private UUID paymentRequestId;
    private TenantPaymentPlan tenantPaymentPlan ;
    private Integer amount;
    private Date dueDate;
    private TransactionStatus paymentStatus;
    private Boolean isActive;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
