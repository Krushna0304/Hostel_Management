package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.enums.InvoiceStatus;
import com.krunity.HostelManagment.model.InstallmentInvoice;
import com.krunity.HostelManagment.model.TenantPaymentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstallmentInvoiceRepository extends JpaRepository<InstallmentInvoice, UUID> {

    List<InstallmentInvoice> findByPaymentPlan(TenantPaymentPlan paymentPlan);

    List<InstallmentInvoice> findByPaymentPlan_PlanId(UUID planId);

    List<InstallmentInvoice> findByPaymentPlanAndStatus(TenantPaymentPlan paymentPlan, InvoiceStatus status);

    Optional<InstallmentInvoice> findByPaymentPlanAndInstallmentNumber(
            TenantPaymentPlan paymentPlan, 
            Integer installmentNumber
    );

    List<InstallmentInvoice> findByDueDateBefore(LocalDate date);

    List<InstallmentInvoice> findByStatusAndDueDateBefore(InvoiceStatus status, LocalDate date);

    Optional<InstallmentInvoice> findByInvoiceNumber(String invoiceNumber);
}