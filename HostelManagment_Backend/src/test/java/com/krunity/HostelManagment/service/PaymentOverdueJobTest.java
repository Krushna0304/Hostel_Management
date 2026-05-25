package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.enums.TransactionStatus;
import com.krunity.HostelManagment.model.PaymentRequestSchedule;
import com.krunity.HostelManagment.model.TenantPaymentPlan;
import com.krunity.HostelManagment.repository.PaymentRequestScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test class for PaymentOverdueJob
 * Tests the overdue payment marking functionality
 */
public class PaymentOverdueJobTest {

    @Mock
    private PaymentRequestScheduleRepository scheduleRepository;

    @InjectMocks
    private PaymentOverdueJob paymentOverdueJob;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testMarkOverdueInstallments_ShouldMarkScheduledPaymentsAsOverdue() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate pastDueDate = today.minusDays(5);

        PaymentRequestSchedule overdueSchedule = new PaymentRequestSchedule();
        overdueSchedule.setScheduleId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        overdueSchedule.setDueDate(pastDueDate);
        overdueSchedule.setPaymentStatus(TransactionStatus.SCHEDULED);
        overdueSchedule.setAmount(5000L);
        overdueSchedule.setPaidAmount(0L);
        
        TenantPaymentPlan plan = new TenantPaymentPlan();
        plan.setAgreementId("test-agreement-1");
        overdueSchedule.setTenantPaymentPlan(plan);

        List<PaymentRequestSchedule> overdueSchedules = Arrays.asList(overdueSchedule);

        when(scheduleRepository.findByPaymentStatusAndDueDateBefore(
            eq(TransactionStatus.SCHEDULED), 
            eq(today)
        )).thenReturn(overdueSchedules);

        // Act
        paymentOverdueJob.markOverdueInstallments();

        // Assert
        verify(scheduleRepository).findByPaymentStatusAndDueDateBefore(
            TransactionStatus.SCHEDULED, 
            today
        );
        verify(scheduleRepository).saveAll(overdueSchedules);
        
        // Verify the schedule was marked as overdue
        assert overdueSchedule.getPaymentStatus() == TransactionStatus.OVERDUE;
    }

    @Test
    void testMarkOverdueInstallments_ShouldHandleEmptyList() {
        // Arrange
        LocalDate today = LocalDate.now();
        when(scheduleRepository.findByPaymentStatusAndDueDateBefore(
            eq(TransactionStatus.SCHEDULED), 
            eq(today)
        )).thenReturn(Arrays.asList());

        // Act
        paymentOverdueJob.markOverdueInstallments();

        // Assert
        verify(scheduleRepository).findByPaymentStatusAndDueDateBefore(
            TransactionStatus.SCHEDULED, 
            today
        );
        verify(scheduleRepository, never()).saveAll(any());
    }
}