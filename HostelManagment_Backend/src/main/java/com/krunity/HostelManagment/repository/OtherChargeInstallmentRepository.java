package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.enums.PaymentStatus;
import com.krunity.HostelManagment.model.OtherCharge;
import com.krunity.HostelManagment.model.OtherChargeInstallment;
import com.krunity.HostelManagment.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OtherChargeInstallmentRepository extends JpaRepository<OtherChargeInstallment, UUID> {

    // Find installments by charge
    List<OtherChargeInstallment> findByOtherChargeOrderByInstallmentNumber(OtherCharge otherCharge);

    // Find installments by tenant
    List<OtherChargeInstallment> findByTenantOrderByDueDateAsc(User tenant);

    // Find pending installments for a tenant
    @Query("SELECT oci FROM OtherChargeInstallment oci WHERE oci.tenant = :tenant AND oci.paymentStatus IN :statuses ORDER BY oci.dueDate ASC")
    List<OtherChargeInstallment> findByTenantAndPaymentStatusIn(@Param("tenant") User tenant, @Param("statuses") List<PaymentStatus> statuses);

    // Find overdue installments
    @Query("SELECT oci FROM OtherChargeInstallment oci WHERE oci.dueDate < :currentDate AND oci.paymentStatus != 'COMPLETED' ORDER BY oci.dueDate ASC")
    List<OtherChargeInstallment> findOverdueInstallments(@Param("currentDate") LocalDateTime currentDate);

    // Find next due installment for a charge
    @Query("SELECT oci FROM OtherChargeInstallment oci WHERE oci.otherCharge = :charge AND oci.paymentStatus IN ('PENDING', 'PARTIALLY_PAID', 'OVERDUE') ORDER BY oci.installmentNumber ASC")
    List<OtherChargeInstallment> findNextDueInstallments(@Param("charge") OtherCharge charge);

    // Find installments by charge and status
    List<OtherChargeInstallment> findByOtherChargeAndPaymentStatusOrderByInstallmentNumber(OtherCharge otherCharge, PaymentStatus paymentStatus);

    // Count pending installments for a tenant
    @Query("SELECT COUNT(oci) FROM OtherChargeInstallment oci WHERE oci.tenant = :tenant AND oci.paymentStatus != 'COMPLETED'")
    Long countPendingInstallmentsByTenant(@Param("tenant") User tenant);

    // Count overdue installments for a tenant
    @Query("SELECT COUNT(oci) FROM OtherChargeInstallment oci WHERE oci.tenant = :tenant AND oci.dueDate < :currentDate AND oci.paymentStatus != 'COMPLETED'")
    Long countOverdueInstallmentsByTenant(@Param("tenant") User tenant, @Param("currentDate") LocalDateTime currentDate);
}