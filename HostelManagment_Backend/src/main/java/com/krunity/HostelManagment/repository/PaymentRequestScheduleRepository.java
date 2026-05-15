package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.enums.TransactionStatus;
import com.krunity.HostelManagment.model.PaymentRequestSchedule;
import com.krunity.HostelManagment.model.TenantPaymentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRequestScheduleRepository extends JpaRepository<PaymentRequestSchedule, UUID> {

    List<PaymentRequestSchedule> findByTenantPaymentPlan(TenantPaymentPlan plan);

    List<PaymentRequestSchedule> findByTenantPaymentPlan_PlanId(UUID planId);

    List<PaymentRequestSchedule> findByTenantPaymentPlan_Tenant_UserId(UUID tenantId);

    List<PaymentRequestSchedule> findByPaymentStatusAndDueDateBefore(TransactionStatus status, LocalDate date);

    List<PaymentRequestSchedule> findByTenantPaymentPlan_Tenant_UserIdAndPaymentStatus(
            UUID tenantId, TransactionStatus status);
    
    List<PaymentRequestSchedule> findByTenantPaymentPlan_PlanIdAndPaymentStatusIn(
            UUID planId, List<TransactionStatus> statuses);
    
    List<PaymentRequestSchedule> findByDueDateAndPaymentStatusIn(LocalDate dueDate, List<TransactionStatus> statuses);
    
    List<PaymentRequestSchedule> findByDueDateBeforeAndPaymentStatusIn(LocalDate dueDate, List<TransactionStatus> statuses);
}
