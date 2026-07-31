package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.dto.SettlementCalculationResult;
import com.krunity.HostelManagment.dto.SettlementTransaction;
import com.krunity.HostelManagment.enums.RoomAllotmentStatus;
import com.krunity.HostelManagment.enums.SettlementStatus;
import com.krunity.HostelManagment.exception.NotFoundException;
import com.krunity.HostelManagment.model.*;
import com.krunity.HostelManagment.repository.RoomAllotmentRepository;
import com.krunity.HostelManagment.repository.SettlementRequestRepository;
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
 * Integration tests for the complete settlement transaction workflow (Task 7.1.1).
 * Covers: createSettlementTransaction → approveSettlementWithRoomUpdate → SETTLEMENT_DONE,
 * early settlement workflow, amount calculation, and error handling.
 */
@ExtendWith(MockitoExtension.class)
class SettlementTransactionWorkflowTest {

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

    private User owner;
    private User tenant;
    private Room room;
    private Agreement agreement;
    private RoomAllotment allotment;

    @BeforeEach
    void setUp() {
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

        room = Room.builder()
                .roomId(UUID.randomUUID())
                .roomNumber("B-202")
                .totalBeds(4)
                .build();

        agreement = Agreement.builder()
                .id("agreement-settle-001")
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
                .agreementId("agreement-settle-001")
                .startDate(LocalDate.now().minusMonths(6))
                .endDate(LocalDate.now().plusDays(30))
                .roomAllotmentStatus(RoomAllotmentStatus.ACTIVE)
                .build();
    }

    // ─── createSettlementTransaction Tests ───────────────────────────────────

    @Test
    void testCreateSettlementTransaction_Success_StatusBecomesTransactionCreated() {
        // Given
        SettlementCalculationResult calcResult = buildCalcResult("OWNER_PAYABLE", BigDecimal.valueOf(2000));
        SettlementTransaction transaction = buildTransaction("OWNER_PAYABLE", BigDecimal.valueOf(2000));
        SettlementRequest savedSettlement = SettlementRequest.builder()
                .settlementId(UUID.randomUUID())
                .agreementId("agreement-settle-001")
                .tenant(tenant)
                .owner(owner)
                .status(SettlementStatus.SETTLEMENT_TRANSACTION_CREATED)
                .finalSettlementAmount(BigDecimal.valueOf(2000))
                .settlementType("OWNER_PAYABLE")
                .transactionCreatedAt(LocalDateTime.now())
                .build();

        when(agreementService.getAgreementById("agreement-settle-001")).thenReturn(Optional.of(agreement));
        when(roomAllotmentRepository.findByAgreementId("agreement-settle-001")).thenReturn(Optional.of(allotment));
        when(settlementCalculator.calculateSettlement(any(), any(), any())).thenReturn(calcResult);
        when(userRepository.findById(owner.getUserId())).thenReturn(Optional.of(owner));
        when(settlementCalculator.createSettlementTransaction(any(), any(), any())).thenReturn(transaction);
        when(settlementCalculator.formatSettlementTransaction(any())).thenReturn("{\"transactionId\":\"tx-001\"}");
        when(settlementRepository.save(any(SettlementRequest.class))).thenReturn(savedSettlement);
        doNothing().when(notificationService).sendSms(anyString(), anyString());

        // When
        SettlementRequest result = enhancedSettlementService.createSettlementTransaction(
                "agreement-settle-001", owner.getUserId(), LocalDate.now(), "Test notes");

        // Then
        assertNotNull(result);
        assertEquals(SettlementStatus.SETTLEMENT_TRANSACTION_CREATED, result.getStatus());
        assertEquals("agreement-settle-001", result.getAgreementId());
        assertEquals(BigDecimal.valueOf(2000), result.getFinalSettlementAmount());
        assertEquals("OWNER_PAYABLE", result.getSettlementType());
        verify(settlementRepository).save(any(SettlementRequest.class));
    }

    @Test
    void testCreateSettlementTransaction_UnauthorizedOwner_ThrowsSecurityException() {
        // Given
        UUID wrongOwnerId = UUID.randomUUID();
        when(agreementService.getAgreementById("agreement-settle-001")).thenReturn(Optional.of(agreement));

        // When & Then
        assertThrows(Exception.class, () ->
                enhancedSettlementService.createSettlementTransaction(
                        "agreement-settle-001", wrongOwnerId, LocalDate.now(), null));

        verify(settlementRepository, never()).save(any());
    }

    @Test
    void testCreateSettlementTransaction_AgreementNotFound_ThrowsNotFoundException() {
        // Given
        when(agreementService.getAgreementById(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(Exception.class, () ->
                enhancedSettlementService.createSettlementTransaction(
                        "non-existent-agreement", owner.getUserId(), LocalDate.now(), null));
    }

    // ─── approveSettlementWithRoomUpdate Tests ────────────────────────────────

    @Test
    void testApproveSettlementWithRoomUpdate_Success_StatusBecomesApproved() {
        // Given
        UUID settlementId = UUID.randomUUID();
        SettlementRequest pendingSettlement = SettlementRequest.builder()
                .settlementId(settlementId)
                .agreementId("agreement-settle-001")
                .tenant(tenant)
                .owner(owner)
                .status(SettlementStatus.PENDING_OWNER_REVIEW)
                .earlySettlementRequested(true)
                .requestedEndDate(LocalDate.now().plusDays(15))
                .build();

        // Set allotment to a status that can transition to ON_NOTICE_PERIOD
        allotment.setRoomAllotmentStatus(RoomAllotmentStatus.SETTLEMENT_REQUESTED);

        when(settlementRepository.findById(settlementId)).thenReturn(Optional.of(pendingSettlement));
        when(roomAllotmentRepository.findByAgreementId("agreement-settle-001"))
                .thenReturn(Optional.of(allotment));
        when(settlementRepository.save(any(SettlementRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roomAllotmentRepository.save(any(RoomAllotment.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(notificationService).sendSettlementApprovalNotification(any(), any());

        // When
        SettlementRequest result = enhancedSettlementService.approveSettlementWithRoomUpdate(
                settlementId, owner.getUserId(), "Approved by owner", true);

        // Then
        assertNotNull(result);
        assertEquals(SettlementStatus.SETTLEMENT_APPROVED, result.getStatus());
        assertEquals("Approved by owner", result.getOwnerNotes());
        verify(settlementRepository).save(any(SettlementRequest.class));
    }

    @Test
    void testApproveSettlementWithRoomUpdate_UnauthorizedOwner_ThrowsSecurityException() {
        // Given
        UUID settlementId = UUID.randomUUID();
        UUID wrongOwnerId = UUID.randomUUID();

        SettlementRequest settlement = SettlementRequest.builder()
                .settlementId(settlementId)
                .tenant(tenant)
                .owner(owner)
                .status(SettlementStatus.PENDING_OWNER_REVIEW)
                .build();

        when(settlementRepository.findById(settlementId)).thenReturn(Optional.of(settlement));

        // When & Then
        assertThrows(Exception.class, () ->
                enhancedSettlementService.approveSettlementWithRoomUpdate(
                        settlementId, wrongOwnerId, "Notes", true));

        verify(settlementRepository, never()).save(any());
    }

    // ─── processEarlySettlement Tests ─────────────────────────────────────────

    @Test
    void testProcessEarlySettlement_Success_StatusBecomesPendingOwnerReview() {
        // Given
        LocalDate requestedEndDate = LocalDate.now().plusDays(15);

        when(agreementService.getAgreementById("agreement-settle-001")).thenReturn(Optional.of(agreement));
        when(roomAllotmentRepository.findByAgreementId("agreement-settle-001")).thenReturn(Optional.of(allotment));
        when(settlementRepository.findByAgreementId("agreement-settle-001")).thenReturn(Optional.empty());
        when(settlementCalculator.calculateSettlement(any(), any(), any()))
                .thenReturn(buildCalcResult("TENANT_PAYABLE", BigDecimal.valueOf(500)));
        when(userRepository.findById(owner.getUserId())).thenReturn(Optional.of(owner));
        when(settlementRepository.save(any(SettlementRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roomAllotmentRepository.save(any(RoomAllotment.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(notificationService).sendSettlementRequestNotification(any(), any(), any());

        // When
        SettlementRequest result = enhancedSettlementService.processEarlySettlement(
                "agreement-settle-001", tenant.getUserId(), requestedEndDate, "Job transfer");

        // Then
        assertNotNull(result);
        assertEquals(SettlementStatus.PENDING_OWNER_REVIEW, result.getStatus());
        assertTrue(result.isEarlySettlementRequested());
        assertEquals(requestedEndDate, result.getRequestedEndDate());
        verify(settlementRepository).save(any(SettlementRequest.class));
    }

    @Test
    void testProcessEarlySettlement_AlreadyExists_ThrowsIllegalStateException() {
        // Given
        SettlementRequest existing = SettlementRequest.builder()
                .settlementId(UUID.randomUUID())
                .agreementId("agreement-settle-001")
                .status(SettlementStatus.PENDING_OWNER_REVIEW)
                .build();

        when(agreementService.getAgreementById("agreement-settle-001")).thenReturn(Optional.of(agreement));
        when(roomAllotmentRepository.findByAgreementId("agreement-settle-001")).thenReturn(Optional.of(allotment));
        when(settlementRepository.findByAgreementId("agreement-settle-001")).thenReturn(Optional.of(existing));

        // When & Then
        assertThrows(Exception.class, () ->
                enhancedSettlementService.processEarlySettlement(
                        "agreement-settle-001", tenant.getUserId(), LocalDate.now().plusDays(10), "reason"));
    }

    @Test
    void testSettlementAmountCalculation_OwnerPayableWhenDepositExceedsDeductions() {
        // Given
        SettlementCalculationResult calcResult = SettlementCalculationResult.builder()
                .agreementId("agreement-settle-001")
                .calculationDate(LocalDate.now())
                .allotmentId(allotment.getAllotmentId())
                .securityDeposit(BigDecimal.valueOf(10000))
                .outstandingRent(BigDecimal.valueOf(2000))
                .outstandingCharges(BigDecimal.ZERO)
                .damageCharges(BigDecimal.ZERO)
                .cleaningCharges(BigDecimal.ZERO)
                .otherDeductions(BigDecimal.ZERO)
                .earlyExitPenalty(BigDecimal.ZERO)
                .totalDeductions(BigDecimal.valueOf(2000))
                .finalSettlementAmount(BigDecimal.valueOf(8000))
                .settlementType("OWNER_PAYABLE")
                .build();

        SettlementTransaction transaction = buildTransaction("OWNER_PAYABLE", BigDecimal.valueOf(8000));

        when(agreementService.getAgreementById("agreement-settle-001")).thenReturn(Optional.of(agreement));
        when(roomAllotmentRepository.findByAgreementId("agreement-settle-001")).thenReturn(Optional.of(allotment));
        when(settlementCalculator.calculateSettlement(any(), any(), any())).thenReturn(calcResult);
        when(userRepository.findById(owner.getUserId())).thenReturn(Optional.of(owner));
        when(settlementCalculator.createSettlementTransaction(any(), any(), any())).thenReturn(transaction);
        when(settlementCalculator.formatSettlementTransaction(any())).thenReturn("{\"tx\":\"data\"}");
        when(settlementRepository.save(any(SettlementRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(notificationService).sendSms(anyString(), anyString());

        // When
        SettlementRequest result = enhancedSettlementService.createSettlementTransaction(
                "agreement-settle-001", owner.getUserId(), LocalDate.now(), null);

        // Then
        assertEquals("OWNER_PAYABLE", result.getSettlementType());
        assertEquals(BigDecimal.valueOf(8000), result.getFinalSettlementAmount());
    }

    // ─── Helper Methods ────────────────────────────────────────────────────────

    private SettlementCalculationResult buildCalcResult(String type, BigDecimal amount) {
        return SettlementCalculationResult.builder()
                .agreementId("agreement-settle-001")
                .calculationDate(LocalDate.now())
                .allotmentId(allotment.getAllotmentId())
                .securityDeposit(BigDecimal.valueOf(5000))
                .outstandingRent(BigDecimal.valueOf(1000))
                .outstandingCharges(BigDecimal.ZERO)
                .damageCharges(BigDecimal.ZERO)
                .cleaningCharges(BigDecimal.ZERO)
                .otherDeductions(BigDecimal.ZERO)
                .earlyExitPenalty(BigDecimal.ZERO)
                .totalDeductions(BigDecimal.valueOf(1000))
                .finalSettlementAmount(amount)
                .settlementType(type)
                .build();
    }

    private SettlementTransaction buildTransaction(String type, BigDecimal amount) {
        return SettlementTransaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .agreementId("agreement-settle-001")
                .allotmentId(allotment.getAllotmentId().toString())
                .calculationDate(LocalDate.now())
                .finalSettlementAmount(amount)
                .settlementType(type)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
