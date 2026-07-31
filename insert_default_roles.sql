-- Insert default roles into the roles table
-- This should be run after the roles table is created

INSERT INTO roles (role_id, name) VALUES 
('11111111-1111-1111-1111-111111111111', 'ADMIN'),
('22222222-2222-2222-2222-222222222222', 'OWNER'), 
('33333333-3333-3333-3333-333333333333', 'TENANT')
ON CONFLICT (role_id) DO NOTHING;

-- Verify the insertion
SELECT * FROM roles;