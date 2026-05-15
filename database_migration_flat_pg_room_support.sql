-- Database migration for Flat/PG Room type support
-- Adds room_type column to the rooms table to distinguish between PG Rooms and Flats
--
-- Requirements: 1.3, 1.4, 8.1
--
-- IMPORTANT: Run this script before deploying the flat-pg-room-support backend changes.
-- Existing rows will have room_type = NULL after migration.
-- Service logic treats NULL as PG_ROOM for full backward compatibility (Requirement 8.1).

ALTER TABLE rooms ADD COLUMN room_type VARCHAR(20) DEFAULT 'PG_ROOM';

-- Add an index to support efficient filtering by room_type in the active rooms query
CREATE INDEX IF NOT EXISTS idx_rooms_room_type ON rooms(room_type);

-- Verify the column was added successfully
SELECT
    column_name,
    data_type,
    character_maximum_length,
    column_default,
    is_nullable
FROM information_schema.columns
WHERE table_name = 'rooms'
  AND column_name = 'room_type';

COMMENT ON COLUMN rooms.room_type IS 'Room type classification: PG_ROOM for PG rooms, FLAT for flat units. NULL is treated as PG_ROOM for backward compatibility.';
