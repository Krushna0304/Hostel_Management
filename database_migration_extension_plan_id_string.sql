-- Extension plans are MongoDB RoomAgreementPlan documents, whose IDs are strings/ObjectIds.
-- Run once for databases created before the extension-plan ID type was corrected.
ALTER TABLE extend_allotment_requests
    ALTER COLUMN new_plan_id TYPE VARCHAR(255)
    USING new_plan_id::text;
