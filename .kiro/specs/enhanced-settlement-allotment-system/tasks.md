# Implementation Plan: Enhanced Settlement and Allotment System

## Overview

This document outlines the implementation tasks for the Enhanced Settlement and Allotment System, organized by priority and dependencies. The system enhances the existing hostel management platform with advanced settlement processing, allotment extensions, plan expiry notifications, and room overbooking management capabilities.

## Tasks

- [x] 1.1 Database Schema Migrations - Create database migration scripts for enhanced settlement system (Backend Developer, 4 hours, Dependencies: None)
  - **Status**: ✅ Completed
  - **Description**: Create database migration scripts for enhanced settlement system
  - **Acceptance Criteria**:
    - [x] 1.1.1 Enhanced settlement_requests table with new columns
    - [x] 1.1.2 New extend_allotment_requests table
    - [x] 1.1.3 Enhanced room_allotments with new statuses
    - [x] 1.1.4 Plan expiry notifications table
    - [x] 1.1.5 Room overbooking log table
    - [x] 1.1.6 All indexes and constraints created

- [x] 1.2 Enhanced Settlement Status Enums - Extend existing SettlementStatus enum with new states (Backend Developer, 1 hour, Dependencies: 1.1)
  - **Status**: ✅ Completed
  - **Description**: Extend existing SettlementStatus enum with new states
  - **Acceptance Criteria**:
    - [x] 1.2.1 New settlement statuses added (SETTLEMENT_TRANSACTION_CREATED, SETTLEMENT_APPROVED, SETTLEMENT_DONE)
    - [x] 1.2.2 Enhanced RoomAllotmentStatus with new values
    - [x] 1.2.3 Migration compatibility maintained

- [x] 1.3 Enhanced Room Allotment Entity - Extend RoomAllotment entity with new fields and relationships (Backend Developer, 2 hours, Dependencies: 1.1, 1.2)
  - **Status**: ✅ Completed
  - **Description**: Extend RoomAllotment entity with new fields and relationships
  - **Acceptance Criteria**:
    - [x] 1.3.1 Extension request tracking fields added
    - [x] 1.3.2 Parent allotment relationship for extensions
    - [x] 1.3.3 Settlement task flags
    - [x] 1.3.4 JPA relationships properly configured

- [x] 2.1 Settlement Transaction Calculator - Create settlement transaction calculation engine (Backend Developer, 6 hours, Dependencies: 1.3)
  - **Status**: ✅ Completed
  - **Description**: Create settlement transaction calculation engine
  - **Acceptance Criteria**:
    - [x] 2.1.1 Calculate settlement based on current plan snapshot
    - [x] 2.1.2 Support early exit penalties
    - [x] 2.1.3 Handle positive/negative settlement amounts
    - [x] 2.1.4 Parse and format settlement transaction data
    - [x] 2.1.5 Round-trip parsing validation

- [x] 2.2 Enhanced Settlement Service - Extend existing SettlementService with new workflows (Backend Developer, 8 hours, Dependencies: 2.1)
  - **Status**: ✅ Completed
  - **Description**: Extend existing SettlementService with new workflows
  - **Acceptance Criteria**:
    - [x] 2.2.1 Create settlement transaction at any time
    - [x] 2.2.2 Process early settlement requests
    - [x] 2.2.3 Handle settlement approval with room updates
    - [x] 2.2.4 Maintain audit trail
    - [x] 2.2.5 Integration with notification service

- [x] 2.3 Settlement Transaction API Endpoints - Create REST endpoints for settlement transaction management (Backend Developer, 4 hours, Dependencies: 2.2)
  - **Status**: ✅ Completed
  - **Description**: Create REST endpoints for settlement transaction management
  - **Acceptance Criteria**:
    - [x] 2.3.1 POST /api/v1/settlements/transactions
    - [x] 2.3.1 POST /api/v1/settlements/early-settlement
    - [x] 2.3.1 PUT /api/v1/settlements/{id}/approve-with-room-update
    - [x] 2.3.1 Proper authorization and validation
    - [x] 2.3.1 Error handling with detailed messages

- [x] 3.1 Extend Allotment Request Entity - Create ExtendAllotmentRequest entity and repository (Backend Developer, 3 hours, Dependencies: 1.1)
  - **Status**: ✅ Completed
  - **Description**: Create ExtendAllotmentRequest entity and repository
  - **Acceptance Criteria**:
    - [x] 3.1.1 Complete entity with all fields and relationships
    - [x] 3.1.2 JPA repository with custom query methods
    - [x] 3.1.3 Status transition validation
    - [x] 3.1.4 Expiration handling

- [x] 3.2 Extended Allotment Service - Create service for handling allotment extension workflow (Backend Developer, 10 hours, Dependencies: 3.1, 2.2)
  - **Status**: ✅ Completed
  - **Description**: Create service for handling allotment extension workflow
  - **Acceptance Criteria**:
    - [x] 3.2.1 Create extension requests with validation
    - [x] 3.2.2 Owner approval process with agreement creation
    - [ ] 3.2.3 Payment processing integration
    - [x] 3.2.4 Seamless transition date calculation
    - [x] 3.2.5 Notification integration

- [x] 3.3 Extension Request API Endpoints - Create REST endpoints for extension request management (Backend Developer, 5 hours, Dependencies: 3.2)
  - **Status**: ✅ Completed
  - **Description**: Create REST endpoints for extension request management
  - **Acceptance Criteria**:
    - [x] 3.3.1 POST /api/v1/allotments/extend
    - [x] 3.3.2 PUT /api/v1/allotments/extend/{id}/approve
    - [x] 3.3.3 POST /api/v1/allotments/extend/{id}/payment
    - [x] 3.3.4 GET /api/v1/allotments/extend/tenant/{tenantId}
    - [x] 3.3.5 Authorization and validation

- [x] 4.1 Plan Expiry Notification Entity - Create PlanExpiryNotification entity and repository (Backend Developer, 2 hours, Dependencies: 1.1)
  - **Status**: ✅ Completed
  - **Description**: Create PlanExpiryNotification entity and repository
  - **Acceptance Criteria**:
    - [x] 4.1.1 Complete entity with notification types
    - [x] 4.1.2 Repository with scheduling queries
    - [x] 4.1.3 Delivery status tracking
    - [x] 4.1.4 Retry mechanism support

- [x] 4.2 Plan Expiry Notification Service - Create automated plan expiry notification system (Backend Developer, 6 hours, Dependencies: 4.1)
  - **Status**: ✅ Completed
  - **Description**: Create automated plan expiry notification system
  - **Acceptance Criteria**:
    - [x] 4.2.1 Scheduled job for daily processing
    - [x] 4.2.2 Configurable lead times
    - [x] 4.2.3 Integration with existing NotificationService
    - [x] 4.2.4 Template-based messaging
    - [x] 4.2.5 Retry logic for failed notifications

- [x] 4.3 Notification Management API - Create endpoints for notification management (Backend Developer, 3 hours, Dependencies: 4.2)
  - **Status**: ✅ Completed
  - **Description**: Create endpoints for notification management
  - **Acceptance Criteria**:
    - [x] 4.3.1 GET /api/v1/notifications/plan-expiry/{tenantId}
    - [x] 4.3.2 POST /api/v1/notifications/plan-expiry/schedule
    - [x] 4.3.3 PUT /api/v1/notifications/plan-expiry/{id}/resend
    - [x] 4.3.4 Admin endpoints for notification configuration

- [x] 5.1 Room Overbooking Manager - Create room allocation system with overbooking support (Backend Developer, 8 hours, Dependencies: 1.1)
  - **Status**: Not Started
  - **Description**: Create room allocation system with overbooking support
  - **Acceptance Criteria**:
    - [x] 5.1.1 First-come-first-served allocation logic
    - [x] 5.1.2 Overbooking detection (20 agreements for 10 bed room)
    - [x] 5.1.3 Room availability calculation with pending actions
    - [x] 5.1.4 Allocation failure handling
    - [x] 5.1.5 Concurrent allocation safety

- [x] 5.2 Room Allocation API Endpoints - Create endpoints for room allocation management (Backend Developer, 4 hours, Dependencies: 5.1)
  - **Status**: Completed
  - **Description**: Create endpoints for room allocation management
  - **Acceptance Criteria**:
    - [x] 5.2.1 GET /api/v1/rooms/{id}/availability
    - [x] 5.2.2 POST /api/v1/rooms/{id}/allocate
    - [x] 5.2.3 GET /api/v1/rooms/search/available
    - [x] 5.2.4 Room dropdown with pending action counts
    - [x] 5.2.5 Sorting by TenantActionPending count

- [ x] 6.1 Settlement Transaction Components - Create frontend components for settlement transactions (Frontend Developer, 8 hours, Dependencies: 2.3)
  - **Status**: Not Started
  - **Description**: Create frontend components for settlement transactions
  - **Acceptance Criteria**:
    - [x] 6.1.1 Settlement transaction creation modal
    - [x] 6.1.2 Early settlement request form
    - [x] 6.1.3 Enhanced settlement summary display
    - [x] 6.1.4 Real-time status updates
    - [x] 6.1.5 Integration with existing settlement components

- [x] 6.2 Extension Request Components - Create frontend components for allotment extensions (Frontend Developer, 12 hours, Dependencies: 3.3)
  - **Status**: ✅ Completed
  - **Description**: Create frontend components for allotment extensions
  - **Acceptance Criteria**:
    - [x] 6.2.1 Extension request form for tenants
    - [x] 6.2.2 Owner approval interface
    - [x] 6.2.3 Payment processing integration
    - [x] 6.2.4 Extension status tracking
    - [x] 6.2.5 Accept button for approved extensions

- [x] 6.3 Enhanced Room Management UI - Enhance room management interface with overbooking support (Frontend Developer, 6 hours, Dependencies: 5.2)
  - **Status**: Completed
  - **Description**: Enhance room management interface with overbooking support
  - **Acceptance Criteria**:
    - [x] 6.3.1 Room availability display with bed counts
    - [x] 6.3.2 Pending action indicators
    - [x] 6.3.3 Overbooking warnings and messages
    - [x] 6.3.4 Room dropdown with sorting
    - [x] 6.3.5 Real-time availability updates

- [x] 6.4 Plan Expiry Notification UI - Create UI for plan expiry notifications (Frontend Developer, 4 hours, Dependencies: 4.3)
  - **Status**: Completed
  - **Description**: Create UI for plan expiry notifications
  - **Acceptance Criteria**:
    - [x] 6.4.1 Notification center for tenants
    - [x] 6.4.2 Plan expiry dashboard
    - [x] 6.4.3 Action buttons for settlement/extension
    - [x] 6.4.4 Notification preferences
    - [x] 6.4.5 Admin notification management

- [x] 7.1 Service Integration Testing - Integration testing for all new services (Full Stack Developer, 6 hours, Dependencies: All backend tasks)
  - **Status**: ✅ Completed
  - **Description**: Integration testing for all new services
  - **Acceptance Criteria**:
    - [x] 7.1.1 End-to-end settlement transaction workflow
    - [x] 7.1.2 Extension request complete workflow
    - [x] 7.1.3 Plan expiry notification system
    - [x] 7.1.4 Room overbooking scenarios
    - [x] 7.1.5 Error handling validation

- [ ] 7.2 Frontend-Backend Integration - Complete frontend-backend integration (Full Stack Developer, 4 hours, Dependencies: All frontend and backend tasks)
  - **Status**: Not Started
  - **Description**: Complete frontend-backend integration
  - **Acceptance Criteria**:
    - [ ] 7.2.1 All API endpoints connected
    - [ ] 7.2.2 Real-time updates working
    - [ ] 7.2.3 Error handling on frontend
    - [ ] 7.2.4 Loading states and user feedback
    - [ ] 7.2.5 Cross-browser compatibility

- [ ] 7.3 Performance Testing & Optimization - Performance testing and optimization (Backend Developer, 4 hours, Dependencies: 7.2)
  - **Status**: Not Started
  - **Description**: Performance testing and optimization
  - **Acceptance Criteria**:
    - [ ] 7.3.1 Database query optimization
    - [ ] 7.3.2 Caching implementation
    - [ ] 7.3.3 Concurrent access testing
    - [ ] 7.3.4 Load testing for overbooking scenarios
    - [ ] 7.3.5 Memory usage optimization

- [ ] 8.1 API Documentation - Complete API documentation (Backend Developer, 3 hours, Dependencies: All API tasks)
  - **Status**: Not Started
  - **Description**: Complete API documentation
  - **Acceptance Criteria**:
    - [ ] 8.1.1 OpenAPI/Swagger documentation
    - [ ] 8.1.2 Request/response examples
    - [ ] 8.1.3 Error code documentation
    - [ ] 8.1.4 Authentication requirements
    - [ ] 8.1.5 Rate limiting documentation

- [ ] 8.2 User Documentation - Create user documentation and guides (Product Owner, 4 hours, Dependencies: 7.2)
  - **Status**: Not Started
  - **Description**: Create user documentation and guides
  - **Acceptance Criteria**:
    - [ ] 8.2.1 Settlement process guide
    - [ ] 8.2.2 Extension request guide
    - [ ] 8.2.3 Room allocation guide
    - [ ] 8.2.4 Notification setup guide
    - [ ] 8.2.5 Troubleshooting documentation

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["4.1.1", "4.1.2", "4.1.3", "4.1.4"] },
    { "id": 1, "tasks": ["4.2.1", "4.2.2", "4.2.3", "4.2.4", "4.2.5", "5.1.1", "5.1.2", "5.1.3", "5.1.4", "5.1.5"] },
    { "id": 2, "tasks": ["4.3.1", "4.3.2", "4.3.3", "4.3.4", "5.2.1", "5.2.2", "5.2.3", "5.2.4", "5.2.5"] },
    { "id": 3, "tasks": ["6.1.1", "6.1.2", "6.1.3", "6.1.4", "6.1.5", "6.2.1", "6.2.2", "6.2.3", "6.2.4", "6.2.5"] },
    { "id": 4, "tasks": ["6.3.1", "6.3.2", "6.3.3", "6.3.4", "6.3.5", "6.4.1", "6.4.2", "6.4.3", "6.4.4", "6.4.5"] },
    { "id": 5, "tasks": ["7.1.1", "7.1.2", "7.1.3", "7.1.4", "7.1.5"] },
    { "id": 6, "tasks": ["7.2.1", "7.2.2", "7.2.3", "7.2.4", "7.2.5"] },
    { "id": 7, "tasks": ["7.3.1", "7.3.2", "7.3.3", "7.3.4", "7.3.5", "8.1.1", "8.1.2", "8.1.3", "8.1.4", "8.1.5"] },
    { "id": 8, "tasks": ["8.2.1", "8.2.2", "8.2.3", "8.2.4", "8.2.5"] }
  ]
}
```

## Notes

**Total Estimated Effort**: 103 hours  
**Critical Path**: Database → Settlement System → Extension System → Frontend → Integration  
**Recommended Team Size**: 2-3 developers (1 Backend, 1 Frontend, 1 Full Stack for integration)  
**Estimated Timeline**: 6-8 weeks (assuming 15-20 hours per week per developer)

**Risk Mitigation**:
1. **Database Migration Risks**: Test all migrations on staging environment first
2. **Integration Complexity**: Implement comprehensive error handling and rollback mechanisms  
3. **Performance Risks**: Implement caching and optimize queries early
4. **Concurrency Issues**: Use proper locking mechanisms for room allocation
5. **Notification Delivery**: Implement retry mechanisms and fallback notification methods