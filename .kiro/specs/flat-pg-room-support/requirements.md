# Requirements Document

## Introduction

This feature extends the hostel management system to support two distinct room types: **PG Rooms** and **Flats**. Currently the system treats all rooms uniformly. The extension introduces a `roomType` field on the Room entity, a `planType` field on Room Agreement Plans, and a new `FLAT` agreement type in the agreement creation workflow. Flat agreements differ from PG Room agreements in that a single primary tenant account holds the agreement while additional co-tenant names (free-text, not user accounts) can be recorded. All billing, payments, and reminders continue to flow through the single primary tenant account. PG Room behaviour remains unchanged.

---

## Glossary

- **System**: The hostel management application (Spring Boot backend + React frontend).
- **Owner**: The hostel owner who manages rooms, plans, and agreements.
- **Tenant**: A registered user account that holds a room agreement.
- **Primary_Tenant**: The single registered user account that is the agreement holder for a Flat.
- **Co_Tenant**: An additional occupant of a Flat whose name is recorded on the agreement but who does not have a separate user account in the system.
- **Room**: A physical room entity stored in PostgreSQL, belonging to a Floor and Hostel.
- **RoomType**: An enumeration with values `PG_ROOM` and `FLAT` that classifies a Room.
- **PG_Room**: A room of type `PG_ROOM`, supporting multiple beds each occupied by an individual tenant account.
- **Flat**: A room of type `FLAT`, assigned to exactly one Primary_Tenant account with optional Co_Tenant names.
- **RoomAgreementPlan**: A MongoDB document defining billing terms for an agreement.
- **PlanType**: An enumeration with values `PG_ROOM` and `FLAT` that classifies a RoomAgreementPlan.
- **Agreement**: A MongoDB document linking a Tenant, a Room, and a RoomAgreementPlan.
- **AgreementType**: An enumeration with values `ROOM`, `WORKER`, and `FLAT`.
- **RoomAllotment**: A PostgreSQL record created when an agreement is activated, linking a Room to a Tenant.
- **AgreementStepper**: The multi-step frontend wizard used by the Owner to create agreements.
- **CreateRoomPage**: The frontend page used by the Owner to create a new Room.
- **CreatePlanModal**: The frontend modal used by the Owner to create a new RoomAgreementPlan.

---

## Requirements

### Requirement 1: Room Type Classification

**User Story:** As an Owner, I want to classify each room as either a PG Room or a Flat when creating it, so that the system can apply the correct agreement and billing rules for each room type.

#### Acceptance Criteria

1. WHEN an Owner opens the Create Room form, THE CreateRoomPage SHALL display a required room type selector with exactly two options: "PG Room" and "Flat".
2. IF an Owner submits the Create Room form without selecting a room type, THEN THE CreateRoomPage SHALL display an inline validation error adjacent to the room type selector and SHALL prevent the form from being submitted.
3. WHEN an Owner selects "PG Room" and submits the Create Room form with all other required fields valid, THE System SHALL persist the room with `roomType` set to `PG_ROOM`.
4. WHEN an Owner selects "Flat" and submits the Create Room form with all other required fields valid, THE System SHALL persist the room with `roomType` set to `FLAT`.
5. WHEN a Room is retrieved via the API, THE System SHALL include the `roomType` field in the response payload with the value `PG_ROOM` or `FLAT`.
6. IF the Create Room API receives a `roomType` value that is neither `PG_ROOM` nor `FLAT`, THEN THE System SHALL return an HTTP 400 response with an error message identifying the invalid field.

---

### Requirement 2: Flat Occupancy Model

**User Story:** As an Owner, I want Flat rooms to be assigned to exactly one primary tenant account with optional co-tenant names recorded on the agreement, so that billing and communications remain centralised to a single account.

#### Acceptance Criteria

1. WHEN a Flat agreement transitions to ACTIVE status, THE System SHALL create exactly one RoomAllotment record linking the Flat room to the Primary_Tenant, provided no active RoomAllotment already exists for that Flat room.
2. THE Agreement document for a Flat SHALL store a `coTenantNames` field as a list of free-text strings, each no longer than 100 characters.
3. WHEN a Flat agreement is created, THE System SHALL accept a `coTenantNames` list containing between 0 and 5 entries, each no longer than 100 characters.
4. WHILE a Flat agreement is ACTIVE, THE System SHALL route all billing records, payment requests, and reminder notifications through the Primary_Tenant account only.
5. WHILE a Flat agreement is ACTIVE, THE System SHALL NOT create RoomAllotment records for any Co_Tenant name in the `coTenantNames` list.
6. THE System SHALL NOT validate Co_Tenant name strings against registered user accounts.
7. WHEN a Flat agreement is created with an empty `coTenantNames` list, THE System SHALL accept the agreement without requiring any co-tenant information.
8. IF a Flat agreement activation is attempted and an active RoomAllotment already exists for the same Flat room, THEN THE System SHALL reject the activation with an HTTP 409 response and an error message stating that the room is already occupied.

---

### Requirement 3: Plan Type Classification

**User Story:** As an Owner, I want to tag each Room Agreement Plan as either a PG Room plan or a Flat plan when creating it, so that only compatible plans are shown during agreement creation.

#### Acceptance Criteria

1. THE RoomAgreementPlan document SHALL include a `planType` field with allowed values `PG_ROOM` and `FLAT`.
2. WHEN an Owner opens the Create Plan form, THE CreatePlanModal SHALL display a required `planType` selector with options "PG Room" and "Flat".
3. IF an Owner submits the Create Plan form without selecting a `planType`, THEN THE System SHALL display an inline validation error adjacent to the `planType` selector and SHALL prevent submission.
4. WHEN a RoomAgreementPlan is persisted, THE System SHALL store the selected `planType` value (`PG_ROOM` or `FLAT`) on the plan document.
5. WHEN the plans API is called with a `planType` query parameter of `PG_ROOM`, THE System SHALL return only plans whose `planType` is `PG_ROOM` or whose `planType` field is absent (null/missing).
6. WHEN the plans API is called with a `planType` query parameter of `FLAT`, THE System SHALL return only plans whose `planType` is `FLAT`.

---

### Requirement 4: Flat Agreement Type in the Agreement Stepper

**User Story:** As an Owner, I want a dedicated "Flat agreement" option in the agreement creation wizard, so that I can create agreements specifically for Flat rooms with the correct plan and co-tenant fields.

#### Acceptance Criteria

1. THE `AgreementType` enumeration SHALL include the value `FLAT` in addition to the existing `ROOM` and `WORKER` values.
2. THE AgreementTypeStep SHALL display a "Flat agreement" card alongside the existing "Room agreement" and "Worker agreement" cards.
3. WHEN an Owner selects the "Flat agreement" card, THE AgreementStepper SHALL set `agreementType` to `FLAT` in the form state and advance to the User Select step.
4. WHEN the `FLAT` agreement type is active, THE UserSelectStep SHALL set the user role filter to `TENANT` (same as the `ROOM` type), so that only registered tenant accounts are selectable as the Primary_Tenant.
5. WHEN the `FLAT` agreement type is active, THE AgreementFormStep SHALL request plans filtered by `planType=FLAT` and SHALL display only the returned plans in the plan selector.
6. WHEN the `FLAT` agreement type is active, THE AgreementFormStep SHALL request rooms filtered by `roomType=FLAT` and SHALL display only the returned rooms in the room selector.
7. WHEN the `FLAT` agreement type is active, THE AgreementFormStep SHALL display a co-tenant names section containing an "Add co-tenant" button and a list of entered names, each with a remove button.
8. WHEN an Owner clicks "Add co-tenant" and enters a non-empty name string of up to 100 characters, THE AgreementFormStep SHALL append the name to the co-tenant list displayed in the form.
9. WHEN an Owner clicks the remove button next to a co-tenant name, THE AgreementFormStep SHALL remove that entry from the co-tenant list.
10. WHEN an Owner submits a Flat agreement, THE System SHALL persist the agreement with `type` set to `FLAT` and the `coTenantNames` list from the form state.

---

### Requirement 5: PG Room Agreement Type Filtering

**User Story:** As an Owner, I want the "Room agreement" flow to show only PG Room plans and PG Room rooms, so that incompatible plans and rooms are never mixed with PG Room agreements.

#### Acceptance Criteria

1. WHEN the `ROOM` agreement type is active, THE AgreementFormStep SHALL request plans filtered by `planType=PG_ROOM` and SHALL display only the returned plans (including plans with no `planType` set) in the plan selector.
2. WHEN the `ROOM` agreement type is active, THE AgreementFormStep SHALL request rooms filtered by `roomType=PG_ROOM` and SHALL display only the returned rooms in the room selector.
3. IF the plans API returns an empty list for `planType=PG_ROOM`, THEN THE AgreementFormStep SHALL display the message "No PG Room plans available. Please create a PG Room plan first." and SHALL disable the plan selector and the Next button.
4. IF the rooms API returns an empty list for the selected floor with `roomType=PG_ROOM`, THEN THE AgreementFormStep SHALL display the message "No PG Rooms available on this floor." and SHALL disable the room selector.

---

### Requirement 6: Room Listing Filtered by Room Type

**User Story:** As an Owner, I want the room selection API to support filtering by room type, so that the frontend can request only rooms of the relevant type during agreement creation.

#### Acceptance Criteria

1. WHEN the room listing API is called with a `roomType=PG_ROOM` query parameter, THE System SHALL return only Rooms whose `roomType` is `PG_ROOM`, applying this filter in addition to any existing hostel, floor, and active-status filters.
2. WHEN the room listing API is called with a `roomType=FLAT` query parameter, THE System SHALL return only Rooms whose `roomType` is `FLAT`, applying this filter in addition to any existing hostel, floor, and active-status filters.
3. WHEN the room listing API is called without a `roomType` query parameter, THE System SHALL return all Rooms regardless of `roomType`, preserving existing behaviour.
4. IF the room listing API is called with a `roomType` value that is neither `PG_ROOM` nor `FLAT`, THEN THE System SHALL return an HTTP 400 response with an error message identifying the invalid `roomType` value.
5. WHEN the room listing API is called with a `roomType` query parameter and no rooms match the combined filter criteria, THE System SHALL return an HTTP 200 response with an empty list.

---

### Requirement 7: Agreement Review and Persistence for Flat Agreements

**User Story:** As an Owner, I want the agreement review step to display co-tenant names for Flat agreements, so that I can verify the details before confirming.

#### Acceptance Criteria

1. WHEN the `FLAT` agreement type is active and the Owner reaches the review step, THE AgreementReviewStep SHALL display a "Co-tenants" section listing each name from the `coTenantNames` form state, or the text "None" if the list is empty.
2. WHEN the `FLAT` agreement type is active and the Owner reaches the review step, THE AgreementReviewStep SHALL display the Primary_Tenant's display name and the selected Flat room number.
3. WHEN the Owner clicks "Confirm & Create" on a Flat agreement, THE System SHALL validate that `type` is `FLAT`, `roomId` is present, `userId` is present, and `coTenantNames` is a list (empty list is valid); IF any of `type`, `roomId`, or `userId` is absent, THEN THE System SHALL return an HTTP 400 response identifying the missing field.
4. WHEN the Owner clicks "Confirm & Create" on a Flat agreement and all required fields are present, THE System SHALL persist the Agreement document with `type=FLAT`, the provided `roomId`, `userId`, `planId`, `startDate`, and `coTenantNames` list, and SHALL return the agreement ID and QR token in the response.

---

### Requirement 8: Backward Compatibility

**User Story:** As an Owner, I want existing PG Room agreements, plans, and rooms to continue working without modification, so that the new feature does not disrupt current operations.

#### Acceptance Criteria

1. WHEN the System reads a Room record whose `roomType` column is NULL, THE System SHALL treat that room as `PG_ROOM` in all filtering, display, and allotment logic.
2. WHEN the System reads a RoomAgreementPlan document whose `planType` field is absent or null, THE System SHALL treat that plan as `PG_ROOM` in all filtering and display logic.
3. WHEN the System processes an Agreement document whose `type` is `ROOM`, THE System SHALL apply the same activation, allotment, payment scheduling, and reminder logic as before this feature was introduced, without any change in behaviour.
4. WHEN the plans API is called with `planType=PG_ROOM`, THE System SHALL include plans whose `planType` field is absent or null in the returned list, alongside plans explicitly set to `PG_ROOM`.
