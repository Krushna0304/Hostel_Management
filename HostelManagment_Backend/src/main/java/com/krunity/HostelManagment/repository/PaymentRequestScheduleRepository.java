package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.enums.TransactionStatus;
import com.krunity.HostelManagment.model.PaymentRequestSchedule;
import com.krunity.HostelManagment.model.TenantPaymentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.UUID;

@Repository
public interface PaymentRequestScheduleRepository extends JpaRepository<PaymentRequestSchedule, UUID> {

    List<PaymentRequestSchedule> findByTenantPaymentPlan(TenantPaymentPlan plan);

    List<PaymentRequestSchedule> findByTenantPaymentPlan_PlanId(UUID planId);
    
    // Add ordered version for proper installment display
    @Query("SELECT prs FROM PaymentRequestSchedule prs WHERE prs.tenantPaymentPlan.planId = :planId " +
           "ORDER BY prs.installmentNumber ASC")
    List<PaymentRequestSchedule> findByTenantPaymentPlan_PlanIdOrderByInstallmentNumber(
            @Param("planId") UUID planId);

    List<PaymentRequestSchedule> findByTenantPaymentPlan_Tenant_UserId(UUID tenantId);

    List<PaymentRequestSchedule> findByPaymentStatusAndDueDateBefore(TransactionStatus status, LocalDate date);

    List<PaymentRequestSchedule> findByTenantPaymentPlan_Tenant_UserIdAndPaymentStatus(
            UUID tenantId, TransactionStatus status);
    
    List<PaymentRequestSchedule> findByTenantPaymentPlan_PlanIdAndPaymentStatusIn(
            UUID planId, List<TransactionStatus> statuses);
    
    // Add ordered version for proper installment collection priority
    @Query("SELECT prs FROM PaymentRequestSchedule prs WHERE prs.tenantPaymentPlan.planId = :planId " +
           "AND prs.paymentStatus IN :statuses " +
           "ORDER BY CASE " +
           "  WHEN prs.paymentStatus = 'OVERDUE' THEN 1 " +
           "  WHEN prs.paymentStatus = 'PARTIALLY_PAID' THEN 2 " +
           "  WHEN prs.paymentStatus = 'SCHEDULED' THEN 3 " +
           "  ELSE 4 END, " +
           "prs.dueDate ASC")
    List<PaymentRequestSchedule> findByTenantPaymentPlan_PlanIdAndPaymentStatusInOrderByPriorityAndDueDate(
            @Param("planId") UUID planId, @Param("statuses") List<TransactionStatus> statuses);
    
    List<PaymentRequestSchedule> findByDueDateAndPaymentStatusIn(LocalDate dueDate, List<TransactionStatus> statuses);
    
    List<PaymentRequestSchedule> findByDueDateBeforeAndPaymentStatusIn(LocalDate dueDate, List<TransactionStatus> statuses);
}
