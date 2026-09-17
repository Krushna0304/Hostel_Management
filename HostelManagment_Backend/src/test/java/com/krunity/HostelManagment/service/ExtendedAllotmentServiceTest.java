package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.dto.ExtensionApprovalDto;
import com.krunity.HostelManagment.dto.ExtensionRequestDto;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test class for ExtendedAllotmentService owner approval process functionality
 */
@ExtendWith(MockitoExtension.class)
class ExtendedAllotmentServiceTest {

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
    private ExtendAllotmentRequest extensionRequest;
    private RoomAgreementPlan plan;

    @BeforeEach
    void setUp() {
        // Set up test data
        tenant = User.builder()
                .userId(UUID.randomUUID())
                .displayName("John Doe")
                .username("john.doe")
                .build();

        owner = User.builder()
                .userId(UUID.randomUUID())
                .displayName("Jane Smith")
                .username("jane.smith")
                .build();

        room = Room.builder()
                .roomId(UUID.randomUUID())
                .roomNumber("A-101")
                .build();

        currentAgreement = Agreement.builder()
                .id("agreement-123")
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
                .agreementId("agreement-123")
                .startDate(LocalDate.now().minusMonths(6))
                .endDate(LocalDate.now().plusDays(30))
                .roomAllotmentStatus(com.krunity.HostelManagment.enums.RoomAllotmentStatus.ACTIVE)
                .build();

        plan = RoomAgreementPlan.builder()
                .id(UUID.randomUUID().toString())
                .planName("Standard Plan")
                .duration(Duration.builder()
                        .unit("MONTH")
                        .value(6)
                        .durationType("FIXED")
                        .build())
                .build();

        extensionRequest = ExtendAllotmentRequest.builder()
                .extendRequestId(UUID.randomUUID())
                .tenant(tenant)
                .owner(owner)
                .currentAgreementId("agreement-123")
                .currentRoom(room)
                .newPlanId(plan.getId())
                .extensionStartDate(LocalDate.now().plusDays(30))
                .extensionEndDate(LocalDate.now().plusDays(210)) // 6 months extension
                .status(ExtendAllotmentStatus.PENDING_OWNER_APPROVAL)
                .activationAmount(BigDecimal.valueOf(5000))
                .settlementAdjustment(BigDecimal.valueOf(-500))
                .totalAmount(BigDecimal.valueOf(4500))
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Extension Request Creation Tests with Seamless Date Calculation
    // ═══════════════════════════════════════════════════════════════════════════════

    @Test
    void testCreateExtensionRequest_SeamlessTransition_MonthlyPlan() {
        // Given
        LocalDate currentEndDate = LocalDate.of(2024, 3, 15);
        
        // Update the existing currentAllotment with the test end date
        currentAllotment.setEndDate(currentEndDate);
        
        ExtensionRequestDto requestDto = new ExtensionRequestDto();
        requestDto.setCurrentAgreementId("agreement-123");
        requestDto.setPlanId(plan.getId());
        requestDto.setTenantNotes("Request 6-month extension");

        // Setup plan with 6-month duration
        plan.setDuration(Duration.builder()
                .unit("MONTH")
                .value(6)
                .durationType("FIXED")
                .build());

        // Mock dependencies - return the updated currentAllotment
        when(agreementService.getAgreementById(anyString())).thenReturn(Optional.of(currentAgreement));
        when(roomAllotmentRepository.findByAgreementId(anyString())).thenReturn(Optional.of(currentAllotment));
        when(roomAgreementPlanService.getPlanById(anyString())).thenReturn(plan);
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(owner));
        when(extendRequestRepository.hasActiveTenantExtensionRequest(any(), anyString())).thenReturn(false);
        when(extendRequestRepository.save(any(ExtendAllotmentRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ExtendAllotmentRequest result = extendedAllotmentService.createExtensionRequest(tenant.getUserId(), requestDto);

        // Then - Verify seamless transition (no gap, no overlap)
        assertNotNull(result);
        assertEquals(currentEndDate, result.getExtensionStartDate()); // Starts exactly when current ends
        assertEquals(currentEndDate.plusMonths(6), result.getExtensionEndDate()); // 6 months from start
        
        verify(extendRequestRepository).save(argThat(request -> 
            request.getExtensionStartDate().equals(currentEndDate) &&
            request.getExtensionEndDate().equals(currentEndDate.plusMonths(6))
        ));
    }

    @Test
    void testOwnerApprovalProcess_Success() {
        // Given
        UUID requestId = extensionRequest.getExtendRequestId();
        UUID ownerId = owner.getUserId();
        
        ExtensionApprovalDto approvalDto = new ExtensionApprovalDto();
        approvalDto.setApproved(true);
        approvalDto.setOwnerNotes("Extension approved for good tenant");
        approvalDto.setFinalActivationAmount(BigDecimal.valueOf(5200));

        // Mock repository calls
        when(extendRequestRepository.findById(requestId)).thenReturn(Optional.of(extensionRequest));
        when(roomAgreementPlanService.getPlanById(anyString())).thenReturn(plan);
        
        // Mock agreement creation
        Agreement newAgreement = Agreement.builder()
                .id("agreement-new-456")
                .userId(tenant.getUserId())
                .ownerId(owner.getUserId())
                .roomId(room.getRoomId())
                .startDate(extensionRequest.getExtensionStartDate())
                .endDate(extensionRequest.getExtensionEndDate())
                .status(com.krunity.HostelManagment.enums.AgreementStatus.DRAFT)
                .build();
        
        when(agreementService.createAgreement(any(Agreement.class))).thenReturn(newAgreement);
        when(extendRequestRepository.save(any(ExtendAllotmentRequest.class))).thenReturn(extensionRequest);

        // When
        ExtendAllotmentRequest result = extendedAllotmentService.approveExtensionRequest(
                requestId, ownerId, approvalDto);

        // Then
        assertNotNull(result);
        assertEquals(ExtendAllotmentStatus.APPROVED_PENDING_PAYMENT, result.getStatus());
        assertEquals("Extension approved for good tenant", result.getOwnerNotes());
        assertEquals(BigDecimal.valueOf(5200), result.getActivationAmount());
        assertEquals("agreement-new-456", result.getNewAgreementId());
        assertNotNull(result.getApprovedAt());
        assertNotNull(result.getExpiresAt());

        // Verify repository interactions
        verify(extendRequestRepository).findById(requestId);
        verify(extendRequestRepository).save(extensionRequest);
        verify(agreementService).createAgreement(any(Agreement.class));
        verify(notificationService).sendNotification(
                eq(tenant), 
                any(), 
                anyString(), 
                anyString(), 
                any()
        );
    }

    @Test
    void testOwnerApprovalProcess_AgreementCreation() {
        // Given
        UUID requestId = extensionRequest.getExtendRequestId();
        UUID ownerId = owner.getUserId();
        
        ExtensionApprovalDto approvalDto = new ExtensionApprovalDto();
        approvalDto.setApproved(true);

        when(extendRequestRepository.findById(requestId)).thenReturn(Optional.of(extensionRequest));
        when(roomAgreementPlanService.getPlanById(anyString())).thenReturn(plan);
        
        // Mock successful agreement creation
        Agreement draftAgreement = Agreement.builder()
                .id("new-agreement-789")
                .userId(tenant.getUserId())
                .ownerId(owner.getUserId())
                .roomId(room.getRoomId())
                .planId(plan.getId())
                .planSnapshot(plan)
                .startDate(extensionRequest.getExtensionStartDate())
                .endDate(extensionRequest.getExtensionEndDate())
                .status(com.krunity.HostelManagment.enums.AgreementStatus.DRAFT)
                .build();
        
        when(agreementService.createAgreement(any(Agreement.class))).thenReturn(draftAgreement);
        when(extendRequestRepository.save(any(ExtendAllotmentRequest.class))).thenReturn(extensionRequest);

        // When
        ExtendAllotmentRequest result = extendedAllotmentService.approveExtensionRequest(
                requestId, ownerId, approvalDto);

        // Then - Verify agreement was created correctly
        verify(agreementService).createAgreement(argThat(agreement -> 
                agreement.getType() == com.krunity.HostelManagment.enums.AgreementType.ROOM &&
                agreement.getUserId().equals(tenant.getUserId()) &&
                agreement.getOwnerId().equals(owner.getUserId()) &&
                agreement.getRoomId().equals(room.getRoomId()) &&
                agreement.getPlanId().equals(plan.getId()) &&
                agreement.getStartDate().equals(extensionRequest.getExtensionStartDate()) &&
                agreement.getEndDate().equals(extensionRequest.getExtensionEndDate()) &&
                agreement.getStatus() == com.krunity.HostelManagment.enums.AgreementStatus.DRAFT &&
                agreement.getPlanSnapshot().equals(plan)
        ));

        // Verify extension request is updated with new agreement ID
        assertEquals("new-agreement-789", result.getNewAgreementId());
    }

    @Test
    void testOwnerApprovalProcess_ExpirationTimer() {
        // Given
        UUID requestId = extensionRequest.getExtendRequestId();
        UUID ownerId = owner.getUserId();
        
        ExtensionApprovalDto approvalDto = new ExtensionApprovalDto();
        approvalDto.setApproved(true);

        when(extendRequestRepository.findById(requestId)).thenReturn(Optional.of(extensionRequest));
        when(roomAgreementPlanService.getPlanById(anyString())).thenReturn(plan);
        
        Agreement newAgreement = Agreement.builder()
                .id("agreement-timer-test")
                .status(com.krunity.HostelManagment.enums.AgreementStatus.DRAFT)
                .build();
        
        when(agreementService.createAgreement(any(Agreement.class))).thenReturn(newAgreement);
        when(extendRequestRepository.save(any(ExtendAllotmentRequest.class))).thenReturn(extensionRequest);

        LocalDateTime beforeApproval = LocalDateTime.now();

        // When
        ExtendAllotmentRequest result = extendedAllotmentService.approveExtensionRequest(
                requestId, ownerId, approvalDto);

        // Then - Verify expiration timer is set (72 hours from approval)
        assertNotNull(result.getExpiresAt());
        assertTrue(result.getExpiresAt().isAfter(beforeApproval.plusHours(71))); // Within 72 hours
        assertTrue(result.getExpiresAt().isBefore(beforeApproval.plusHours(73))); // Within 72 hours
        
        // Verify it's not expired immediately after approval
        assertFalse(result.isExpired());
        
        // Verify payment can be processed
        assertTrue(result.canProcessPayment());
    }

    @Test
    void testOwnerApprovalProcess_NotificationSent() {
        // Given
        UUID requestId = extensionRequest.getExtendRequestId();
        UUID ownerId = owner.getUserId();
        
        ExtensionApprovalDto approvalDto = new ExtensionApprovalDto();
        approvalDto.setApproved(true);

        when(extendRequestRepository.findById(requestId)).thenReturn(Optional.of(extensionRequest));
        when(roomAgreementPlanService.getPlanById(anyString())).thenReturn(plan);
        
        Agreement newAgreement = Agreement.builder()
                .id("agreement-notification-test")
                .status(com.krunity.HostelManagment.enums.AgreementStatus.DRAFT)
                .build();
        
        when(agreementService.createAgreement(any(Agreement.class))).thenReturn(newAgreement);
        when(extendRequestRepository.save(any(ExtendAllotmentRequest.class))).thenReturn(extensionRequest);

        // When
        ExtendAllotmentRequest result = extendedAllotmentService.approveExtensionRequest(
                requestId, ownerId, approvalDto);

        // Then - Verify tenant notification was sent
        verify(notificationService).sendNotification(
                eq(tenant), 
                eq(com.krunity.HostelManagment.enums.NotificationType.EXTENSION_REQUEST_APPROVED),
                anyString(),
                contains("extension request has been approved"),
                any()
        );
    }

    @Test
    void testOwnerApprovalProcess_UnauthorizedOwner() {
        // Given
        UUID requestId = extensionRequest.getExtendRequestId();
        UUID wrongOwnerId = UUID.randomUUID(); // Different owner
        
        ExtensionApprovalDto approvalDto = new ExtensionApprovalDto();
        approvalDto.setApproved(true);

        when(extendRequestRepository.findById(requestId)).thenReturn(Optional.of(extensionRequest));

        // When & Then
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            extendedAllotmentService.approveExtensionRequest(requestId, wrongOwnerId, approvalDto);
        });

        assertEquals("Owner is not authorized for this extension request", exception.getMessage());
        
        // Verify no agreement was created
        verify(agreementService, never()).createAgreement(any());
        
        // Verify no notification was sent
        verify(notificationService, never()).sendNotification(any(), any(), any(), any(), any());
    }

    @Test
    void testOwnerApprovalProcess_InvalidStatus() {
        // Given
        extensionRequest.updateStatus(ExtendAllotmentStatus.EXPIRED);
        
        UUID requestId = extensionRequest.getExtendRequestId();
        UUID ownerId = owner.getUserId();
        
        ExtensionApprovalDto approvalDto = new ExtensionApprovalDto();
        approvalDto.setApproved(true);

        when(extendRequestRepository.findById(requestId)).thenReturn(Optional.of(extensionRequest));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            extendedAllotmentService.approveExtensionRequest(requestId, ownerId, approvalDto);
        });

        assertEquals("Extension request is not in expected status: PENDING_OWNER_APPROVAL", 
                    exception.getMessage());
    }

    @Test
    void testOwnerApprovalProcess_RequestNotFound() {
        // Given
        UUID nonExistentRequestId = UUID.randomUUID();
        UUID ownerId = owner.getUserId();
        
        ExtensionApprovalDto approvalDto = new ExtensionApprovalDto();
        approvalDto.setApproved(true);

        when(extendRequestRepository.findById(nonExistentRequestId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(com.krunity.HostelManagment.exception.NotFoundException.class, () -> {
            extendedAllotmentService.approveExtensionRequest(nonExistentRequestId, ownerId, approvalDto);
        });
    }

    @Test
    void testOwnerApprovalProcess_AmountCalculation() {
        // Given
        UUID requestId = extensionRequest.getExtendRequestId();
        UUID ownerId = owner.getUserId();
        
        ExtensionApprovalDto approvalDto = new ExtensionApprovalDto();
        approvalDto.setApproved(true);
        approvalDto.setFinalActivationAmount(BigDecimal.valueOf(6000)); // Owner adjusts amount

        when(extendRequestRepository.findById(requestId)).thenReturn(Optional.of(extensionRequest));
        when(roomAgreementPlanService.getPlanById(anyString())).thenReturn(plan);
        
        Agreement newAgreement = Agreement.builder()
                .id("agreement-amount-test")
                .status(com.krunity.HostelManagment.enums.AgreementStatus.DRAFT)
                .build();
        
        when(agreementService.createAgreement(any(Agreement.class))).thenReturn(newAgreement);
        when(extendRequestRepository.save(any(ExtendAllotmentRequest.class))).thenReturn(extensionRequest);

        // When
        ExtendAllotmentRequest result = extendedAllotmentService.approveExtensionRequest(
                requestId, ownerId, approvalDto);

        // Then - Verify final amounts are calculated correctly
        assertEquals(BigDecimal.valueOf(6000), result.getActivationAmount());
        // Settlement adjustment remains -500, so total should be 6000 + (-500) = 5500
        assertEquals(BigDecimal.valueOf(5500), result.getTotalAmount());
    }
    @Test
    void testCreateExtensionRequest_YearlyPlan() {
        // Given
        LocalDate currentEndDate = LocalDate.of(2024, 6, 30);
        currentAllotment.setEndDate(currentEndDate);
        
        ExtensionRequestDto requestDto = new ExtensionRequestDto();
        requestDto.setCurrentAgreementId("agreement-123");
        requestDto.setPlanId(plan.getId());

        // Setup plan with 1-year duration
        plan.setDuration(Duration.builder()
                .unit("YEAR")
                .value(1)
                .durationType("FIXED")
                .build());

        // Mock dependencies
        when(agreementService.getAgreementById(anyString())).thenReturn(Optional.of(currentAgreement));
        when(roomAllotmentRepository.findByAgreementId(anyString())).thenReturn(Optional.of(currentAllotment));
        when(roomAgreementPlanService.getPlanById(anyString())).thenReturn(plan);
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(owner));
        when(extendRequestRepository.hasActiveTenantExtensionRequest(any(), anyString())).thenReturn(false);
        when(extendRequestRepository.save(any(ExtendAllotmentRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        ExtendAllotmentRequest result = extendedAllotmentService.createExtensionRequest(tenant.getUserId(), requestDto);

        // Then
        assertEquals(currentEndDate, result.getExtensionStartDate());
        assertEquals(currentEndDate.plusYears(1), result.getExtensionEndDate());
    }

    @Test
    void testCreateExtensionRequest_MonthEndEdgeCase() {
        // Given - Current agreement ends on January 31
        LocalDate currentEndDate = LocalDate.of(2024, 1, 31);
        
        // Update the existing currentAllotment with the test end date
        currentAllotment.setEndDate(currentEndDate);
        
        ExtensionRequestDto requestDto = new ExtensionRequestDto();
        requestDto.setCurrentAgreementId("agreement-123");
        requestDto.setPlanId(plan.getId());

        // Setup plan with 1-month duration
        plan.setDuration(Duration.builder()
                .unit("MONTH")
                .value(1)
                .durationType("FIXED")
                .build());

        // Mock dependencies
        when(agreementService.getAgreementById(anyString())).thenReturn(Optional.of(currentAgreement));
        when(roomAllotmentRepository.findByAgreementId(anyString())).thenReturn(Optional.of(currentAllotment));
        when(roomAgreementPlanService.getPlanById(anyString())).thenReturn(plan);
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(owner));
        when(extendRequestRepository.hasActiveTenantExtensionRequest(any(), anyString())).thenReturn(false);
        when(extendRequestRepository.save(any(ExtendAllotmentRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ExtendAllotmentRequest result = extendedAllotmentService.createExtensionRequest(tenant.getUserId(), requestDto);

        // Then - Should end on February 29 (2024 is leap year) since Jan 31 is month end
        assertEquals(currentEndDate, result.getExtensionStartDate());
        assertEquals(LocalDate.of(2024, 2, 29), result.getExtensionEndDate());
    }

    @Test
    void testCreateExtensionRequest_LeapYearHandling() {
        // Given - Current agreement ends on February 29, 2024 (leap year)
        LocalDate currentEndDate = LocalDate.of(2024, 2, 29);
        
        // Update the existing currentAllotment with the test end date
        currentAllotment.setEndDate(currentEndDate);
        
        ExtensionRequestDto requestDto = new ExtensionRequestDto();
        requestDto.setCurrentAgreementId("agreement-123");
        requestDto.setPlanId(plan.getId());

        // Setup plan with 12-month duration (to Feb 2025, non-leap year)
        plan.setDuration(Duration.builder()
                .unit("MONTH")
                .value(12)
                .durationType("FIXED")
                .build());

        // Mock dependencies
        when(agreementService.getAgreementById(anyString())).thenReturn(Optional.of(currentAgreement));
        when(roomAllotmentRepository.findByAgreementId(anyString())).thenReturn(Optional.of(currentAllotment));
        when(roomAgreementPlanService.getPlanById(anyString())).thenReturn(plan);
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(owner));
        when(extendRequestRepository.hasActiveTenantExtensionRequest(any(), anyString())).thenReturn(false);
        when(extendRequestRepository.save(any(ExtendAllotmentRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ExtendAllotmentRequest result = extendedAllotmentService.createExtensionRequest(tenant.getUserId(), requestDto);

        // Then - Should end on February 28, 2025 (non-leap year, adjust to last day of month)
        assertEquals(currentEndDate, result.getExtensionStartDate());
        assertEquals(LocalDate.of(2025, 2, 28), result.getExtensionEndDate());
    }
    @Test
    void testCreateExtensionRequest_WeeklyPlan() {
        // Given
        LocalDate currentEndDate = LocalDate.of(2024, 3, 15);
        currentAllotment.setEndDate(currentEndDate);
        
        ExtensionRequestDto requestDto = new ExtensionRequestDto();
        requestDto.setCurrentAgreementId("agreement-123");
        requestDto.setPlanId(plan.getId());

        // Setup plan with 4-week duration
        plan.setDuration(Duration.builder()
                .unit("WEEK")
                .value(4)
                .durationType("FIXED")
                .build());

        // Mock dependencies
        when(agreementService.getAgreementById(anyString())).thenReturn(Optional.of(currentAgreement));
        when(roomAllotmentRepository.findByAgreementId(anyString())).thenReturn(Optional.of(currentAllotment));
        when(roomAgreementPlanService.getPlanById(anyString())).thenReturn(plan);
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(owner));
        when(extendRequestRepository.hasActiveTenantExtensionRequest(any(), anyString())).thenReturn(false);
        when(extendRequestRepository.save(any(ExtendAllotmentRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        ExtendAllotmentRequest result = extendedAllotmentService.createExtensionRequest(tenant.getUserId(), requestDto);

        // Then
        assertEquals(currentEndDate, result.getExtensionStartDate());
        assertEquals(currentEndDate.plusWeeks(4), result.getExtensionEndDate());
    }

    @Test
    void testCreateExtensionRequest_DailyPlan() {
        // Given
        LocalDate currentEndDate = LocalDate.of(2024, 3, 15);
        currentAllotment.setEndDate(currentEndDate);
        
        ExtensionRequestDto requestDto = new ExtensionRequestDto();
        requestDto.setCurrentAgreementId("agreement-123");
        requestDto.setPlanId(plan.getId());

        // Setup plan with 30-day duration
        plan.setDuration(Duration.builder()
                .unit("DAY")
                .value(30)
                .durationType("FIXED")
                .build());

        // Mock dependencies
        when(agreementService.getAgreementById(anyString())).thenReturn(Optional.of(currentAgreement));
        when(roomAllotmentRepository.findByAgreementId(anyString())).thenReturn(Optional.of(currentAllotment));
        when(roomAgreementPlanService.getPlanById(anyString())).thenReturn(plan);
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(owner));
        when(extendRequestRepository.hasActiveTenantExtensionRequest(any(), anyString())).thenReturn(false);
        when(extendRequestRepository.save(any(ExtendAllotmentRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        ExtendAllotmentRequest result = extendedAllotmentService.createExtensionRequest(tenant.getUserId(), requestDto);

        // Then
        assertEquals(currentEndDate, result.getExtensionStartDate());
        assertEquals(currentEndDate.plusDays(30), result.getExtensionEndDate());
    }

    @Test
    void testCreateExtensionRequest_NullDuration_DefaultsTo12Months() {
        // Given
        LocalDate currentEndDate = LocalDate.of(2024, 3, 15);
        currentAllotment.setEndDate(currentEndDate);
        
        ExtensionRequestDto requestDto = new ExtensionRequestDto();
        requestDto.setCurrentAgreementId("agreement-123");
        requestDto.setPlanId(plan.getId());

        // Setup plan with null duration
        plan.setDuration(null);

        // Mock dependencies
        when(agreementService.getAgreementById(anyString())).thenReturn(Optional.of(currentAgreement));
        when(roomAllotmentRepository.findByAgreementId(anyString())).thenReturn(Optional.of(currentAllotment));
        when(roomAgreementPlanService.getPlanById(anyString())).thenReturn(plan);
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(owner));
        when(extendRequestRepository.hasActiveTenantExtensionRequest(any(), anyString())).thenReturn(false);
        when(extendRequestRepository.save(any(ExtendAllotmentRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        ExtendAllotmentRequest result = extendedAllotmentService.createExtensionRequest(tenant.getUserId(), requestDto);

        // Then - Should default to 12 months
        assertEquals(currentEndDate, result.getExtensionStartDate());
        assertEquals(currentEndDate.plusMonths(12), result.getExtensionEndDate());
    }
    @Test
    void testCreateExtensionRequest_InvalidDurationUnit_DefaultsToMonths() {
        // Given
        LocalDate currentEndDate = LocalDate.of(2024, 3, 15);
        currentAllotment.setEndDate(currentEndDate);
        
        ExtensionRequestDto requestDto = new ExtensionRequestDto();
        requestDto.setCurrentAgreementId("agreement-123");
        requestDto.setPlanId(plan.getId());

        // Setup plan with invalid duration unit
        plan.setDuration(Duration.builder()
                .unit("INVALID_UNIT")
                .value(3)
                .durationType("FIXED")
                .build());

        // Mock dependencies
        when(agreementService.getAgreementById(anyString())).thenReturn(Optional.of(currentAgreement));
        when(roomAllotmentRepository.findByAgreementId(anyString())).thenReturn(Optional.of(currentAllotment));
        when(roomAgreementPlanService.getPlanById(anyString())).thenReturn(plan);
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(owner));
        when(extendRequestRepository.hasActiveTenantExtensionRequest(any(), anyString())).thenReturn(false);
        when(extendRequestRepository.save(any(ExtendAllotmentRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        ExtendAllotmentRequest result = extendedAllotmentService.createExtensionRequest(tenant.getUserId(), requestDto);

        // Then - Should treat as months
        assertEquals(currentEndDate, result.getExtensionStartDate());
        assertEquals(currentEndDate.plusMonths(3), result.getExtensionEndDate());
    }

    @Test
    void testCreateExtensionRequest_PlanServiceFailure_UsesDefaultDuration() {
        // Given
        LocalDate currentEndDate = LocalDate.of(2024, 3, 15);
        currentAllotment.setEndDate(currentEndDate);
        
        ExtensionRequestDto requestDto = new ExtensionRequestDto();
        requestDto.setCurrentAgreementId("agreement-123");
        requestDto.setPlanId(plan.getId());

        // Mock dependencies
        when(agreementService.getAgreementById(anyString())).thenReturn(Optional.of(currentAgreement));
        when(roomAllotmentRepository.findByAgreementId(anyString())).thenReturn(Optional.of(currentAllotment));
        when(roomAgreementPlanService.getPlanById(anyString())).thenThrow(new RuntimeException("Plan service failure"));
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.of(owner));
        when(extendRequestRepository.hasActiveTenantExtensionRequest(any(), anyString())).thenReturn(false);
        when(extendRequestRepository.save(any(ExtendAllotmentRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        ExtendAllotmentRequest result = extendedAllotmentService.createExtensionRequest(tenant.getUserId(), requestDto);

        // Then - Should use fallback duration of 12 months
        assertEquals(currentEndDate, result.getExtensionStartDate());
        assertEquals(currentEndDate.plusMonths(12), result.getExtensionEndDate());
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // Original Owner Approval Process Tests  
    // ═══════════════════════════════════════════════════════════════════════════════
}
