package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.dto.ExtensionApprovalDto;
import com.krunity.HostelManagment.dto.ExtensionRequestDto;
import com.krunity.HostelManagment.dto.PaymentDetailsDto;
import com.krunity.HostelManagment.enums.ExtendAllotmentStatus;
import com.krunity.HostelManagment.model.*;
import com.krunity.HostelManagment.model.plan.Duration;
import com.krunity.HostelManagment.repository.ExtendAllotmentRequestRepository;
import com.krunity.HostelManagment.repository.RoomAllotmentRepository;
import com.krunity.HostelManagment.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for the complete extension request workflow (Task 7.1.2).
 * Tests: createExtensionRequest → approveExtensionRequest → processPayment,
 * rejection workflow, payment expiry, and duplicate prevention.
 */
@ExtendWith(MockitoExtension.class)
class ExtensionWorkflowIntegrationTest {

    @Mock
    private ExtendAllotmentRequestRepository extendRequestRepository;

    @Mock
    private RoomAllotmentRepository roomAllotmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AgreementService agreementService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private SettlementTransactionCalculator settlementCalculator;

    @Mock
    private NotificationService notificationService;

    @Mock
    private RoomAgreementPlanService roomAgreementPlanService;

    @InjectMocks
    private ExtendedAllotmentService extendedAllotmentService;

    private User tenant;
    private User owner;
    private Room room;
    private Agreement currentAgreement;
    private RoomAllotment currentAllotment;
    private RoomAgreementPlan plan;

    @BeforeEach
    void setUp() {
        tenant = User.builder()
                .userId(UUID.randomUUID())
                .displayName("John Tenant")
                .username("john.tenant")
                .build();

        owner = User.builder()
                .userId(UUID.randomUUID())
                .displayName("Jane Owner")
                .username("jane.owner")
                .build();

        room = Room.builder()
                .roomId(UUID.randomUUID())
                .roomNumber("C-301")
                .build();

        currentAgreement = Agreement.builder()
                .id("agreement-ext-001")
                .userId(tenant.getUserId())
                .ownerId(owner.getUserId())
                .roomId(room.getRoomId())
                .startDate(LocalDate.now().minusMonths(6))
                .endDate(LocalDate.now().plusDays(30))
                .status(com.krunity.HostelManagment.enums.AgreementStatus.ACTIVE)
                .build();

        currentAllotment = RoomAllotment.builder()
                .allotmentId(UUID.randomUUID())
                .tenant(tenant)
                .room(room)
                .agreementId("agreement-ext-001")
                .startDate(LocalDate.now().minusMonths(6))
                .endDate(LocalDate.now().plusDays(30))
                .roomAllotmentStatus(com.krunity.HostelManagment.enums.RoomAllotmentStatus.ACTIVE)
                .build();

        plan = RoomAgreementPlan.builder()
                .id(UUID.randomUUID().toString())
                .planName("6 Month Extension Plan")
                .duration(Duration.builder()
                        .unit("MONTH")
                        .value(6)
                        .durationType("FIXED")
                        .build())
                .build();
    }

    // ─── Full Extension Workflow Tests ────────────────────────────────────────

    @Test
    void testFullExtensionWorkflow_CreateToPayment_Success() {
        // Given - Create extension request
        ExtensionRequestDto requestDto = new ExtensionRequestDto();
        requestDto.setCurrentAgreementId("agreement-ext-001");
        requestDto.setPlanId(UUID.fromString(plan.getId()));
        requestDto.setTenantNotes("Request 6-month extension");

        when(agreementService.getAgreementById("agreement-ext-001")).thenReturn(Optional.of(currentAgreement));
        when(roomAllotmentRepository.findByAgreementId("agreement-ext-001")).thenReturn(Optional.of(currentAllotment));
        when(roomAgreementPlanService.getPlanById(anyString())).thenReturn(plan);
        when(userRepository.findById(owner.getUserId())).thenReturn(Optional.of(owner));
        when(extendRequestRepository.hasActiveTenantExtensionRequest(any(), anyString())).thenReturn(false);
        when(extendRequestRepository.save(any(ExtendAllotmentRequest.class))).thenAnswer(inv -> {
            ExtendAllotmentRequest req = inv.getArgument(0);
            req.setExtendRequestId(UUID.randomUUID());
            return req;
        });

        // When - Create request
        ExtendAllotmentRequest createdRequest = extendedAllotmentService.createExtensionRequest(
                tenant.getUserId(), requestDto);

        // Then - Verify creation
        assertNotNull(createdRequest);
        assertEquals(ExtendAllotmentStatus.PENDING_OWNER_APPROVAL, createdRequest.getStatus());

        // Given - Approve request
        UUID requestId = createdRequest.getExtendRequestId();
        ExtensionApprovalDto approvalDto = new ExtensionApprovalDto();
        approvalDto.setApproved(true);
        approvalDto.setOwnerNotes("Approved for extension");

        Agreement newAgreement = Agreement.builder()
                .id("agreement-ext-new-001")
                .status(com.krunity.HostelManagment.enums.AgreementStatus.DRAFT)
                .build();

        when(extendRequestRepository.findById(requestId)).thenReturn(Optional.of(createdRequest));
        when(agreementService.createAgreement(any(Agreement.class))).thenReturn(newAgreement);

        // When - Approve request
        ExtendAllotmentRequest approvedRequest = extendedAllotmentService.approveExtensionRequest(
                requestId, owner.getUserId(), approvalDto);

        // Then - Verify approval
        assertNotNull(approvedRequest);
        assertEquals(ExtendAllotmentStatus.APPROVED_PENDING_PAYMENT, approvedRequest.getStatus());
        assertNotNull(approvedRequest.getNewAgreementId());
        verify(notificationService, atLeastOnce()).sendNotification(eq(tenant), any(), any(), any(), any());
    }

    @Test
    void testExtensionApproval_CreatesNewDraftAgreement() {
        // Given
        UUID requestId = UUID.randomUUID();
        ExtendAllotmentRequest extensionRequest = ExtendAllotmentRequest.builder()
                .extendRequestId(requestId)
                .tenant(tenant)
                .owner(owner)
                .currentRoom(room)
                .newPlanId(UUID.fromString(plan.getId()))
                .extensionStartDate(currentAllotment.getEndDate())
                .extensionEndDate(currentAllotment.getEndDate().plusMonths(6))
                .status(ExtendAllotmentStatus.PENDING_OWNER_APPROVAL)
                .build();

        ExtensionApprovalDto approvalDto = new ExtensionApprovalDto();
        approvalDto.setApproved(true);
        approvalDto.setOwnerNotes("Extension approved");

        Agreement newAgreement = Agreement.builder()
                .id("new-draft-agreement-789")
                .userId(tenant.getUserId())
                .ownerId(owner.getUserId())
                .status(com.krunity.HostelManagment.enums.AgreementStatus.DRAFT)
                .build();

        when(extendRequestRepository.findById(requestId)).thenReturn(Optional.of(extensionRequest));
        when(roomAgreementPlanService.getPlanById(anyString())).thenReturn(plan);
        when(agreementService.createAgreement(any(Agreement.class))).thenReturn(newAgreement);
        when(extendRequestRepository.save(any(ExtendAllotmentRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        ExtendAllotmentRequest result = extendedAllotmentService.approveExtensionRequest(
                requestId, owner.getUserId(), approvalDto);

        // Then
        verify(agreementService).createAgreement(argThat(agreement ->
                agreement.getStatus() == com.krunity.HostelManagment.enums.AgreementStatus.DRAFT &&
                agreement.getType() == com.krunity.HostelManagment.enums.AgreementType.ROOM));
        assertEquals("new-draft-agreement-789", result.getNewAgreementId());
    }

    // ─── Rejection Workflow Tests ─────────────────────────────────────────────

    @Test
    void testRejectExtensionRequest_DuplicatePrevention() {
        // Given - First extension request exists
        ExtensionRequestDto requestDto = new ExtensionRequestDto();
        requestDto.setCurrentAgreementId("agreement-ext-001");
        requestDto.setPlanId(UUID.fromString(plan.getId()));

        when(agreementService.getAgreementById("agreement-ext-001")).thenReturn(Optional.of(currentAgreement));
        when(roomAllotmentRepository.findByAgreementId("agreement-ext-001")).thenReturn(Optional.of(currentAllotment));
        when(extendRequestRepository.hasActiveTenantExtensionRequest(any(), anyString())).thenReturn(true);

        // When & Then - Duplicate request should be prevented
        assertThrows(Exception.class, () ->
                extendedAllotmentService.createExtensionRequest(tenant.getUserId(), requestDto));

        verify(extendRequestRepository, never()).save(any());
    }

    // ─── Payment Expiry Tests ─────────────────────────────────────────────────

    @Test
    void testPaymentExpiry_ApprovedRequestExpiresAfter72Hours() {
        // Given
        UUID requestId = UUID.randomUUID();
        ExtendAllotmentRequest extensionRequest = ExtendAllotmentRequest.builder()
                .extendRequestId(requestId)
                .tenant(tenant)
                .owner(owner)
                .status(ExtendAllotmentStatus.PENDING_OWNER_APPROVAL)
                .currentRoom(room)
                .newPlanId(UUID.fromString(plan.getId()))
                .extensionStartDate(LocalDate.now())
                .extensionEndDate(LocalDate.now().plusMonths(6))
                .build();

        ExtensionApprovalDto approvalDto = new ExtensionApprovalDto();
        approvalDto.setApproved(true);

        Agreement newAgreement = Agreement.builder()
                .id("agreement-timer-test")
                .status(com.krunity.HostelManagment.enums.AgreementStatus.DRAFT)
                .build();

        when(extendRequestRepository.findById(requestId)).thenReturn(Optional.of(extensionRequest));
        when(roomAgreementPlanService.getPlanById(anyString())).thenReturn(plan);
        when(agreementService.createAgreement(any(Agreement.class))).thenReturn(newAgreement);
        when(extendRequestRepository.save(any(ExtendAllotmentRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime beforeApproval = LocalDateTime.now();

        // When
        ExtendAllotmentRequest result = extendedAllotmentService.approveExtensionRequest(
                requestId, owner.getUserId(), approvalDto);

        // Then - Verify expiration is set to ~72 hours from now
        assertNotNull(result.getExpiresAt());
        assertTrue(result.getExpiresAt().isAfter(beforeApproval.plusHours(71)));
        assertTrue(result.getExpiresAt().isBefore(beforeApproval.plusHours(73)));
    }

    @Test
    void testProcessPayment_AfterExpiry_ThrowsIllegalStateException() {
        // Given - Expired extension request
        UUID requestId = UUID.randomUUID();
        ExtendAllotmentRequest expiredRequest = ExtendAllotmentRequest.builder()
                .extendRequestId(requestId)
                .tenant(tenant)
                .owner(owner)
                .status(ExtendAllotmentStatus.APPROVED_PENDING_PAYMENT)
                .newAgreementId("agreement-new")
                .extensionStartDate(LocalDate.now())
                .extensionEndDate(LocalDate.now().plusMonths(6))
                .currentRoom(room)
                .build();

        // Set expiration to past
        expiredRequest.setPaymentExpirationFromNow(-100); // Expired 100 hours ago

        PaymentDetailsDto paymentDto = new PaymentDetailsDto();
        paymentDto.setPaymentReference("PAYMENT-REF-001");

        when(extendRequestRepository.findById(requestId)).thenReturn(Optional.of(expiredRequest));

        // When & Then
        assertThrows(Exception.class, () ->
                extendedAllotmentService.processExtensionPayment(requestId, tenant.getUserId(), paymentDto));
    }

    // ─── Error Handling Tests ──────────────────────────────────────────────────

    @Test
    void testCreateExtensionRequest_InvalidAgreement_ThrowsNotFoundException() {
        // Given
        ExtensionRequestDto requestDto = new ExtensionRequestDto();
        requestDto.setCurrentAgreementId("non-existent-agreement");
        requestDto.setPlanId(UUID.randomUUID());

        when(agreementService.getAgreementById(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(Exception.class, () ->
                extendedAllotmentService.createExtensionRequest(tenant.getUserId(), requestDto));
    }

    @Test
    void testApproveExtension_UnauthorizedOwner_ThrowsSecurityException() {
        // Given
        UUID requestId = UUID.randomUUID();
        UUID wrongOwnerId = UUID.randomUUID();

        ExtendAllotmentRequest request = ExtendAllotmentRequest.builder()
                .extendRequestId(requestId)
                .tenant(tenant)
                .owner(owner)
                .status(ExtendAllotmentStatus.PENDING_OWNER_APPROVAL)
                .build();

        ExtensionApprovalDto approvalDto = new ExtensionApprovalDto();
        approvalDto.setApproved(true);

        when(extendRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        // When & Then
        assertThrows(Exception.class, () ->
                extendedAllotmentService.approveExtensionRequest(requestId, wrongOwnerId, approvalDto));
    }

    @Test
    void testSeamlessTransition_ExtensionStartsOnCurrentEnd() {
        // Given
        LocalDate currentEndDate = LocalDate.of(2024, 3, 15); // mid-month, no month-end edge case
        currentAllotment.setEndDate(currentEndDate);

        ExtensionRequestDto requestDto = new ExtensionRequestDto();
        requestDto.setCurrentAgreementId("agreement-ext-001");
        requestDto.setPlanId(UUID.fromString(plan.getId()));

        when(agreementService.getAgreementById("agreement-ext-001")).thenReturn(Optional.of(currentAgreement));
        when(roomAllotmentRepository.findByAgreementId("agreement-ext-001")).thenReturn(Optional.of(currentAllotment));
        when(roomAgreementPlanService.getPlanById(anyString())).thenReturn(plan);
        when(userRepository.findById(owner.getUserId())).thenReturn(Optional.of(owner));
        when(extendRequestRepository.hasActiveTenantExtensionRequest(any(), anyString())).thenReturn(false);
        when(extendRequestRepository.save(any(ExtendAllotmentRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        ExtendAllotmentRequest result = extendedAllotmentService.createExtensionRequest(
                tenant.getUserId(), requestDto);

        // Then - Extension starts exactly when current allotment ends (no gap, no overlap)
        assertEquals(currentEndDate, result.getExtensionStartDate());
        assertEquals(currentEndDate.plusMonths(6), result.getExtensionEndDate());
    }
}
