package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.dto.InstallmentResponse;
import com.krunity.HostelManagment.dto.OwnerCollectionSummaryResponse;
import com.krunity.HostelManagment.dto.TenantDashboardResponse;
import com.krunity.HostelManagment.enums.TransactionStatus;
import com.krunity.HostelManagment.exception.NotFoundException;
import com.krunity.HostelManagment.model.PaymentRequestSchedule;
import com.krunity.HostelManagment.model.RoomAllotment;
import com.krunity.HostelManagment.model.TenantPaymentPlan;
import com.krunity.HostelManagment.model.Agreement;
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

    @Autowired
    private TenantAgreementSelectionService tenantAgreementSelectionService;

    /**
     * Returns the full dashboard summary for the currently logged-in tenant.
     */
    public TenantDashboardResponse getTenantDashboard(UUID tenantId) {
        Agreement agreement = tenantAgreementSelectionService.selectForTenant(tenantId);
        RoomAllotment allotment = roomAllotmentRepository
                .findByTenant_UserIdAndAgreementId(tenantId, agreement.getId())
                .orElseThrow(() -> new NotFoundException("No room allotment found for selected agreement"));

        // The plan is owned by the resolved allotment. Looking up any active
        // plan by tenant is ambiguous while an accepted extension exists.
        TenantPaymentPlan plan = allotment.getPaymentPlanId();
        if (plan == null) {
            throw new NotFoundException("No payment plan found for current allotment");
        }

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
        response.setInstallments(schedules.stream().map(this::toInstallmentResponse).toList());
        return response;
    }

    /**
     * Returns collection summary for all tenants under the given owner.
     */
    public OwnerCollectionSummaryResponse getOwnerCollectionSummary(UUID ownerId) {
        // One row is deliberately returned per agreement/allotment. A tenant can
        // have a history of agreements, each with its own payment plan and ledger.
        List<RoomAllotment> allotments = roomAllotmentRepository
                .findByRoom_Hostel_Owner_UserId(ownerId);

        long totalCollected = 0L;
        long totalPending = 0L;
        long totalOverdue = 0L;
        int overdueTenantsCount = 0;

        List<OwnerCollectionSummaryResponse.TenantCollectionRow> rows = new ArrayList<>();

        for (RoomAllotment allotment : allotments.stream()
                .sorted(Comparator.comparing(RoomAllotment::getStartDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList()) {
            UUID tenantId = allotment.getTenant().getUserId();

            TenantPaymentPlan plan = allotment.getPaymentPlanId();

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
            row.setPlanId(plan.getPlanId().toString());
            row.setStartDate(plan.getStartDate());
            row.setEndDate(plan.getEndDate());
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
