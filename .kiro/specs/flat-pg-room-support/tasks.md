# Implementation Plan: flat-pg-room-support

## Overview

This plan implements flat/PG room type support across the Spring Boot backend and React frontend. The work is structured in layers: database migration → backend enums/entities/DTOs → service logic → API endpoints → frontend components → property-based tests. Each task builds on the previous, ending with full integration. Backward compatibility (null roomType/planType treated as PG_ROOM) is enforced throughout.

## Tasks

- [x] 1. Database migration and backend enums
  - [x] 1.1 Write SQL migration script to add `room_type` column to `rooms` table
    - Add `ALTER TABLE rooms ADD COLUMN room_type VARCHAR(20) DEFAULT 'PG_ROOM';`
    - Existing rows will have `NULL`; service logic will treat `NULL` as `PG_ROOM`
    - Create migration file at `database_migration_flat_pg_room_support.sql`
    - _Requirements: 1.3, 1.4, 8.1_

  - [x] 1.2 Create `RoomType` and `PlanType` enums in the backend
    - Create `com.krunity.HostelManagment.enums.RoomType` with values `PG_ROOM`, `FLAT`
    - Create `com.krunity.HostelManagment.enums.PlanType` with values `PG_ROOM`, `FLAT`
    - Add `FLAT` value to the existing `AgreementType` enum
    - _Requirements: 1.3, 1.4, 3.1, 4.1_

- [x] 2. Backend entity and document updates
  - [x] 2.1 Add `roomType` field to the `Room` JPA entity
    - Add `@Enumerated(EnumType.STRING)` field `roomType` of type `RoomType` to the `Room` entity
    - Map to the `room_type` column; leave nullable to support existing rows
    - _Requirements: 1.3, 1.4, 1.5, 8.1_

  - [x] 2.2 Add `planType` and `coTenantNames` fields to MongoDB documents
    - Add `planType` field (String) to `RoomAgreementPlan` document class
    - Add `coTenantNames` field (`List<String>`) to `Agreement` document class
    - _Requirements: 3.1, 3.4, 2.2, 4.10_

- [x] 3. Backend DTOs and request/response models
  - [x] 3.1 Update `CreateRoomRequest` DTO with `roomType` validation
    - Add `@NotNull RoomType roomType` field to `CreateRoomRequest`
    - Ensure `@Valid` is applied on the controller parameter
    - _Requirements: 1.2, 1.3, 1.4, 1.6_

  - [x] 3.2 Update `CreatePlanRequest` DTO and `RoomResponse` / `PlanResponse`
    - Change `planType` in `CreatePlanRequest` from `String` to `PlanType` enum with `@NotNull`
    - Add `roomType` field to `RoomResponse` DTO
    - Ensure `PlanResponse` serialises `planType` as `"PG_ROOM"` or `"FLAT"`
    - _Requirements: 3.2, 3.3, 3.4, 1.5_

  - [x] 3.3 Create `CreateFlatAgreementRequest` DTO with validation annotations
    - Add fields: `@NotNull UUID userId`, `@NotNull UUID roomId`, `@NotBlank String planId`, `@NotNull @FutureOrPresent LocalDate startDate`, `@Size(max=5) List<@Size(max=100) String> coTenantNames`
    - Default `coTenantNames` to empty `ArrayList`
    - _Requirements: 2.3, 2.7, 7.3, 7.4_

- [x] 4. Backend service logic — room and plan filtering
  - [x] 4.1 Update `RoomService.createRoom` to persist `roomType`
    - Map `roomType` from `CreateRoomRequest` to the `Room` entity before saving
    - _Requirements: 1.3, 1.4_

  - [x] 4.2 Update `RoomService.getAllActiveRooms` to support `roomType` filter
    - Accept optional `String roomType` parameter
    - Validate the value against `RoomType` enum; return HTTP 400 for invalid values
    - Filter query: if `roomType=PG_ROOM`, include rooms where `room_type = 'PG_ROOM'` OR `room_type IS NULL`; if `roomType=FLAT`, include only `room_type = 'FLAT'`; if absent, return all
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 8.1_

  - [ ]* 4.3 Write property test for room type filter correctness (Property 3)
    - **Property 3: Room type filter correctness**
    - **Validates: Requirements 6.1, 6.2, 6.3, 8.1**
    - Use jqwik `@Property(tries=100)` with a custom `@Provide` method generating mixed room collections (PG_ROOM, FLAT, NULL)
    - Assert: `?roomType=PG_ROOM` returns rooms with `PG_ROOM` or `NULL`; `?roomType=FLAT` returns only `FLAT`; no param returns all

  - [x] 4.4 Update `RoomAgreementPlanService.getActivePlans` to support `planType` filter
    - Accept optional `String planType` parameter
    - Validate against `PlanType` enum; return HTTP 400 for invalid values
    - Filter: `planType=PG_ROOM` → return plans where `planType = 'PG_ROOM'` OR `planType` is absent/null; `planType=FLAT` → return only `FLAT` plans; absent → return all
    - _Requirements: 3.5, 3.6, 8.2, 8.4_

  - [ ]* 4.5 Write property test for plan type filter correctness (Property 5)
    - **Property 5: Plan type filter correctness**
    - **Validates: Requirements 3.5, 3.6, 8.2, 8.4**
    - Use jqwik with a `@Provide` method generating mixed plan collections (PG_ROOM, FLAT, null/absent)
    - Assert: `?planType=PG_ROOM` returns PG_ROOM and null plans; `?planType=FLAT` returns only FLAT plans

- [x] 5. Checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Backend service logic — flat agreement creation and activation
  - [x] 6.1 Implement `AgreementService.createFlatAgreement`
    - Validate that the referenced room has `roomType=FLAT` and the plan has `planType=FLAT`
    - Persist `Agreement` document with `type=FLAT`, `coTenantNames`, and all other required fields
    - Return `QrActivationResponse` with `agreementId` and `qrToken`
    - _Requirements: 4.10, 7.3, 7.4_

  - [ ]* 6.2 Write property test for flat agreement persistence round-trip (Property 6)
    - **Property 6: Flat agreement persistence round-trip**
    - **Validates: Requirements 4.10, 7.4**
    - Use jqwik to generate valid `CreateFlatAgreementRequest` instances with 0–5 co-tenant names each ≤ 100 chars
    - Assert: persisted document has `type=FLAT`, exact `coTenantNames` list, non-null `agreementId` and `qrToken`

  - [ ]* 6.3 Write property test for co-tenant name list constraints (Property 9)
    - **Property 9: Co-tenant name list constraints**
    - **Validates: Requirements 2.3, 2.7**
    - Use jqwik to generate lists with 0–5 valid entries (accepted) and lists with >5 entries or entries >100 chars (rejected)
    - Assert: valid lists → HTTP 201; invalid lists → HTTP 400

  - [x] 6.4 Update `AgreementService.activateAgreement` for the FLAT path
    - When `agreement.type == FLAT`, check for existing active `RoomAllotment` for the flat room
    - If allotment exists → throw `ConflictException` (HTTP 409) with message "Room is already occupied. An active allotment exists for this flat."
    - If no allotment → create exactly one `RoomAllotment` for the primary tenant only; do NOT create allotments for `coTenantNames`
    - Decrement `available_beds` on the room
    - _Requirements: 2.1, 2.4, 2.5, 2.8_

  - [ ]* 6.5 Write property test for flat activation creates exactly one allotment (Property 7)
    - **Property 7: Flat activation creates exactly one allotment**
    - **Validates: Requirements 2.1, 2.5**
    - Use jqwik to generate flat activation scenarios with varying `coTenantNames` lengths (0–5)
    - Assert: after activation, exactly one `RoomAllotment` exists for the flat room, linked to the primary tenant

  - [ ]* 6.6 Write property test for duplicate flat activation rejected (Property 8)
    - **Property 8: Duplicate flat activation is rejected**
    - **Validates: Requirements 2.8**
    - Use jqwik to generate flat rooms that already have an active `RoomAllotment`
    - Assert: second activation attempt returns HTTP 409

- [x] 7. Backend API controllers
  - [x] 7.1 Update `RoomController` to accept `roomType` in create and filter in list
    - Add `roomType` to the `POST /hostels/{hostelId}/{floorId}/rooms` request body binding
    - Add `@RequestParam(required = false) String roomType` to `GET .../rooms/active`
    - Delegate validation and filtering to `RoomService`
    - _Requirements: 1.2, 1.6, 6.1, 6.2, 6.3, 6.4_

  - [ ]* 7.2 Write property test for room type round-trip via API (Property 1)
    - **Property 1: Room type round-trip**
    - **Validates: Requirements 1.3, 1.4, 1.5**
    - Use jqwik to generate valid `CreateRoomRequest` instances with `roomType` in `{PG_ROOM, FLAT}`
    - POST to create, GET to retrieve, assert `roomType` in response matches submitted value

  - [ ]* 7.3 Write property test for invalid room type rejected (Property 2)
    - **Property 2: Invalid room type is rejected**
    - **Validates: Requirements 1.6, 6.4**
    - Use jqwik to generate strings not in `{PG_ROOM, FLAT}` (including empty, null, random strings)
    - Assert: API returns HTTP 400 for each invalid value

  - [x] 7.4 Update `PlanController` to accept `planType` in create and filter in list
    - Add `planType` to the `POST /api/plans` request body binding
    - Add `@RequestParam(required = false) String planType` to `GET /api/plans/active`
    - Delegate validation and filtering to `RoomAgreementPlanService`
    - _Requirements: 3.2, 3.3, 3.5, 3.6_

  - [ ]* 7.5 Write property test for plan type round-trip via API (Property 4)
    - **Property 4: Plan type round-trip**
    - **Validates: Requirements 3.4**
    - Use jqwik to generate valid `CreatePlanRequest` instances with `planType` in `{PG_ROOM, FLAT}`
    - POST to create, GET to retrieve, assert `planType` in response matches submitted value

  - [x] 7.6 Add `POST /api/agreements/flat` endpoint to `AgreementController`
    - Map `@PostMapping("/flat")` to `AgreementService.createFlatAgreement`
    - Apply `@Valid` on the `@RequestBody CreateFlatAgreementRequest`
    - Return `ResponseEntity<QrActivationResponse>` with HTTP 201
    - _Requirements: 7.3, 7.4_

  - [ ]* 7.7 Write property test for flat agreement required field validation (Property 10)
    - **Property 10: Flat agreement required field validation**
    - **Validates: Requirements 7.3**
    - Use jqwik to generate requests with each of `{type, roomId, userId}` individually absent/null
    - Assert: API returns HTTP 400 identifying the missing field for each case

  - [ ]* 7.8 Write property test for null roomType treated as PG_ROOM (Property 11)
    - **Property 11: Backward compatibility — null roomType treated as PG_ROOM**
    - **Validates: Requirements 8.1**
    - Seed rooms with `room_type = NULL` in the database
    - Assert: `?roomType=PG_ROOM` includes them; `?roomType=FLAT` excludes them

- [x] 8. Checkpoint — Ensure all backend tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Frontend — AddRoom and CreatePlanModal updates
  - [x] 9.1 Update `AddRoom.jsx` to include the room type selector
    - Add `roomType: ''` to `formData` initial state
    - Add a `<select>` (or `<SelectField>`) with options `PG_ROOM` → "PG Room" and `FLAT` → "Flat"
    - Add validation: if `roomType` is empty on submit, show inline error "Room type is required" adjacent to the selector and prevent submission
    - Include `roomType` in the `createRoom` API call payload
    - _Requirements: 1.1, 1.2, 1.3, 1.4_

  - [x] 9.2 Update `CreatePlanModal.jsx` to include the plan type selector
    - Change `planType` initial value from `'ROOM_AGREEMENT'` to `''`
    - Add a `<select>` with options `PG_ROOM` → "PG Room" and `FLAT` → "Flat"
    - Add validation: if `planType` is empty on submit, show inline error "Plan type is required" and prevent submission
    - Include `planType` in the `createPlan` API call payload
    - _Requirements: 3.2, 3.3, 3.4_

- [x] 10. Frontend — Agreement stepper updates
  - [x] 10.1 Update `AgreementTypeStep.jsx` to add the "Flat agreement" card
    - Add a third card "Flat agreement" that calls `handleTypeSelect('FLAT')`
    - Ensure the card is visually consistent with the existing "Room agreement" and "Worker agreement" cards
    - _Requirements: 4.2, 4.3_

  - [x] 10.2 Update `UserSelectStep.jsx` to handle `FLAT` agreement type
    - Extend `getRoleFromAgreementType()` to return `'TENANT'` when `agreementType === 'FLAT'`
    - _Requirements: 4.4_

  - [x] 10.3 Update `AgreementFormStep.jsx` for type-aware room/plan fetching and co-tenant UI
    - When `agreementType === 'FLAT'`: fetch rooms with `?roomType=FLAT`, fetch plans with `?planType=FLAT`
    - When `agreementType === 'ROOM'`: fetch rooms with `?roomType=PG_ROOM`, fetch plans with `?planType=PG_ROOM`
    - Add co-tenant names sub-section (visible only for FLAT): "Add co-tenant" button, list of entered names each with a remove button, max 5 entries, each ≤ 100 characters
    - Show inline error if a name exceeds 100 characters; disable "Add co-tenant" button when list has 5 entries
    - Show empty-state messages: "No Flat plans available. Please create a Flat plan first." / "No Flat rooms available on this floor." for FLAT; "No PG Room plans available. Please create a PG Room plan first." / "No PG Rooms available on this floor." for ROOM
    - Store `coTenantNames` in `formData`
    - On submit for FLAT type, call `POST /api/agreements/flat`; for ROOM type, call existing endpoint
    - _Requirements: 4.5, 4.6, 4.7, 4.8, 4.9, 5.1, 5.2, 5.3, 5.4_

  - [x] 10.4 Update `AgreementReviewStep.jsx` to display co-tenant names for FLAT agreements
    - When `agreementType === 'FLAT'`, show a "Co-tenants" section listing each name from `formData.coTenantNames`, or "None" if the list is empty
    - Show the primary tenant's display name and the selected flat room number
    - Pass `coTenantNames` in the agreement payload when calling the API
    - _Requirements: 7.1, 7.2, 7.4_

- [x] 11. Frontend — service layer updates
  - [x] 11.1 Update `agreementService.js` and `hostelService.js` with new API calls
    - Add `createFlatAgreement: (data) => apiClient.post('/api/agreements/flat', data)` to `agreementService.js`
    - Update `getActiveRoomsByFloor` in `hostelService.js` to accept an optional `roomType` parameter and append `?roomType=${roomType}` when provided
    - Update the plan-fetching service call to accept an optional `planType` parameter
    - _Requirements: 4.5, 4.6, 5.1, 5.2_

- [x] 12. Final checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP delivery
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation at key milestones
- Property tests use jqwik (JUnit 5 compatible) with `@Property(tries=100)` — add `net.jqwik:jqwik` to `pom.xml` if not already present
- Unit tests complement property tests; both are sub-tasks under their parent implementation tasks
- The SQL migration file should be run before starting the backend changes
- Backward compatibility is enforced in service logic (null roomType/planType → PG_ROOM), not at the DB level
- Property 12 (ROOM agreements unchanged) is covered by the existing test suite; no new property test task is added since it validates no regression in existing behaviour

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["2.1", "2.2"] },
    { "id": 2, "tasks": ["3.1", "3.2", "3.3"] },
    { "id": 3, "tasks": ["4.1", "4.4"] },
    { "id": 4, "tasks": ["4.2", "4.3", "4.5"] },
    { "id": 5, "tasks": ["6.1", "7.1", "7.4"] },
    { "id": 6, "tasks": ["6.2", "6.3", "6.4", "7.2", "7.3", "7.5", "7.6"] },
    { "id": 7, "tasks": ["6.5", "6.6", "7.7", "7.8"] },
    { "id": 8, "tasks": ["9.1", "9.2"] },
    { "id": 9, "tasks": ["10.1", "10.2", "11.1"] },
    { "id": 10, "tasks": ["10.3"] },
    { "id": 11, "tasks": ["10.4"] }
  ]
}
```
