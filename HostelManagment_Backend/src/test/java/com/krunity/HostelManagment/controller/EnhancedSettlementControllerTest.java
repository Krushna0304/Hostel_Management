package com.krunity.HostelManagment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krunity.HostelManagment.dto.SettlementTransactionRequest;
import com.krunity.HostelManagment.dto.EarlySettlementRequest;
import com.krunity.HostelManagment.dto.SettlementApprovalRequest;
import com.krunity.HostelManagment.enums.SettlementStatus;
import com.krunity.HostelManagment.model.SettlementRequest;
import com.krunity.HostelManagment.model.User;
import com.krunity.HostelManagment.service.EnhancedSettlementService;
import com.krunity.HostelManagment.service.SettlementTransactionCalculator;
import com.krunity.HostelManagment.Utils.ApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for EnhancedSettlementController
 * Tests the POST /api/v1/settlements/transactions endpoint
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(value = EnhancedSettlementController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
class EnhancedSettlementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EnhancedSettlementService enhancedSettlementService;

    @MockBean
    private SettlementTransactionCalculator settlementTransactionCalculator;

    private User mockOwner;
    private User mockTenant;
    private SettlementTransactionRequest validRequest;
    private EarlySettlementRequest validEarlySettlementRequest;
    private SettlementApprovalRequest validApprovalRequest;
    private SettlementRequest mockSettlementResponse;
    private SettlementRequest mockEarlySettlementResponse;
    private SettlementRequest mockApprovedSettlementResponse;

    @BeforeEach
    void setUp() {
        // Setup mock owner
        mockOwner = new User();
        mockOwner.setUserId(UUID.randomUUID());
        mockOwner.setDisplayName("Test Owner");
        mockOwner.setPhoneNumber("1234567890");

        // Setup mock tenant
        mockTenant = new User();
        mockTenant.setUserId(UUID.randomUUID());
        mockTenant.setDisplayName("Test Tenant");
        mockTenant.setPhoneNumber("9876543210");

        // Setup valid request
        validRequest = new SettlementTransactionRequest();
        validRequest.setAgreementId("test-agreement-123");
        validRequest.setCalculationDate(LocalDate.now());
        validRequest.setNotes("Test settlement transaction");

        // Setup valid early settlement request
        validEarlySettlementRequest = new EarlySettlementRequest();
        validEarlySettlementRequest.setAgreementId("test-agreement-123");
        validEarlySettlementRequest.setRequestedEndDate(LocalDate.now().plusDays(30));
        validEarlySettlementRequest.setReason("Job relocation");
        validEarlySettlementRequest.setTenantNotes("Need to vacate early due to job transfer");

        // Setup valid approval request
        validApprovalRequest = new SettlementApprovalRequest();
        validApprovalRequest.setOwnerNotes("Approved for early exit");
        validApprovalRequest.setUpdateRoomAvailability(true);

        // Setup mock settlement response
        mockSettlementResponse = SettlementRequest.builder()
                .settlementId(UUID.randomUUID())
                .agreementId("test-agreement-123")
                .status(SettlementStatus.SETTLEMENT_TRANSACTION_CREATED)
                .finalSettlementAmount(BigDecimal.valueOf(2500.00))
                .settlementType("OWNER_PAYABLE")
                .createdAt(LocalDateTime.now())
                .transactionCreatedAt(LocalDateTime.now())
                .build();

        // Setup mock early settlement response
        mockEarlySettlementResponse = SettlementRequest.builder()
                .settlementId(UUID.randomUUID())
                .agreementId("test-agreement-123")
                .status(SettlementStatus.PENDING_OWNER_REVIEW)
                .requestedEndDate(LocalDate.now().plusDays(30))
                .earlySettlementRequested(true)
                .tenant(mockTenant)
                .createdAt(LocalDateTime.now())
                .build();

        // Setup mock approved settlement response
        mockApprovedSettlementResponse = SettlementRequest.builder()
                .settlementId(UUID.randomUUID())
                .agreementId("test-agreement-123")
                .status(SettlementStatus.SETTLEMENT_APPROVED)
                .requestedEndDate(LocalDate.now().plusDays(30))
                .earlySettlementRequested(true)
                .tenant(mockTenant)
                .owner(mockOwner)
                .finalSettlementAmount(BigDecimal.valueOf(1500.00))
                .settlementType("TENANT_PAYABLE")
                .roomAvailabilityUpdated(true)
                .ownerNotes("Approved for early exit")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testCreateSettlementTransaction_Success() throws Exception {
        // Mock ApplicationContext to return the mock owner
        try (MockedStatic<ApplicationContext> mockedContext = Mockito.mockStatic(ApplicationContext.class)) {
            mockedContext.when(ApplicationContext::getUser).thenReturn(mockOwner);

            // Mock service method
            when(enhancedSettlementService.createSettlementTransaction(
                    eq("test-agreement-123"),
                    eq(mockOwner.getUserId()),
                    eq(LocalDate.now()),
                    eq("Test settlement transaction")
            )).thenReturn(mockSettlementResponse);

            // Mock getSettlementTransaction method
            when(enhancedSettlementService.getSettlementTransaction(any()))
                    .thenReturn(null); // Simulating no existing transaction

            // Perform the request
            mockMvc.perform(post("/api/v1/settlements/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.settlementId").exists())
                    .andExpect(jsonPath("$.status").value("SETTLEMENT_TRANSACTION_CREATED"))
                    .andExpect(jsonPath("$.agreementId").value("test-agreement-123"))
                    .andExpect(jsonPath("$.settlementAmount").value(2500.0))
                    .andExpect(jsonPath("$.settlementType").value("OWNER_PAYABLE"))
                    .andExpect(jsonPath("$.notificationSent").value(true))
                    .andExpect(jsonPath("$.message").value("Settlement transaction created successfully"));

            // Verify service method was called
            verify(enhancedSettlementService).createSettlementTransaction(
                    eq("test-agreement-123"),
                    eq(mockOwner.getUserId()),
                    eq(validRequest.getCalculationDate()),
                    eq("Test settlement transaction")
            );
        }
    }

    @Test
    void testCreateSettlementTransaction_ValidationError() throws Exception {
        // Create invalid request (missing required fields)
        SettlementTransactionRequest invalidRequest = new SettlementTransactionRequest();
        // Missing agreementId and calculationDate

        mockMvc.perform(post("/api/v1/settlements/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        // Verify service method was not called
        verifyNoInteractions(enhancedSettlementService);
    }

    @Test
    void testCreateSettlementTransaction_SecurityException() throws Exception {
        // Mock ApplicationContext to return the mock owner
        try (MockedStatic<ApplicationContext> mockedContext = Mockito.mockStatic(ApplicationContext.class)) {
            mockedContext.when(ApplicationContext::getUser).thenReturn(mockOwner);

            // Mock service method to throw SecurityException
            when(enhancedSettlementService.createSettlementTransaction(
                    any(), any(), any(), any()
            )).thenThrow(new SecurityException("Owner is not authorized for this agreement"));

            // Perform the request
            mockMvc.perform(post("/api/v1/settlements/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("ACCESS_DENIED"))
                    .andExpect(jsonPath("$.message").value("Owner is not authorized for this agreement"));
        }
    }

    @Test
    void testCreateSettlementTransaction_ServiceException() throws Exception {
        // Mock ApplicationContext to return the mock owner
        try (MockedStatic<ApplicationContext> mockedContext = Mockito.mockStatic(ApplicationContext.class)) {
            mockedContext.when(ApplicationContext::getUser).thenReturn(mockOwner);

            // Mock service method to throw general exception
            when(enhancedSettlementService.createSettlementTransaction(
                    any(), any(), any(), any()
            )).thenThrow(new RuntimeException("Settlement calculation failed"));

            // Perform the request
            mockMvc.perform(post("/api/v1/settlements/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("TRANSACTION_CREATION_FAILED"))
                    .andExpect(jsonPath("$.message").value("Settlement calculation failed"));
        }
    }

    @Test
    void testCreateSettlementTransaction_EmptyAgreementId() throws Exception {
        // Create request with empty agreement ID
        validRequest.setAgreementId("");

        mockMvc.perform(post("/api/v1/settlements/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());

        // Verify service method was not called
        verifyNoInteractions(enhancedSettlementService);
    }

    @Test
    void testCreateSettlementTransaction_NullCalculationDate() throws Exception {
        // Create request with null calculation date
        validRequest.setCalculationDate(null);

        mockMvc.perform(post("/api/v1/settlements/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());

        // Verify service method was not called
        verifyNoInteractions(enhancedSettlementService);
    }

    @Test
    void testCreateSettlementTransaction_WithOptionalNotes() throws Exception {
        // Test with notes = null (should be allowed)
        validRequest.setNotes(null);

        // Mock ApplicationContext to return the mock owner
        try (MockedStatic<ApplicationContext> mockedContext = Mockito.mockStatic(ApplicationContext.class)) {
            mockedContext.when(ApplicationContext::getUser).thenReturn(mockOwner);

            // Mock service method
            when(enhancedSettlementService.createSettlementTransaction(
                    eq("test-agreement-123"),
                    eq(mockOwner.getUserId()),
                    eq(validRequest.getCalculationDate()),
                    isNull()
            )).thenReturn(mockSettlementResponse);

            // Mock getSettlementTransaction method
            when(enhancedSettlementService.getSettlementTransaction(any()))
                    .thenReturn(null);

            // Perform the request
            mockMvc.perform(post("/api/v1/settlements/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("Settlement transaction created successfully"));
        }
    }

    // ─── Early Settlement Tests ──────────────────────────────────────────────

    @Test
    void testRequestEarlySettlement_Success() throws Exception {
        // Mock ApplicationContext to return the mock tenant
        try (MockedStatic<ApplicationContext> mockedContext = Mockito.mockStatic(ApplicationContext.class)) {
            mockedContext.when(ApplicationContext::getUser).thenReturn(mockTenant);

            // Mock service method
            when(enhancedSettlementService.processEarlySettlement(
                    eq("test-agreement-123"),
                    eq(mockTenant.getUserId()),
                    eq(validEarlySettlementRequest.getRequestedEndDate()),
                    eq("Need to vacate early due to job transfer")
            )).thenReturn(mockEarlySettlementResponse);

            // Perform the request
            mockMvc.perform(post("/api/v1/settlements/early-settlement")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validEarlySettlementRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.settlementId").exists())
                    .andExpect(jsonPath("$.status").value("PENDING_OWNER_REVIEW"))
                    .andExpect(jsonPath("$.agreementId").value("test-agreement-123"))
                    .andExpect(jsonPath("$.requestedEndDate").exists())
                    .andExpect(jsonPath("$.earlyExit").value(true))
                    .andExpect(jsonPath("$.message").value("Early settlement request submitted successfully"));

            // Verify service method was called
            verify(enhancedSettlementService).processEarlySettlement(
                    eq("test-agreement-123"),
                    eq(mockTenant.getUserId()),
                    eq(validEarlySettlementRequest.getRequestedEndDate()),
                    eq("Need to vacate early due to job transfer")
            );
        }
    }

    @Test
    void testRequestEarlySettlement_ValidationError() throws Exception {
        // Create invalid request (missing required fields)
        EarlySettlementRequest invalidRequest = new EarlySettlementRequest();
        // Missing agreementId and requestedEndDate

        mockMvc.perform(post("/api/v1/settlements/early-settlement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        // Verify service method was not called
        verifyNoInteractions(enhancedSettlementService);
    }

    @Test
    void testRequestEarlySettlement_SecurityException() throws Exception {
        // Mock ApplicationContext to return the mock tenant
        try (MockedStatic<ApplicationContext> mockedContext = Mockito.mockStatic(ApplicationContext.class)) {
            mockedContext.when(ApplicationContext::getUser).thenReturn(mockTenant);

            // Mock service method to throw SecurityException
            when(enhancedSettlementService.processEarlySettlement(
                    any(), any(), any(), any()
            )).thenThrow(new SecurityException("Tenant is not authorized for this agreement"));

            // Perform the request
            mockMvc.perform(post("/api/v1/settlements/early-settlement")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validEarlySettlementRequest)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("ACCESS_DENIED"))
                    .andExpect(jsonPath("$.message").value("Tenant is not authorized for this agreement"));
        }
    }

    @Test
    void testRequestEarlySettlement_IllegalArgumentException() throws Exception {
        // Mock ApplicationContext to return the mock tenant
        try (MockedStatic<ApplicationContext> mockedContext = Mockito.mockStatic(ApplicationContext.class)) {
            mockedContext.when(ApplicationContext::getUser).thenReturn(mockTenant);

            // Mock service method to throw IllegalArgumentException
            when(enhancedSettlementService.processEarlySettlement(
                    any(), any(), any(), any()
            )).thenThrow(new IllegalArgumentException("Requested end date cannot be in the past"));

            // Perform the request
            mockMvc.perform(post("/api/v1/settlements/early-settlement")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validEarlySettlementRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.message").value("Requested end date cannot be in the past"));
        }
    }

    @Test
    void testRequestEarlySettlement_ServiceException() throws Exception {
        // Mock ApplicationContext to return the mock tenant
        try (MockedStatic<ApplicationContext> mockedContext = Mockito.mockStatic(ApplicationContext.class)) {
            mockedContext.when(ApplicationContext::getUser).thenReturn(mockTenant);

            // Mock service method to throw general exception
            when(enhancedSettlementService.processEarlySettlement(
                    any(), any(), any(), any()
            )).thenThrow(new RuntimeException("Settlement request processing failed"));

            // Perform the request
            mockMvc.perform(post("/api/v1/settlements/early-settlement")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validEarlySettlementRequest)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").value("EARLY_SETTLEMENT_FAILED"))
                    .andExpect(jsonPath("$.message").value("Settlement request processing failed"));
        }
    }

    // ─── Settlement Approval Tests ───────────────────────────────────────────

    @Test
    void testApproveSettlementWithRoomUpdate_Success() throws Exception {
        UUID settlementId = UUID.randomUUID();
        
        // Mock ApplicationContext to return the mock owner
        try (MockedStatic<ApplicationContext> mockedContext = Mockito.mockStatic(ApplicationContext.class)) {
            mockedContext.when(ApplicationContext::getUser).thenReturn(mockOwner);

            // Mock service method
            when(enhancedSettlementService.approveSettlementWithRoomUpdate(
                    eq(settlementId),
                    eq(mockOwner.getUserId()),
                    eq("Approved for early exit"),
                    eq(true)
            )).thenReturn(mockApprovedSettlementResponse);

            // Perform the request
            mockMvc.perform(put("/api/v1/settlements/{settlementId}/approve-with-room-update", settlementId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validApprovalRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.settlementId").exists())
                    .andExpect(jsonPath("$.status").value("SETTLEMENT_APPROVED"))
                    .andExpect(jsonPath("$.agreementId").value("test-agreement-123"))
                    .andExpect(jsonPath("$.finalAmount").value(1500.0))
                    .andExpect(jsonPath("$.settlementType").value("TENANT_PAYABLE"))
                    .andExpect(jsonPath("$.roomAvailabilityUpdated").value(true))
                    .andExpect(jsonPath("$.message").value("Settlement approved successfully"));

            // Verify service method was called
            verify(enhancedSettlementService).approveSettlementWithRoomUpdate(
                    eq(settlementId),
                    eq(mockOwner.getUserId()),
                    eq("Approved for early exit"),
                    eq(true)
            );
        }
    }

    @Test
    void testApproveSettlementWithRoomUpdate_SecurityException() throws Exception {
        UUID settlementId = UUID.randomUUID();
        
        // Mock ApplicationContext to return the mock owner
        try (MockedStatic<ApplicationContext> mockedContext = Mockito.mockStatic(ApplicationContext.class)) {
            mockedContext.when(ApplicationContext::getUser).thenReturn(mockOwner);

            // Mock service method to throw SecurityException
            when(enhancedSettlementService.approveSettlementWithRoomUpdate(
                    any(), any(), any(), anyBoolean()
            )).thenThrow(new SecurityException("Owner is not authorized for this settlement"));

            // Perform the request
            mockMvc.perform(put("/api/v1/settlements/{settlementId}/approve-with-room-update", settlementId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validApprovalRequest)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("ACCESS_DENIED"))
                    .andExpect(jsonPath("$.message").value("Owner is not authorized for this settlement"));
        }
    }

    @Test
    void testApproveSettlementWithRoomUpdate_ServiceException() throws Exception {
        UUID settlementId = UUID.randomUUID();
        
        // Mock ApplicationContext to return the mock owner
        try (MockedStatic<ApplicationContext> mockedContext = Mockito.mockStatic(ApplicationContext.class)) {
            mockedContext.when(ApplicationContext::getUser).thenReturn(mockOwner);

            // Mock service method to throw general exception
            when(enhancedSettlementService.approveSettlementWithRoomUpdate(
                    any(), any(), any(), anyBoolean()
            )).thenThrow(new RuntimeException("Settlement approval failed"));

            // Perform the request
            mockMvc.perform(put("/api/v1/settlements/{settlementId}/approve-with-room-update", settlementId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validApprovalRequest)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").value("APPROVAL_FAILED"))
                    .andExpect(jsonPath("$.message").value("Settlement approval failed"));
        }
    }

    @Test
    void testApproveSettlementWithRoomUpdate_WithoutRoomUpdate() throws Exception {
        UUID settlementId = UUID.randomUUID();
        
        // Create approval request without room update
        SettlementApprovalRequest noRoomUpdateRequest = new SettlementApprovalRequest();
        noRoomUpdateRequest.setOwnerNotes("Approved without room update");
        noRoomUpdateRequest.setUpdateRoomAvailability(false);

        // Create mock response without room update
        SettlementRequest noRoomUpdateResponse = SettlementRequest.builder()
                .settlementId(settlementId)
                .agreementId("test-agreement-123")
                .status(SettlementStatus.SETTLEMENT_APPROVED)
                .roomAvailabilityUpdated(false)
                .ownerNotes("Approved without room update")
                .build();
        
        // Mock ApplicationContext to return the mock owner
        try (MockedStatic<ApplicationContext> mockedContext = Mockito.mockStatic(ApplicationContext.class)) {
            mockedContext.when(ApplicationContext::getUser).thenReturn(mockOwner);

            // Mock service method
            when(enhancedSettlementService.approveSettlementWithRoomUpdate(
                    eq(settlementId),
                    eq(mockOwner.getUserId()),
                    eq("Approved without room update"),
                    eq(false)
            )).thenReturn(noRoomUpdateResponse);

            // Perform the request
            mockMvc.perform(put("/api/v1/settlements/{settlementId}/approve-with-room-update", settlementId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(noRoomUpdateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.settlementId").exists())
                    .andExpect(jsonPath("$.roomAvailabilityUpdated").value(false))
                    .andExpect(jsonPath("$.message").value("Settlement approved successfully"));
        }
    }
}