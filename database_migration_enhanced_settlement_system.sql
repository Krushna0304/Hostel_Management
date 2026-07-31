-- Enhanced Settlement and Allotment System Database Migration
-- This script creates all necessary database changes for the enhanced settlement system
-- Run this script after backing up your existing database

-- =====================================================
-- 1. ENHANCED SETTLEMENT_REQUESTS TABLE
-- =====================================================

-- Add new columns to existing settlement_requests table
ALTER TABLE settlement_requests ADD COLUMN IF NOT EXISTS settlement_transaction_data JSONB;
ALTER TABLE settlement_requests ADD COLUMN IF NOT EXISTS early_settlement_requested BOOLEAN DEFAULT FALSE;
ALTER TABLE settlement_requests ADD COLUMN IF NOT EXISTS settlement_approved_by UUID REFERENCES users(user_id) ON DELETE SET NULL;
ALTER TABLE settlement_requests ADD COLUMN IF NOT EXISTS auto_settlement_eligible BOOLEAN DEFAULT FALSE;
ALTER TABLE settlement_requests ADD COLUMN IF NOT EXISTS room_availability_updated BOOLEAN DEFAULT FALSE;
ALTER TABLE settlement_requests ADD COLUMN IF NOT EXISTS transaction_created_at TIMESTAMP;

-- Update settlement status constraint to include new statuses
ALTER TABLE settlement_requests DROP CONSTRAINT IF EXISTS settlement_requests_status_check;
ALTER TABLE settlement_requests ADD CONSTRAINT settlement_requests_status_check 
    CHECK (status IN (
        'PENDING_OWNER_REVIEW', 
        'CALCULATION_IN_PROGRESS', 
        'PENDING_TENANT_PAYMENT', 
        'PENDING_OWNER_PAYMENT', 
        'PAYMENT_IN_PROGRESS', 
        'SETTLEMENT_TRANSACTION_CREATED',
        'SETTLEMENT_APPROVED',
        'SETTLEMENT_DONE',
        'COMPLETED', 
        'CANCELLED', 
        'REJECTED'
    ));

-- Add indexes for better performance
CREATE INDEX IF NOT EXISTS idx_settlements_transaction_created ON settlement_requests(transaction_created_at) WHERE transaction_created_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_settlements_early_settlement ON settlement_requests(early_settlement_requested) WHERE early_settlement_requested = true;
CREATE INDEX IF NOT EXISTS idx_settlements_approved_by ON settlement_requests(settlement_approved_by) WHERE settlement_approved_by IS NOT NULL;

-- =====================================================
-- 2. EXTEND ALLOTMENT REQUESTS TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS extend_allotment_requests (
    extend_request_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- References
    tenant_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    current_agreement_id VARCHAR(255) NOT NULL, -- MongoDB Agreement ID
    current_room_id UUID REFERENCES rooms(room_id) ON DELETE SET NULL,
    
    -- Extension Details
    new_plan_id UUID, -- Will reference room_agreement_plans when that table exists
    extension_start_date DATE NOT NULL,
    extension_end_date DATE NOT NULL,
    
    -- Status
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_OWNER_APPROVAL' 
        CHECK (status IN (
            'PENDING_OWNER_APPROVAL', 
            'APPROVED_PENDING_PAYMENT', 
            'PAYMENT_COMPLETED',
            'AGREEMENT_CREATED',
            'ACTIVE',
            'REJECTED', 
            'CANCELLED',
            'EXPIRED'
        )),
    
    -- Financial Details  
    activation_amount DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (activation_amount >= 0),
    settlement_adjustment DECIMAL(10,2) DEFAULT 0,
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (total_amount >= 0),
    payment_reference VARCHAR(255),
    
    -- New Agreement Reference (after creation)
    new_agreement_id VARCHAR(255),
    
    -- Notes
    tenant_notes VARCHAR(500),
    owner_notes VARCHAR(500),
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP,
    payment_completed_at TIMESTAMP,
    expires_at TIMESTAMP, -- Auto-cancellation time if payment not completed
    
    -- Constraints
    CONSTRAINT valid_extension_dates CHECK (extension_end_date > extension_start_date),
    CONSTRAINT valid_extension_start CHECK (extension_start_date >= CURRENT_DATE),
    CONSTRAINT unique_active_extension_per_agreement 
        UNIQUE (current_agreement_id) 
        DEFERRABLE INITIALLY DEFERRED
);

-- Indexes for extend_allotment_requests
CREATE INDEX IF NOT EXISTS idx_extend_requests_tenant_status ON extend_allotment_requests(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_extend_requests_owner_status ON extend_allotment_requests(owner_id, status);  
CREATE INDEX IF NOT EXISTS idx_extend_requests_current_agreement ON extend_allotment_requests(current_agreement_id);
CREATE INDEX IF NOT EXISTS idx_extend_requests_expires_at ON extend_allotment_requests(expires_at) WHERE expires_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_extend_requests_created_at ON extend_allotment_requests(created_at);

-- =====================================================
-- 3. ENHANCED ROOM_ALLOTMENTS TABLE
-- =====================================================

-- Add new statuses to room_allotments (preserve existing data)
ALTER TABLE room_allotments DROP CONSTRAINT IF EXISTS room_allotments_room_allotment_status_check;
ALTER TABLE room_allotments ADD CONSTRAINT room_allotments_room_allotment_status_check 
    CHECK (room_allotment_status IN (
        'UPCOMING',
        'ACTIVE', 
        'SETTLEMENT_PENDING',
        'SETTLEMENT_REQUESTED',
        'ON_NOTICE_PERIOD',
        'ALLOTMENT_ACTION_PENDING',
        'SETTLEMENT_TASK',
        'LEFT'
    ));

-- Add extension tracking fields
ALTER TABLE room_allotments ADD COLUMN IF NOT EXISTS extension_request_id UUID 
    REFERENCES extend_allotment_requests(extend_request_id) ON DELETE SET NULL;
ALTER TABLE room_allotments ADD COLUMN IF NOT EXISTS is_extension BOOLEAN DEFAULT FALSE;
ALTER TABLE room_allotments ADD COLUMN IF NOT EXISTS parent_allotment_id UUID 
    REFERENCES room_allotments(allotment_id) ON DELETE SET NULL;
ALTER TABLE room_allotments ADD COLUMN IF NOT EXISTS settlement_task_type VARCHAR(50);

-- Add indexes for enhanced room allotment queries
CREATE INDEX IF NOT EXISTS idx_room_allotments_extension_request ON room_allotments(extension_request_id) WHERE extension_request_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_room_allotments_parent ON room_allotments(parent_allotment_id) WHERE parent_allotment_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_room_allotments_settlement_task ON room_allotments(settlement_task_type) WHERE settlement_task_type IS NOT NULL;

-- =====================================================
-- 4. PLAN EXPIRY NOTIFICATIONS TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS plan_expiry_notifications (
    notification_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- References
    agreement_id VARCHAR(255) NOT NULL,
    tenant_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    room_allotment_id UUID REFERENCES room_allotments(allotment_id) ON DELETE CASCADE,
    
    -- Notification Details
    notification_type VARCHAR(50) NOT NULL CHECK (notification_type IN (
        'PLAN_EXPIRY_REMINDER',
        'SETTLEMENT_WINDOW_OPEN',  
        'URGENT_ACTION_REQUIRED',
        'FINAL_NOTICE'
    )),
    
    scheduled_date DATE NOT NULL,
    sent_at TIMESTAMP,
    delivery_status VARCHAR(20) DEFAULT 'PENDING' CHECK (delivery_status IN (
        'PENDING', 'SENT', 'DELIVERED', 'FAILED', 'CANCELLED'
    )),
    
    -- Message Content
    message_template VARCHAR(50) NOT NULL,
    message_variables JSONB,
    actual_message TEXT,
    
    -- Metadata
    lead_time_days INTEGER NOT NULL CHECK (lead_time_days > 0 AND lead_time_days <= 90),
    retry_count INTEGER DEFAULT 0 CHECK (retry_count >= 0 AND retry_count <= 5),
    last_retry_at TIMESTAMP,
    max_retries INTEGER DEFAULT 3 CHECK (max_retries >= 0 AND max_retries <= 10),
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for plan_expiry_notifications
CREATE INDEX IF NOT EXISTS idx_plan_expiry_notifications_scheduled ON plan_expiry_notifications(scheduled_date, delivery_status);
CREATE INDEX IF NOT EXISTS idx_plan_expiry_notifications_tenant ON plan_expiry_notifications(tenant_id);
CREATE INDEX IF NOT EXISTS idx_plan_expiry_notifications_agreement ON plan_expiry_notifications(agreement_id);
CREATE INDEX IF NOT EXISTS idx_plan_expiry_notifications_retry ON plan_expiry_notifications(last_retry_at, retry_count) WHERE delivery_status = 'FAILED';

-- =====================================================
-- 5. ROOM OVERBOOKING LOG TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS room_overbooking_log (
    overbooking_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Room Details
    room_id UUID NOT NULL REFERENCES rooms(room_id) ON DELETE CASCADE,
    total_bed_capacity INTEGER NOT NULL CHECK (total_bed_capacity > 0),
    
    -- Overbooking Event
    event_type VARCHAR(50) NOT NULL CHECK (event_type IN (
        'AGREEMENT_CREATED',
        'ALLOCATION_SUCCESSFUL', 
        'ALLOCATION_FAILED',
        'OVERBOOKING_DETECTED',
        'CAPACITY_RESTORED'
    )),
    
    agreements_count INTEGER NOT NULL CHECK (agreements_count >= 0),
    pending_action_count INTEGER NOT NULL CHECK (pending_action_count >= 0),
    available_beds INTEGER NOT NULL,
    
    -- Agreement Reference
    agreement_id VARCHAR(255),
    tenant_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    
    -- Metadata
    allocation_order INTEGER CHECK (allocation_order > 0), -- For first-come-first-served tracking
    allocation_successful BOOLEAN,
    failure_reason TEXT,
    
    -- Timestamps
    event_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for room_overbooking_log
CREATE INDEX IF NOT EXISTS idx_overbooking_log_room_event ON room_overbooking_log(room_id, event_type);
CREATE INDEX IF NOT EXISTS idx_overbooking_log_agreement ON room_overbooking_log(agreement_id) WHERE agreement_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_overbooking_log_timestamp ON room_overbooking_log(event_timestamp);
CREATE INDEX IF NOT EXISTS idx_overbooking_log_room_timestamp ON room_overbooking_log(room_id, event_timestamp);

-- =====================================================
-- 6. SETTLEMENT TRANSACTION AUDIT TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS settlement_transaction_audit (
    audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- References
    settlement_id UUID REFERENCES settlement_requests(settlement_id) ON DELETE CASCADE,
    agreement_id VARCHAR(255) NOT NULL,
    performed_by UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    
    -- Action Details
    action_type VARCHAR(50) NOT NULL CHECK (action_type IN (
        'TRANSACTION_CREATED',
        'CALCULATION_UPDATED',
        'STATUS_CHANGED',
        'NOTIFICATION_SENT',
        'ROOM_AVAILABILITY_UPDATED'
    )),
    
    old_status VARCHAR(50),
    new_status VARCHAR(50),
    
    -- Financial Changes
    old_amount DECIMAL(10,2),
    new_amount DECIMAL(10,2),
    calculation_details JSONB,
    
    -- Metadata
    action_details JSONB,
    ip_address INET,
    user_agent TEXT,
    
    -- Timestamps
    action_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for settlement_transaction_audit
CREATE INDEX IF NOT EXISTS idx_settlement_audit_settlement ON settlement_transaction_audit(settlement_id);
CREATE INDEX IF NOT EXISTS idx_settlement_audit_agreement ON settlement_transaction_audit(agreement_id);
CREATE INDEX IF NOT EXISTS idx_settlement_audit_performed_by ON settlement_transaction_audit(performed_by);
CREATE INDEX IF NOT EXISTS idx_settlement_audit_timestamp ON settlement_transaction_audit(action_timestamp);
CREATE INDEX IF NOT EXISTS idx_settlement_audit_action_type ON settlement_transaction_audit(action_type);

-- =====================================================
-- 7. TRIGGERS FOR AUTOMATIC UPDATES
-- =====================================================

-- Update timestamp trigger for extend_allotment_requests
CREATE OR REPLACE FUNCTION update_extend_request_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_extend_request_updated_at ON extend_allotment_requests;
CREATE TRIGGER trigger_extend_request_updated_at
    BEFORE UPDATE ON extend_allotment_requests
    FOR EACH ROW
    EXECUTE FUNCTION update_extend_request_timestamp();

-- Update timestamp trigger for plan_expiry_notifications
CREATE OR REPLACE FUNCTION update_notification_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_notification_updated_at ON plan_expiry_notifications;
CREATE TRIGGER trigger_notification_updated_at
    BEFORE UPDATE ON plan_expiry_notifications
    FOR EACH ROW
    EXECUTE FUNCTION update_notification_timestamp();

-- =====================================================
-- 8. INITIAL DATA AND CONFIGURATION
-- =====================================================

-- Insert initial notification templates (if not exists)
INSERT INTO notification_templates (template_name, template_type, subject_template, body_template, created_at)
SELECT 
    'PLAN_EXPIRY_REMINDER',
    'SMS',
    'Plan Expiry Reminder',
    'Hi {tenantName}, your accommodation plan expires on {endDate}. Please take action for settlement or extension.',
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM notification_templates 
    WHERE template_name = 'PLAN_EXPIRY_REMINDER' AND template_type = 'SMS'
);

INSERT INTO notification_templates (template_name, template_type, subject_template, body_template, created_at)
SELECT 
    'SETTLEMENT_TRANSACTION_CREATED',
    'SMS',
    'Settlement Transaction Created',
    'Hi {tenantName}, a settlement transaction has been created for your accommodation. Amount: {settlementAmount}. Please check your account.',
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM notification_templates 
    WHERE template_name = 'SETTLEMENT_TRANSACTION_CREATED' AND template_type = 'SMS'
);

INSERT INTO notification_templates (template_name, template_type, subject_template, body_template, created_at)
SELECT 
    'EXTENSION_REQUEST_CREATED',
    'SMS',
    'Extension Request Received',
    'Hi {ownerName}, tenant {tenantName} has requested to extend their accommodation in room {roomNumber}. Please review and approve.',
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM notification_templates 
    WHERE template_name = 'EXTENSION_REQUEST_CREATED' AND template_type = 'SMS'
);

-- =====================================================
-- 9. MIGRATION VERIFICATION QUERIES
-- =====================================================

-- These queries can be run to verify the migration was successful
-- SELECT 'Extended Settlement Columns' as verification, 
--        column_name, data_type 
-- FROM information_schema.columns 
-- WHERE table_name = 'settlement_requests' 
-- AND column_name IN ('settlement_transaction_data', 'early_settlement_requested', 'settlement_approved_by');

-- SELECT 'Extend Allotment Requests Table' as verification,
--        COUNT(*) as table_exists
-- FROM information_schema.tables 
-- WHERE table_name = 'extend_allotment_requests';

-- SELECT 'Plan Expiry Notifications Table' as verification,
--        COUNT(*) as table_exists
-- FROM information_schema.tables 
-- WHERE table_name = 'plan_expiry_notifications';

-- SELECT 'Room Overbooking Log Table' as verification,
--        COUNT(*) as table_exists
-- FROM information_schema.tables 
-- WHERE table_name = 'room_overbooking_log';

-- =====================================================
-- MIGRATION COMPLETE
-- =====================================================

-- Log migration completion
INSERT INTO migration_log (migration_name, executed_at, status, notes)
SELECT 
    'enhanced_settlement_allotment_system_v1',
    CURRENT_TIMESTAMP,
    'COMPLETED',
    'Enhanced Settlement and Allotment System database migration completed successfully'
WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'migration_log');