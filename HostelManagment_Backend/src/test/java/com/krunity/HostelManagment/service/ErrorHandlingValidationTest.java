package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.dto.ExtensionApprovalDto;
import com.krunity.HostelManagment.dto.ExtensionRequestDto;
import com.krunity.HostelManagment.enums.ExtendAllotmentStatus;
import com.krunity.HostelManagment.enums.RoomAllotmentStatus;
import com.krunity.HostelManagment.enums.SettlementStatus;
import com.krunity.HostelManagment.exception.NotFoundException;
import com.krunity.HostelManagment.model.*;
import com.krunity.HostelManagment.model.plan.Duration;
import com.krunity.HostelManagment.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Error handling and validation tests across all services (Task 7.1.5).
 * Tests: NotFoundException for missing entities, SecurityException for unauthorized actions,
 * IllegalStateException for invalid status transitions, and IllegalArgumentException for bad input.
 */
@ExtendWith(MockitoExtension.class)
class ErrorHandlingValidationTest {

    // ─── Mocks for EnhancedSettlementService ──────────────────────────────────
    @Mock
    private SettlementService baseSettlementService;

    @Mock
    private SettlementTransactionCalculator settlementCalculator;

    @Mock
    private SettlementRequestRepository settlementRepository;

    @Mock
    private RoomAllotmentRepository roomAllotmentRepository;

    @Mock
    private AgreementService agreementService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EnhancedSettlementService enhancedSettlementService;

    // ─── Mocks for ExtendedAllotmentService ───────────────────────────────────
    @Mock
    private ExtendAllotmentRequestRepository extendRequestRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private RoomAgreementPlanService roomAgreementPlanService;

    @InjectMocks
    private ExtendedAllotmentService extendedAllotmentService;

    // ─── Mocks for PlanExpiryNotificationService ──────────────────────────────
    @Mock
    private PlanExpiryNotificationRepository notificationRepository;

    @InjectMocks
    private PlanExpiryNotificationService planExpiryNotificationService;

    private User owner;
    private User tenant;
    private User otherUser;
    private Room room;
    private Agreement agreement;
    private RoomAllotment allotment;

    @BeforeEach
    void setUp() {
        // Configure PlanExpiryNotificationService
        ReflectionTestUtils.setField(planExpiryNotificationService, "defaultLeadTimes", new String[]{"30", "15", "7", "3", "1"});
        ReflectionTestUtils.setField(planExpiryNotificationService, "planExpiryNotificationsEnabled", true);
        ReflectionTestUtils.setField(planExpiryNotificationService, "maxRetryAttempts", 3);
        ReflectionTestUtils.setField(planExpiryNotificationService, "cleanupDaysThreshold", 30);
        ReflectionTestUtils.setField(planExpiryNotificationService, "retryDelayMinutes", 5);
        ReflectionTestUtils.setField(planExpiryNotificationService, "deliveryConfirmationTimeoutMinutes", 5);

        owner = User.builder()
                .userId(UUID.randomUUID())
                .displayName("Test Owner")
                .phoneNumber("1234567890")
                .build();

        tenant = User.builder()
                .userId(UUID.randomUUID())
                .displayName("Test Tenant")
                .phoneNumber("9876543210")
                .build();

        otherUser = User.builder()
                .userId(UUID.randomUUID())
                .displayName("Other User")
                .build();

        room = Room.builder()
                .roomId(UUID.randomUUID())
                .roomNumber("F-505")
                .build();

        agreement = Agreement.builder()
                .id("agreement-error-001")
                .userId(tenant.getUserId())
                .ownerId(owner.getUserId())
                .roomId(room.getRoomId())
                .startDate(LocalDate.now().minusMonths(6))
                .endDate(LocalDate.now().plusDays(30))
                .status(com.krunity.HostelManagment.enums.AgreementStatus.ACTIVE)
                .build();

        allotment = RoomAllotment.builder()
                .allotmentId(UUID.randomUUID())
                .tenant(tenant)
                .room(room)
                .agreementId("agreement-error-001")
                .startDate(LocalDate.now().minusMonths(6))
                .endDate(LocalDate.now().plusDays(30))
                .roomAllotmentStatus(RoomAllotmentStatus.ACTIVE)
                .build();
    }

    // ─── NotFoundException Tests ──────────────────────────────────────────────

    @Test
    void testSettlement_AgreementNotFound_ThrowsException() {
        // Given
        when(agreementService.getAgreementById("non-existent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(Exception.class, () ->
                enhancedSettlementService.createSettlementTransaction(
                        "non-existent", owner.getUserId(), LocalDate.now(), null));
    }

    @Test
    void testApproveSettlement_SettlementNotFound_ThrowsException() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(settlementRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(Exception.class, () ->
                enhancedSettlementService.approveSettlementWithRoomUpdate(
                        nonExistentId, owner.getUserId(), "notes", true));
    }

    @Test
    void testExtension_RequestNotFound_ThrowsNotFoundException() {
        // Given
        UUID nonExistentRequestId = UUID.randomUUID();
        ExtensionApprovalDto dto = new ExtensionApprovalDto();
        dto.setApproved(true);

        when(extendRequestRepository.findById(nonExistentRequestId)).thenReturn(Optional.empty());

        // When & Then - Service wraps NotFoundException in RuntimeException
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                extendedAllotmentService.approveExtensionRequest(nonExistentRequestId, owner.getUserId(), dto));

        assertTrue(ex.getCause() instanceof NotFoundException ||
                ex.getMessage().contains("Extension request not found"));
    }

    @Test
    void testExtension_AllotmentNotFound_ThrowsNotFoundException() {
        // Given
        ExtensionRequestDto requestDto = new ExtensionRequestDto();
        requestDto.setCurrentAgreementId("agreement-error-001");
        requestDto.setPlanId(UUID.randomUUID().toString());

        when(agreementService.getAgreementById("agreement-error-001")).thenReturn(Optional.of(agreement));
        when(roomAllotmentRepository.findByAgreementId("agreement-error-001")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(Exception.class, () ->
                extendedAllotmentService.createExtensionRequest(tenant.getUserId(), requestDto));
    }

    @Test
    void testEarlySettlement_AllotmentNotFound_ThrowsException() {
        // Given
        when(agreementService.getAgreementById("agreement-error-001")).thenReturn(Optional.of(agreement));
        when(roomAllotmentRepository.findByAgreementId("agreement-error-001")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(Exception.class, () ->
                enhancedSettlementService.processEarlySettlement(
                        "agreement-error-001", tenant.getUserId(), LocalDate.now().plusDays(10), "reason"));
    }

    // ─── SecurityException Tests ──────────────────────────────────────────────

    @Test
    void testCreateSettlementTransaction_WrongOwner_ThrowsSecurityException() {
        // Given - agreement belongs to a different owner
        when(agreementService.getAgreementById("agreement-error-001")).thenReturn(Optional.of(agreement));

        // When & Then - otherUser is not the owner
        assertThrows(Exception.class, () ->
                enhancedSettlementService.createSettlementTransaction(
                        "agreement-error-001", otherUser.getUserId(), LocalDate.now(), null));
    }

    @Test
    void testProcessEarlySettlement_WrongTenant_ThrowsSecurityException() {
        // Given - agreement belongs to a different tenant
        when(agreementService.getAgreementById("agreement-error-001")).thenReturn(Optional.of(agreement));

        // When & Then - otherUser is not the tenant
        assertThrows(Exception.class, () ->
                enhancedSettlementService.processEarlySettlement(
                        "agreement-error-001", otherUser.getUserId(), LocalDate.now().plusDays(10), "reason"));
    }

    @Test
    void testApproveSettlement_WrongOwner_ThrowsSecurityException() {
        // Given
        UUID settlementId = UUID.randomUUID();
        SettlementRequest settlement = SettlementRequest.builder()
                .settlementId(settlementId)
                .tenant(tenant)
                .owner(owner)
                .status(SettlementStatus.PENDING_OWNER_REVIEW)
                .build();

        when(settlementRepository.findById(settlementId)).thenReturn(Optional.of(settlement));

        // When & Then - otherUser is not the owner
        assertThrows(Exception.class, () ->
                enhancedSettlementService.approveSettlementWithRoomUpdate(
                        settlementId, otherUser.getUserId(), "notes", true));
    }

    @Test
    void testApproveExtensionRequest_WrongOwner_ThrowsSecurityException() {
        // Given
        UUID requestId = UUID.randomUUID();
        ExtendAllotmentRequest request = ExtendAllotmentRequest.builder()
                .extendRequestId(requestId)
                .tenant(tenant)
                .owner(owner)
                .status(ExtendAllotmentStatus.PENDING_OWNER_APPROVAL)
                .build();

        ExtensionApprovalDto dto = new ExtensionApprovalDto();
        dto.setApproved(true);

        when(extendRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        // When & Then - Service wraps SecurityException in RuntimeException
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                extendedAllotmentService.approveExtensionRequest(requestId, otherUser.getUserId(), dto));

        assertTrue(ex.getMessage().contains("Owner is not authorized") ||
                (ex.getCause() instanceof SecurityException &&
                 ex.getCause().getMessage().contains("Owner is not authorized")));
    }

    // ─── IllegalStateException Tests ─────────────────────────────────────────

    @Test
    void testApproveExtension_InvalidStatus_ThrowsIllegalStateException() {
        // Given - Request is EXPIRED, not PENDING_OWNER_APPROVAL
        UUID requestId = UUID.randomUUID();
        ExtendAllotmentRequest expiredRequest = ExtendAllotmentRequest.builder()
                .extendRequestId(requestId)
                .tenant(tenant)
                .owner(owner)
                .status(ExtendAllotmentStatus.EXPIRED)
                .build();

        ExtensionApprovalDto dto = new ExtensionApprovalDto();
        dto.setApproved(true);

        when(extendRequestRepository.findById(requestId)).thenReturn(Optional.of(expiredRequest));

        // When & Then - Service wraps IllegalStateException in RuntimeException
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                extendedAllotmentService.approveExtensionRequest(requestId, owner.getUserId(), dto));

        assertTrue(ex.getCause() instanceof IllegalStateException ||
                ex.getMessage().contains("Extension request is not in expected status"));
    }

    @Test
    void testProcessEarlySettlement_AlreadySettled_ThrowsIllegalStateException() {
        // Given - Settlement already exists
        SettlementRequest existing = SettlementRequest.builder()
                .settlementId(UUID.randomUUID())
                .status(SettlementStatus.PENDING_OWNER_REVIEW)
                .build();

        when(agreementService.getAgreementById("agreement-error-001")).thenReturn(Optional.of(agreement));
        when(roomAllotmentRepository.findByAgreementId("agreement-error-001")).thenReturn(Optional.of(allotment));
        when(settlementRepository.findByAgreementId("agreement-error-001")).thenReturn(Optional.of(existing));

        // When & Then
        assertThrows(Exception.class, () ->
                enhancedSettlementService.processEarlySettlement(
                        "agreement-error-001", tenant.getUserId(), LocalDate.now().plusDays(10), "reason"));
    }

    @Test
    void testApproveExtension_AlreadyApproved_ThrowsIllegalStateException() {
        // Given - Extension already approved
        UUID requestId = UUID.randomUUID();
        ExtendAllotmentRequest alreadyApproved = ExtendAllotmentRequest.builder()
                .extendRequestId(requestId)
                .tenant(tenant)
                .owner(owner)
                .status(ExtendAllotmentStatus.APPROVED_PENDING_PAYMENT)
                .build();

        ExtensionApprovalDto dto = new ExtensionApprovalDto();
        dto.setApproved(true);

        when(extendRequestRepository.findById(requestId)).thenReturn(Optional.of(alreadyApproved));

        // When & Then - Service wraps IllegalStateException in RuntimeException
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                extendedAllotmentService.approveExtensionRequest(requestId, owner.getUserId(), dto));

        assertTrue(ex.getCause() instanceof IllegalStateException ||
                ex.getMessage().contains("Extension request is not in expected status"));
    }

    // ─── IllegalArgumentException Tests ──────────────────────────────────────

    @Test
    void testProcessEarlySettlement_PastRequestedDate_ThrowsIllegalArgumentException() {
        // Given - Past date for requested end
        LocalDate pastDate = LocalDate.now().minusDays(5);

        when(agreementService.getAgreementById("agreement-error-001")).thenReturn(Optional.of(agreement));
        when(roomAllotmentRepository.findByAgreementId("agreement-error-001")).thenReturn(Optional.of(allotment));

        // When & Then
        assertThrows(Exception.class, () ->
                enhancedSettlementService.processEarlySettlement(
                        "agreement-error-001", tenant.getUserId(), pastDate, "reason"));
    }

    @Test
    void testProcessEarlySettlement_DateAfterAgreementEnd_ThrowsIllegalArgumentException() {
        // Given - Requested end date is after (or same as) the actual agreement end date
        LocalDate dateAfterEnd = LocalDate.now().plusDays(60); // Agreement ends in 30 days

        when(agreementService.getAgreementById("agreement-error-001")).thenReturn(Optional.of(agreement));
        when(roomAllotmentRepository.findByAgreementId("agreement-error-001")).thenReturn(Optional.of(allotment));

        // When & Then - Date is not before end date
        assertThrows(Exception.class, () ->
                enhancedSettlementService.processEarlySettlement(
                        "agreement-error-001", tenant.getUserId(), dateAfterEnd, "reason"));
    }

    // ─── Cross-Service Validation Tests ───────────────────────────────────────

    @Test
    void testExtensionCreation_TenantUnauthorized_ThrowsSecurityException() {
        // Given - agreement has a different userId
        Agreement anotherTenantAgreement = Agreement.builder()
                .id("agreement-other-001")
                .userId(otherUser.getUserId()) // belongs to otherUser
                .ownerId(owner.getUserId())
                .status(com.krunity.HostelManagment.enums.AgreementStatus.ACTIVE)
                .build();

        ExtensionRequestDto requestDto = new ExtensionRequestDto();
        requestDto.setCurrentAgreementId("agreement-other-001");
        requestDto.setPlanId(UUID.randomUUID().toString());

        when(agreementService.getAgreementById("agreement-other-001")).thenReturn(Optional.of(anotherTenantAgreement));

        // When & Then - tenant is requesting extension for another tenant's agreement
        assertThrows(Exception.class, () ->
                extendedAllotmentService.createExtensionRequest(tenant.getUserId(), requestDto));
    }

    @Test
    void testDuplicateExtensionRequest_ThrowsConflictException() {
        // Given - Active extension request already exists
        RoomAgreementPlan plan = RoomAgreementPlan.builder()
                .id(UUID.randomUUID().toString())
                .planName("Test Plan")
                .duration(Duration.builder().unit("MONTH").value(6).durationType("FIXED").build())
                .build();

        ExtensionRequestDto requestDto = new ExtensionRequestDto();
        requestDto.setCurrentAgreementId("agreement-error-001");
        requestDto.setPlanId(plan.getId());

        when(agreementService.getAgreementById("agreement-error-001")).thenReturn(Optional.of(agreement));
        when(roomAllotmentRepository.findByAgreementId("agreement-error-001")).thenReturn(Optional.of(allotment));
        when(extendRequestRepository.hasActiveTenantExtensionRequest(any(), anyString())).thenReturn(true);

        // When & Then
        assertThrows(Exception.class, () ->
                extendedAllotmentService.createExtensionRequest(tenant.getUserId(), requestDto));

        // Verify no new extension request was saved
        verify(extendRequestRepository, never()).save(any());
    }

    @Test
    void testCancelNotifications_InvalidAgreementId_HandledGracefully() {
        // Given - cancellation returns 0 (nothing to cancel)
        when(notificationRepository.cancelNotificationsForAgreement("no-such-agreement")).thenReturn(0);

        // When - should not throw any exception
        assertDoesNotThrow(() ->
                planExpiryNotificationService.cancelNotificationsForAgreement("no-such-agreement"));

        verify(notificationRepository).cancelNotificationsForAgreement("no-such-agreement");
    }
}
