-- ============================================================================
-- Database Migration: Add isActive field to Plans
-- Purpose: Add isActive boolean field to room_agreement_plans collection
-- Date: 2026-05-19
-- ============================================================================

-- Note: This is a MongoDB migration script
-- Run this in MongoDB shell or through your MongoDB client

-- Add isActive field to all existing plans and set to true by default
db.room_agreement_plans.updateMany(
  { isActive: { $exists: false } },
  { $set: { isActive: true } }
);

-- Verify the migration
print("Migration completed. Checking results:");
print("Total plans with isActive field:", db.room_agreement_plans.countDocuments({ isActive: { $exists: true } }));
print("Active plans:", db.room_agreement_plans.countDocuments({ isActive: true }));
print("Inactive plans:", db.room_agreement_plans.countDocuments({ isActive: false }));

-- Sample query to verify the structure
print("\nSample plan with isActive field:");
db.room_agreement_plans.findOne({}, { planName: 1, status: 1, isActive: 1, inUseFlag: 1 });