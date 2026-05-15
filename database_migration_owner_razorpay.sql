-- ============================================================================
-- Database Migration: Owner Razorpay Configuration
-- Purpose: Implement "Bring Your Own Razorpay" model
-- Date: 2026-04-27
-- ============================================================================

-- Create owner_razorpay_configs table
CREATE TABLE IF NOT EXISTS owner_razorpay_configs (
    config_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL UNIQUE,
    razorpay_key_id VARCHAR(100) NOT NULL,
    razorpay_key_secret_encrypted VARCHAR(500) NOT NULL,
    verification_status VARCHAR(20) NOT NULL DEFAULT 'NOT_VERIFIED',
    last_verified_at TIMESTAMP,
    verification_error VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    mcp_override_disabled BOOLEAN NOT NULL DEFAULT FALSE,
    mcp_override_reason VARCHAR(500),
    mcp_override_by UUID,
    mcp_override_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_owner FOREIGN KEY (owner_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_mcp_user FOREIGN KEY (mcp_override_by) REFERENCES users(user_id) ON DELETE SET NULL,
    CONSTRAINT chk_verification_status CHECK (verification_status IN ('NOT_VERIFIED', 'VERIFYING', 'VERIFIED', 'FAILED'))
);

-- Create index for faster lookups
CREATE INDEX idx_owner_razorpay_owner_id ON owner_razorpay_configs(owner_id);
CREATE INDEX idx_owner_razorpay_verification_status ON owner_razorpay_configs(verification_status);
CREATE INDEX idx_owner_razorpay_is_active ON owner_razorpay_configs(is_active);

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_owner_razorpay_configs_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_owner_razorpay_configs_updated_at
    BEFORE UPDATE ON owner_razorpay_configs
    FOR EACH ROW
    EXECUTE FUNCTION update_owner_razorpay_configs_updated_at();

-- ============================================================================
-- IMPORTANT NOTES:
-- ============================================================================
-- 1. The razorpay_key_secret_encrypted field stores AES-256-GCM encrypted secrets
-- 2. Encryption key must be configured in application.yml: razorpay.encryption.key
-- 3. Generate encryption key using: openssl rand -base64 32
-- 4. Never expose raw secrets in API responses
-- 5. Only OWNER role can enter/modify credentials
-- 6. MCP role can monitor and override but cannot view secrets
-- 7. Payments are enabled only when:
--    - is_active = TRUE
--    - mcp_override_disabled = FALSE
--    - verification_status = 'VERIFIED'
-- ============================================================================

-- Sample query to check owner payment status
-- SELECT 
--     u.email,
--     orc.razorpay_key_id,
--     orc.verification_status,
--     orc.is_active,
--     orc.mcp_override_disabled,
--     CASE 
--         WHEN orc.is_active AND NOT orc.mcp_override_disabled AND orc.verification_status = 'VERIFIED' 
--         THEN 'ENABLED' 
--         ELSE 'DISABLED' 
--     END as payment_status
-- FROM users u
-- LEFT JOIN owner_razorpay_configs orc ON u.user_id = orc.owner_id
-- WHERE u.role = 'OWNER';
