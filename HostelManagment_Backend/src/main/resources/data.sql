-- Default data initialization for Hostel Management System
-- This file is automatically executed by Spring Boot on application startup

-- Insert default roles (using specific UUIDs for consistency)
INSERT INTO roles (role_id, name) VALUES 
('11111111-1111-1111-1111-111111111111', 'ADMIN'),
('22222222-2222-2222-2222-222222222222', 'OWNER'), 
('33333333-3333-3333-3333-333333333333', 'TENANT')
ON CONFLICT (role_id) DO NOTHING;

