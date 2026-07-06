package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.dto.InstallmentResponse;
import com.krunity.HostelManagment.dto.OwnerCollectionSummaryResponse;
import com.krunity.HostelManagment.dto.TenantDashboardResponse;
import com.krunity.HostelManagment.enums.TransactionStatus;
import com.krunity.HostelManagment.exception.NotFoundException;
import com.krunity.HostelManagment.model.PaymentRequestSchedule;
import com.krunity.HostelManagment.model.RoomAllotment;
import com.krunity.HostelManagment.model.TenantPaymentPlan;
import com.krunity.HostelManagment.repository.PaymentRequestScheduleRepository;
import com.krunity.HostelManagment.repository.RoomAllotmentRepository;
import com.krunity.HostelManagment.repository.TenantPaymentPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class TenantDashboardService {

    @Autowired
    private RoomAllotmentRepository roomAllotmentRepository;

    @Autowired
    private TenantPaymentPlanRepository paymentPlanRepository;

    @Autowired
    private PaymentRequestScheduleRepository scheduleRepository;

    /**
     * Returns the full dashboard summary for the currently logged-in tenant.
     */
    public TenantDashboardResponse getTenantDashboard(UUID tenantId) {
        // Get the tenant's current allotment across any live status (everything except
        // LEFT). This keeps the dashboard loading even after the tenant requests a
        // settlement (SETTLEMENT_REQUESTED) or enters the notice period.
        java.util.List<RoomAllotment> allotments = roomAllotmentRepository
                .findByTenant_UserIdAndRoomAllotmentStatusIn(
                        tenantId,
                        com.krunity.HostelManagment.enums.RoomAllotmentStatus.occupyingStatuses());

        // Prefer ACTIVE, then the most recently started allotment.
        RoomAllotment allotment = allotments.stream()
                .max(Comparator
                        .comparing((RoomAllotment a) ->
                                a.getRoomAllotmentStatus() == com.krunity.HostelManagment.enums.RoomAllotmentStatus.ACTIVE)
                        .thenComparing(a -> a.getStartDate() != null ? a.getStartDate() : java.time.LocalDate.MIN))
                .orElseThrow(() -> new NotFoundException("No room allotment found for tenant"));

        // Get active payment plan
        TenantPaymentPlan plan = paymentPlanRepository.findByTenant_UserIdAndIsActiveTrue(tenantId)
                .orElseThrow(() -> new NotFoundException("No active payment plan found for tenant"));

        // Get all schedules ordered by installment number
        List<PaymentRequestSchedule> schedules =
                scheduleRepository.findByTenantPaymentPlan_PlanIdOrderByInstallmentNumber(plan.getPlanId());

        long totalPaid = schedules.stream()
                .filter(s -> s.getPaymentStatus() == TransactionStatus.COMPLETED)
                .mapToLong(PaymentRequestSchedule::getPaidAmount)
                .sum();

        long totalPending = schedules.stream()
                .filter(s -> s.getPaymentStatus() == TransactionStatus.SCHEDULED
                        || s.getPaymentStatus() == TransactionStatus.OVERDUE
                        || s.getPaymentStatus() == TransactionStatus.PARTIALLY_PAID)
                .mapToLong(s -> s.getAmount() - s.getPaidAmount() + s.getLateFeeApplied())
                .sum();

        long overdueCount = schedules.stream()
                .filter(s -> s.getPaymentStatus() == TransactionStatus.OVERDUE)
                .count();

        // Find next due installment (earliest SCHEDULED or OVERDUE)
        InstallmentResponse nextDue = schedules.stream()
                .filter(s -> s.getPaymentStatus() == TransactionStatus.SCHEDULED
                        || s.getPaymentStatus() == TransactionStatus.OVERDUE
                        || s.getPaymentStatus() == TransactionStatus.PARTIALLY_PAID)
                .min(Comparator.comparing(PaymentRequestSchedule::getDueDate))
                .map(this::toInstallmentResponse)
                .orElse(null);

        TenantDashboardResponse response = new TenantDashboardResponse();
        // Allotment
        response.setAllotmentId(allotment.getAllotmentId());
        response.setRoomNumber(allotment.getRoom().getRoomNumber());
        response.setHostelName(allotment.getRoom().getHostel().getHostelName());
        response.setHostelAddress(allotment.getRoom().getHostel().getHostelAddress());
        response.setFloorNumber(allotment.getRoom().getFloor().getFloorNumber());
        response.setAllotmentStatus(allotment.getRoomAllotmentStatus().name());
        response.setAllotmentDate(allotment.getStartDate());
        // Plan
        response.setPlanId(plan.getPlanId());
        response.setAgreementId(plan.getAgreementId());
        response.setInstallmentAmount(plan.getInstallmentAmount());
        response.setPaymentFrequency(plan.getPaymentFrequency().name());
        response.setStartDate(plan.getStartDate());
        response.setEndDate(plan.getEndDate());
        response.setPendingInstallments(plan.getPendingInstallments());
        response.setTotalPaid(totalPaid);
        response.setTotalPending(totalPending);
        response.setOverdueCount((int) overdueCount);
        response.setNextDueInstallment(nextDue);
        return response;
    }

    /**
     * Returns collection summary for all tenants under the given owner.
     */
    public OwnerCollectionSummaryResponse getOwnerCollectionSummary(UUID ownerId) {
        // Get all active and upcoming allotments for hostels owned by this owner.
        // UPCOMING covers tenants who have already activated and paid (incl. first
        // installment) but whose agreement start date is in the future.
        List<RoomAllotment> allotments = roomAllotmentRepository
                .findByRoom_Hostel_Owner_UserIdAndRoomAllotmentStatusIn(
                        ownerId, java.util.List.of(
                                com.krunity.HostelManagment.enums.RoomAllotmentStatus.ACTIVE,
                                com.krunity.HostelManagment.enums.RoomAllotmentStatus.UPCOMING));

        long totalCollected = 0L;
        long totalPending = 0L;
        long totalOverdue = 0L;
        int overdueTenantsCount = 0;

        List<OwnerCollectionSummaryResponse.TenantCollectionRow> rows = new ArrayList<>();

        for (RoomAllotment allotment : allotments) {
            UUID tenantId = allotment.getTenant().getUserId();

            TenantPaymentPlan plan = paymentPlanRepository
                    .findByTenant_UserIdAndIsActiveTrue(tenantId)
                    .orElse(null);

            if (plan == null) continue;

            List<PaymentRequestSchedule> schedules =
                    scheduleRepository.findByTenantPaymentPlan_PlanIdOrderByInstallmentNumber(plan.getPlanId());

            long tenantPaid = schedules.stream()
                    .filter(s -> s.getPaymentStatus() == TransactionStatus.COMPLETED)
                    .mapToLong(PaymentRequestSchedule::getPaidAmount).sum();

            long tenantPending = schedules.stream()
                    .filter(s -> s.getPaymentStatus() == TransactionStatus.SCHEDULED
                            || s.getPaymentStatus() == TransactionStatus.PARTIALLY_PAID)
                    .mapToLong(s -> s.getAmount() - s.getPaidAmount()).sum();

            List<PaymentRequestSchedule> overdueSchedules = schedules.stream()
                    .filter(s -> s.getPaymentStatus() == TransactionStatus.OVERDUE)
                    .toList();

            long tenantOverdue = overdueSchedules.stream()
                    .mapToLong(s -> s.getAmount() - s.getPaidAmount() + s.getLateFeeApplied()).sum();

            totalCollected += tenantPaid;
            totalPending += tenantPending;
            totalOverdue += tenantOverdue;
            if (!overdueSchedules.isEmpty()) overdueTenantsCount++;

            OwnerCollectionSummaryResponse.TenantCollectionRow row =
                    new OwnerCollectionSummaryResponse.TenantCollectionRow();
            row.setTenantId(tenantId.toString());
            row.setTenantName(allotment.getTenant().getDisplayName());
            row.setRoomNumber(allotment.getRoom().getRoomNumber());
            row.setHostelName(allotment.getRoom().getHostel().getHostelName());
            row.setInstallmentAmount(plan.getInstallmentAmount());
            row.setPendingInstallments(plan.getPendingInstallments());
            row.setOverdueInstallments(overdueSchedules.size());
            row.setTotalOverdueAmount(tenantOverdue);
            row.setAgreementId(plan.getAgreementId());
            rows.add(row);
        }

        OwnerCollectionSummaryResponse response = new OwnerCollectionSummaryResponse();
        response.setTotalCollected(totalCollected);
        response.setTotalPending(totalPending);
        response.setTotalOverdue(totalOverdue);
        response.setActiveTenants(rows.size());
        response.setOverdueTenantsCount(overdueTenantsCount);
        response.setTenants(rows);
        return response;
    }

    private InstallmentResponse toInstallmentResponse(PaymentRequestSchedule s) {
        InstallmentResponse r = new InstallmentResponse();
        r.setScheduleId(s.getScheduleId());
        r.setInstallmentNumber(s.getInstallmentNumber());
        r.setAmount(s.getAmount());
        r.setDueDate(s.getDueDate());
        r.setPaymentStatus(s.getPaymentStatus().name());
        r.setPaidAmount(s.getPaidAmount());
        r.setLateFeeApplied(s.getLateFeeApplied());
        r.setPaidAt(s.getPaidAt());
        r.setTransactionId(s.getTransactionId());
        return r;
    }
}
