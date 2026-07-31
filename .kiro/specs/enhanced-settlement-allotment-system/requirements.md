# Requirements Document

## Introduction

The Enhanced Settlement and Allotment System is a comprehensive workflow management system for hostel management applications. This system handles the complete lifecycle of tenant settlements, room allotment management, and agreement extensions through interconnected workflows. The system manages plan expiry notifications, settlement processes, extend allotment requests, and complex room allocation scenarios while handling edge cases like overbooking and multiple tenant agreements.

## Glossary

- **Settlement_System**: The core settlement and allotment workflow management system
- **Plan_Expiry_Notifier**: Component responsible for sending plan expiry notifications to tenants
- **Settlement_Processor**: Component that manages settlement request workflows and state transitions
- **Allotment_Manager**: Component that handles room allotment status and availability
- **Agreement_Engine**: Component that creates and manages tenant agreements
- **Payment_Processor**: Component that handles settlement transaction processing
- **Room_Allocator**: Component that manages room allocation and overbooking scenarios
- **Notification_Service**: Service that sends notifications to tenants and owners
- **Transaction_Calculator**: Component that calculates settlement transaction amounts

## Requirements

### Requirement 1: Plan Expiry Notification Management

**User Story:** As a tenant, I want to receive notifications when my current plan is about to end, so that I can take appropriate action for settlement or extension.

#### Acceptance Criteria

1. WHEN a tenant's plan approaches expiry date, THE Plan_Expiry_Notifier SHALL send notification to the tenant
2. THE Plan_Expiry_Notifier SHALL include current agreement details and available options in the notification
3. WHEN a tenant receives plan expiry notification, THE Settlement_System SHALL enable settlement request functionality
4. THE Plan_Expiry_Notifier SHALL send notifications with configurable lead time before plan expiry

### Requirement 2: Settlement Request Workflow

**User Story:** As a tenant, I want to make settlement requests with my chosen last date, so that I can initiate the settlement process for leaving the accommodation.

#### Acceptance Criteria

1. WHEN a tenant initiates a settlement request, THE Settlement_Processor SHALL create a settlement record with status "REQUESTED"
2. THE Settlement_Processor SHALL accept settlement requests even if last date is less than agreement end date
3. WHEN a settlement request is created, THE Settlement_Processor SHALL notify the owner for approval
4. THE Settlement_Processor SHALL validate tenant eligibility and active agreement before accepting settlement requests

### Requirement 3: Settlement State Management

**User Story:** As a system administrator, I want the settlement process to follow defined state transitions, so that the workflow maintains consistency and traceability.

#### Acceptance Criteria

1. WHEN a settlement is requested, THE Settlement_Processor SHALL set status to "SETTLEMENT_REQUESTED"
2. WHEN an owner approves settlement, THE Settlement_Processor SHALL transition status to "SETTLEMENT_APPROVED"
3. WHEN a settlement transaction is created, THE Settlement_Processor SHALL transition status to "SETTLEMENT_TRANSACTION_CREATED"
4. WHEN settlement payment is completed, THE Settlement_Processor SHALL transition status to "SETTLEMENT_DONE"
5. THE Settlement_Processor SHALL prevent invalid state transitions and log all status changes

### Requirement 4: Room Availability Management After Settlement Approval

**User Story:** As an owner, I want rooms to become available for new agreements when I approve settlements, so that I can maximize occupancy and manage room allocations effectively.

#### Acceptance Criteria

1. WHEN an owner approves a settlement request, THE Allotment_Manager SHALL update the endDate in roomAllotment record
2. THE Allotment_Manager SHALL make the room visible for new agreement creation immediately after settlement approval
3. THE Allotment_Manager SHALL maintain room availability status accurately based on settlement approvals
4. THE Allotment_Manager SHALL handle concurrent settlement approvals without causing data inconsistency

### Requirement 5: Extend Allotment Request Workflow

**User Story:** As a tenant, I want to request extension of my current allotment, so that I can continue staying in the same accommodation without interruption.

#### Acceptance Criteria

1. WHEN a tenant makes an extend allotment request, THE Agreement_Engine SHALL capture user, current room, start date, and chosen plan
2. THE Agreement_Engine SHALL set start date to current agreement end date for seamless transition
3. WHEN an owner approves extend allotment request, THE Agreement_Engine SHALL create new agreement automatically
4. THE Agreement_Engine SHALL calculate final settlement transaction for the extension request
5. THE Agreement_Engine SHALL make new agreement visible on tenant side with Accept button after owner approval

### Requirement 6: Settlement Transaction Processing

**User Story:** As an owner, I want to create settlement transactions at any time based on the plan, so that I can handle financial settlements accurately and transparently.

#### Acceptance Criteria

1. THE Transaction_Calculator SHALL create settlement transactions based on current plan details
2. THE Transaction_Calculator SHALL support positive and negative settlement amounts based on calculations
3. WHEN a settlement transaction is created, THE Notification_Service SHALL notify the tenant immediately
4. THE Transaction_Calculator SHALL maintain audit trail of all settlement transaction calculations
5. THE Transaction_Calculator SHALL handle complex settlement scenarios including partial payments and adjustments

### Requirement 7: Extension Payment Processing

**User Story:** As a tenant, I want to pay the calculated amount for my allotment extension, so that I can confirm my new agreement and continue my accommodation.

#### Acceptance Criteria

1. WHEN a tenant pays for extension, THE Payment_Processor SHALL calculate total as "Activation Amount +/- Settlement Transaction Amount"
2. THE Payment_Processor SHALL validate payment completion before confirming new agreement
3. WHEN payment is completed successfully, THE Agreement_Engine SHALL activate the new agreement
4. IF payment fails or is not completed, THE Agreement_Engine SHALL cancel the new agreement after timeout

### Requirement 8: Room Allotment Status Management

**User Story:** As a system administrator, I want room allotments to have clear status tracking, so that the system can manage room availability and tenant lifecycle accurately.

#### Acceptance Criteria

1. THE Allotment_Manager SHALL support Room Allotment Status values: "UPCOMING", "ACTIVE", "ALLOTMENT_ACTION_PENDING", "LEFT"
2. THE Allotment_Manager SHALL support Settlement Status values: "SETTLEMENT_PENDING", "SETTLEMENT_TASK"
3. THE Allotment_Manager SHALL allow multiple tenants to hold multiple non-overlapping agreements for the same room
4. THE Allotment_Manager SHALL transition statuses based on agreement lifecycle and settlement events

### Requirement 9: Room Availability Display and Overbooking Management

**User Story:** As an owner, I want to see available bed counts and manage agreement allocations, so that I can handle room assignments efficiently while managing overbooking scenarios.

#### Acceptance Criteria

1. THE Room_Allocator SHALL display available bed count and agreements with "TenantActionPending" status in room dropdown
2. THE Room_Allocator SHALL handle edge case where room has 10 beds but 20 agreements are created
3. THE Room_Allocator SHALL implement first-come-first-served basis for room allocation when overbooking occurs
4. WHEN room allocation fails due to overbooking, THE Room_Allocator SHALL display "Room not available" message
5. THE Room_Allocator SHALL sort room dropdown by TenantActionPending count in ascending order

### Requirement 10: Multiple Agreement Management

**User Story:** As a tenant, I want to hold multiple non-overlapping agreements, so that I can book future accommodations while maintaining current arrangements.

#### Acceptance Criteria

1. THE Agreement_Engine SHALL allow one tenant to hold multiple agreements simultaneously
2. THE Agreement_Engine SHALL enforce non-overlapping date validation for multiple tenant agreements
3. THE Agreement_Engine SHALL prevent overlapping agreement creation and display appropriate error messages
4. THE Agreement_Engine SHALL support agreement cancellation if payment is not completed within specified timeframe

### Requirement 11: Settlement Notification System

**User Story:** As a tenant, I want to receive notifications when settlement transactions are created, so that I can track the settlement process and take necessary payment actions.

#### Acceptance Criteria

1. WHEN a settlement transaction is created by owner, THE Notification_Service SHALL send immediate notification to tenant
2. THE Notification_Service SHALL include settlement transaction details and payment instructions in notifications
3. THE Notification_Service SHALL send follow-up reminders for pending settlement payments
4. THE Notification_Service SHALL confirm settlement completion to both tenant and owner

### Requirement 12: Collection and Room Details Management

**User Story:** As an owner, I want to see relevant tenants in collection screens and room details, so that I can manage payments and room occupancy effectively.

#### Acceptance Criteria

1. THE Settlement_System SHALL display tenants with pending payments in collection screens
2. THE Settlement_System SHALL show current and upcoming tenants in room details page
3. THE Settlement_System SHALL filter tenant displays based on payment status and agreement status
4. THE Settlement_System SHALL provide clear indicators for tenants requiring immediate attention

### Requirement 13: Data Consistency and Audit Trail

**User Story:** As a system administrator, I want complete audit trail and data consistency, so that all settlement and allotment operations are traceable and reliable.

#### Acceptance Criteria

1. THE Settlement_System SHALL maintain complete audit trail of all settlement state changes
2. THE Settlement_System SHALL log all agreement creations, modifications, and cancellations
3. THE Settlement_System SHALL ensure data consistency during concurrent operations
4. THE Settlement_System SHALL provide rollback capabilities for failed transactions

### Requirement 14: Settlement Transaction Parser

**User Story:** As a developer, I want to parse settlement transaction data accurately, so that the system can process complex settlement calculations correctly.

#### Acceptance Criteria

1. WHEN settlement transaction data is provided, THE Settlement_Transaction_Parser SHALL parse it into a SettlementTransaction object
2. WHEN invalid settlement transaction data is provided, THE Settlement_Transaction_Parser SHALL return a descriptive error
3. THE Settlement_Transaction_Pretty_Printer SHALL format SettlementTransaction objects back into valid transaction data
4. FOR ALL valid SettlementTransaction objects, parsing then printing then parsing SHALL produce an equivalent object (round-trip property)

### Requirement 15: Agreement State Parser

**User Story:** As a developer, I want to parse agreement state information reliably, so that the system can handle agreement lifecycle management consistently.

#### Acceptance Criteria

1. WHEN agreement state data is provided, THE Agreement_State_Parser SHALL parse it into an AgreementState object
2. WHEN invalid agreement state data is provided, THE Agreement_State_Parser SHALL return a descriptive error
3. THE Agreement_State_Pretty_Printer SHALL format AgreementState objects back into valid state data
4. FOR ALL valid AgreementState objects, parsing then printing then parsing SHALL produce an equivalent object (round-trip property)