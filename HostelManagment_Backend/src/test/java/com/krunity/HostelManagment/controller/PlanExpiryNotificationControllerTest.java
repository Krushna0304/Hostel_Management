package com.krunity.HostelManagment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krunity.HostelManagment.enums.NotificationType;
import com.krunity.HostelManagment.model.PlanExpiryNotification;
import com.krunity.HostelManagment.model.User;
import com.krunity.HostelManagment.service.PlanExpiryNotificationService;
import com.krunity.HostelManagment.Utils.ApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for PlanExpiryNotificationController
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(PlanExpiryNotificationController.class)
@AutoConfigureWebMvc
public class PlanExpiryNotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlanExpiryNotificationService planExpiryNotificationService;

    @Autowired
    private ObjectMapper objectMapper;

    private User mockTenant;
    private User mockOwner;
    private PlanExpiryNotification mockNotification;

    @BeforeEach
    void setUp() {
        mockTenant = new User();
        mockTenant.setUserId(UUID.randomUUID());
        mockTenant.setDisplayName("Test Tenant");

        mockOwner = new User();
        mockOwner.setUserId(UUID.randomUUID());
        mockOwner.setDisplayName("Test Owner");

        mockNotification = new PlanExpiryNotification();
        mockNotification.setNotificationId(UUID.randomUUID());
        mockNotification.setAgreementId("agreement_123");
        mockNotification.setTenant(mockTenant);
        mockNotification.setNotificationType(NotificationType.PLAN_EXPIRY_REMINDER);
        mockNotification.setScheduledDate(LocalDate.now().plusDays(7));
        mockNotification.setDeliveryStatus(PlanExpiryNotification.DeliveryStatus.PENDING);
        mockNotification.setLeadTimeDays(7);
        mockNotification.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void getTenantNotifications_Success() throws Exception {
        // Arrange
        UUID tenantId = mockTenant.getUserId();
        List<PlanExpiryNotification> notifications = Arrays.asList(mockNotification);
        
        when(planExpiryNotificationService.getTenantNotifications(any(User.class), isNull()))
                .thenReturn(notifications);

        try (MockedStatic<ApplicationContext> mockedContext = mockStatic(ApplicationContext.class)) {
            mockedContext.when(ApplicationContext::getUser).thenReturn(mockTenant);

            // Act & Assert
            mockMvc.perform(get("/api/v1/notifications/plan-expiry/{tenantId}", tenantId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.notifications").isArray())
                    .andExpect(jsonPath("$.notifications[0].notificationId").value(mockNotification.getNotificationId().toString()))
                    .andExpect(jsonPath("$.notifications[0].agreementId").value("agreement_123"))
                    .andExpect(jsonPath("$.count").value(1));

            verify(planExpiryNotificationService, times(1)).getTenantNotifications(any(User.class), isNull());
        }
    }

    @Test
    void getTenantNotifications_WithStatusFilter() throws Exception {
        // Arrange
        UUID tenantId = mockTenant.getUserId();
        List<PlanExpiryNotification> notifications = Arrays.asList(mockNotification);
        
        when(planExpiryNotificationService.getTenantNotifications(any(User.class), eq(PlanExpiryNotification.DeliveryStatus.PENDING)))
                .thenReturn(notifications);

        try (MockedStatic<ApplicationContext> mockedContext = mockStatic(ApplicationContext.class)) {
            mockedContext.when(ApplicationContext::getUser).thenReturn(mockTenant);

            // Act & Assert
            mockMvc.perform(get("/api/v1/notifications/plan-expiry/{tenantId}", tenantId)
                            .param("status", "PENDING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.notifications").isArray())
                    .andExpect(jsonPath("$.count").value(1));

            verify(planExpiryNotificationService, times(1))
                    .getTenantNotifications(any(User.class), eq(PlanExpiryNotification.DeliveryStatus.PENDING));
        }
    }

    @Test
    void getTenantNotifications_InvalidStatus_BadRequest() throws Exception {
        // Arrange
        UUID tenantId = mockTenant.getUserId();

        try (MockedStatic<ApplicationContext> mockedContext = mockStatic(ApplicationContext.class)) {
            mockedContext.when(ApplicationContext::getUser).thenReturn(mockTenant);

            // Act & Assert
            mockMvc.perform(get("/api/v1/notifications/plan-expiry/{tenantId}", tenantId)
                            .param("status", "INVALID_STATUS"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("INVALID_STATUS"));
        }
    }

    @Test
    void getAgreementNotifications_Success() throws Exception {
        // Arrange
        String agreementId = "agreement_123";
        List<PlanExpiryNotification> notifications = Arrays.asList(mockNotification);
        
        when(planExpiryNotificationService.getAgreementNotifications(agreementId))
                .thenReturn(notifications);

        try (MockedStatic<ApplicationContext> mockedContext = mockStatic(ApplicationContext.class)) {
            mockedContext.when(ApplicationContext::getUser).thenReturn(mockOwner);

            // Act & Assert
            mockMvc.perform(get("/api/v1/notifications/plan-expiry/agreement/{agreementId}", agreementId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.notifications").isArray())
                    .andExpect(jsonPath("$.notifications[0].agreementId").value(agreementId))
                    .andExpect(jsonPath("$.count").value(1));

            verify(planExpiryNotificationService, times(1)).getAgreementNotifications(agreementId);
        }
    }

    @Test
    void scheduleNotifications_Success() throws Exception {
        // Arrange
        PlanExpiryNotificationController.ScheduleNotificationRequest request = 
                new PlanExpiryNotificationController.ScheduleNotificationRequest();
        request.setAgreementId("agreement_123");
        request.setLeadTimes(Arrays.asList(30, 15, 7, 3, 1));

        when(planExpiryNotificationService.scheduleNotificationsForAgreement(anyString(), anyList()))
                .thenReturn(5);

        try (MockedStatic<ApplicationContext> mockedContext = mockStatic(ApplicationContext.class)) {
            mockedContext.when(ApplicationContext::getUser).thenReturn(mockOwner);

            // Act & Assert
            mockMvc.perform(post("/api/v1/notifications/plan-expiry/schedule")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.scheduledCount").value(5))
                    .andExpect(jsonPath("$.agreementId").value("agreement_123"));

            verify(planExpiryNotificationService, times(1))
                    .scheduleNotificationsForAgreement("agreement_123", Arrays.asList(30, 15, 7, 3, 1));
        }
    }

    @Test
    void scheduleUrgentNotification_Success() throws Exception {
        // Arrange
        PlanExpiryNotificationController.UrgentNotificationRequest request = 
                new PlanExpiryNotificationController.UrgentNotificationRequest();
        request.setAllotmentId(UUID.randomUUID());
        request.setNotificationType(NotificationType.URGENT_ACTION_REQUIRED);

        when(planExpiryNotificationService.sendUrgentNotificationForAllotment(any(UUID.class), any(NotificationType.class)))
                .thenReturn(true);

        try (MockedStatic<ApplicationContext> mockedContext = mockStatic(ApplicationContext.class)) {
            mockedContext.when(ApplicationContext::getUser).thenReturn(mockOwner);

            // Act & Assert
            mockMvc.perform(post("/api/v1/notifications/plan-expiry/schedule/urgent")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"));

            verify(planExpiryNotificationService, times(1))
                    .sendUrgentNotificationForAllotment(any(UUID.class), eq(NotificationType.URGENT_ACTION_REQUIRED));
        }
    }

    @Test
    void resendFailedNotification_Success() throws Exception {
        // Arrange
        UUID notificationId = UUID.randomUUID();
        
        when(planExpiryNotificationService.resendNotification(notificationId))
                .thenReturn(true);

        try (MockedStatic<ApplicationContext> mockedContext = mockStatic(ApplicationContext.class)) {
            mockedContext.when(ApplicationContext::getUser).thenReturn(mockOwner);

            // Act & Assert
            mockMvc.perform(put("/api/v1/notifications/plan-expiry/{id}/resend", notificationId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.notificationId").value(notificationId.toString()));

            verify(planExpiryNotificationService, times(1)).resendNotification(notificationId);
        }
    }

    @Test
    void resendFailedNotification_Failed() throws Exception {
        // Arrange
        UUID notificationId = UUID.randomUUID();
        
        when(planExpiryNotificationService.resendNotification(notificationId))
                .thenReturn(false);

        try (MockedStatic<ApplicationContext> mockedContext = mockStatic(ApplicationContext.class)) {
            mockedContext.when(ApplicationContext::getUser).thenReturn(mockOwner);

            // Act & Assert
            mockMvc.perform(put("/api/v1/notifications/plan-expiry/{id}/resend", notificationId))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"));

            verify(planExpiryNotificationService, times(1)).resendNotification(notificationId);
        }
    }

    @Test
    void cancelNotification_Success() throws Exception {
        // Arrange
        UUID notificationId = UUID.randomUUID();
        
        when(planExpiryNotificationService.cancelNotification(notificationId))
                .thenReturn(true);

        try (MockedStatic<ApplicationContext> mockedContext = mockStatic(ApplicationContext.class)) {
            mockedContext.when(ApplicationContext::getUser).thenReturn(mockOwner);

            // Act & Assert
            mockMvc.perform(put("/api/v1/notifications/plan-expiry/{id}/cancel", notificationId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.notificationId").value(notificationId.toString()));

            verify(planExpiryNotificationService, times(1)).cancelNotification(notificationId);
        }
    }

    @Test
    void cancelAgreementNotifications_Success() throws Exception {
        // Arrange
        String agreementId = "agreement_123";

        try (MockedStatic<ApplicationContext> mockedContext = mockStatic(ApplicationContext.class)) {
            mockedContext.when(ApplicationContext::getUser).thenReturn(mockOwner);

            // Act & Assert
            mockMvc.perform(put("/api/v1/notifications/plan-expiry/agreement/{agreementId}/cancel", agreementId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.agreementId").value(agreementId));

            verify(planExpiryNotificationService, times(1)).cancelNotificationsForAgreement(agreementId);
        }
    }

    @Test
    void getDeliveryStatistics_Success() throws Exception {
        // Arrange
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("deliverySuccessRate", 85.5);
        statistics.put("totalNotifications", 100);
        
        when(planExpiryNotificationService.getDeliveryStatistics())
                .thenReturn(statistics);

        try (MockedStatic<ApplicationContext> mockedContext = mockStatic(ApplicationContext.class)) {
            mockedContext.when(ApplicationContext::getUser).thenReturn(mockOwner);

            // Act & Assert
            mockMvc.perform(get("/api/v1/notifications/plan-expiry/admin/statistics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.statistics.deliverySuccessRate").value(85.5));

            verify(planExpiryNotificationService, times(1)).getDeliveryStatistics();
        }
    }

    @Test
    void manuallyProcessNotifications_Success() throws Exception {
        try (MockedStatic<ApplicationContext> mockedContext = mockStatic(ApplicationContext.class)) {
            mockedContext.when(ApplicationContext::getUser).thenReturn(mockOwner);

            // Act & Assert
            mockMvc.perform(post("/api/v1/notifications/plan-expiry/admin/process"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"));

            verify(planExpiryNotificationService, times(1)).manuallyProcessNotifications();
        }
    }

    @Test
    void getNotificationConfiguration_Success() throws Exception {
        // Arrange
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", true);
        config.put("maxRetryAttempts", 3);
        
        when(planExpiryNotificationService.getNotificationConfiguration())
                .thenReturn(config);

        try (MockedStatic<ApplicationContext> mockedContext = mockStatic(ApplicationContext.class)) {
            mockedContext.when(ApplicationContext::getUser).thenReturn(mockOwner);

            // Act & Assert
            mockMvc.perform(get("/api/v1/notifications/plan-expiry/admin/config"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.configuration.enabled").value(true));

            verify(planExpiryNotificationService, times(1)).getNotificationConfiguration();
        }
    }

    @Test
    void updateNotificationConfiguration_Success() throws Exception {
        // Arrange
        PlanExpiryNotificationController.NotificationConfigRequest request = 
                new PlanExpiryNotificationController.NotificationConfigRequest();
        request.setEnabled(true);
        request.setMaxRetryAttempts(5);

        try (MockedStatic<ApplicationContext> mockedContext = mockStatic(ApplicationContext.class)) {
            mockedContext.when(ApplicationContext::getUser).thenReturn(mockOwner);

            // Act & Assert
            mockMvc.perform(put("/api/v1/notifications/plan-expiry/admin/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"));

            verify(planExpiryNotificationService, times(1)).updateNotificationConfiguration(anyMap());
        }
    }

    @Test
    void getPendingNotificationsSummary_Success() throws Exception {
        // Arrange
        Map<String, Object> summary = new HashMap<>();
        summary.put("dueToday", 5);
        summary.put("overdue", 2);
        summary.put("totalPending", 15);
        
        when(planExpiryNotificationService.getPendingNotificationsSummary())
                .thenReturn(summary);

        try (MockedStatic<ApplicationContext> mockedContext = mockStatic(ApplicationContext.class)) {
            mockedContext.when(ApplicationContext::getUser).thenReturn(mockOwner);

            // Act & Assert
            mockMvc.perform(get("/api/v1/notifications/plan-expiry/admin/pending-summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.summary.dueToday").value(5))
                    .andExpect(jsonPath("$.summary.totalPending").value(15));

            verify(planExpiryNotificationService, times(1)).getPendingNotificationsSummary();
        }
    }
}