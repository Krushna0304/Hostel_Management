package com.krunity.HostelManagment.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Migrates legacy allotment columns/status values after schema updates.
 */
@Slf4j
@Component
public class RoomAllotmentDataMigration implements CommandLineRunner {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void run(String... args) {
        try {
            // Only run data migrations, not schema migrations
            // Schema is now managed by Hibernate entity definitions
            fixInflatedScheduleAmounts();
        } catch (Exception ex) {
            log.warn("Data migration failed: {}", ex.getMessage());
        }
    }

    @Transactional
    public void fixInflatedScheduleAmounts() {
        try {
            int updated = entityManager.createNativeQuery("""
                    UPDATE payment_request_schedules prs
                    SET amount = tpp.installment_amount,
                        paid_amount = CASE
                            WHEN prs.payment_status = 'COMPLETED' THEN tpp.installment_amount
                            WHEN prs.paid_amount > tpp.installment_amount THEN tpp.installment_amount
                            ELSE prs.paid_amount
                        END
                    FROM tenant_payment_plans tpp
                    WHERE prs.plan_id = tpp.plan_id
                      AND prs.amount != tpp.installment_amount
                    """).executeUpdate();
            if (updated > 0) {
                log.info("Payment schedule fix: corrected {} inflated schedule entries", updated);
            }
        } catch (Exception ex) {
            log.debug("Payment schedule amount fix skipped or not needed: {}", ex.getMessage());
        }
    }

    @Transactional
    public void migrateLegacyAllotmentData() {
        // Check if allotment_date column exists first
        boolean allotmentDateColumnExists = columnExists("room_allotments", "allotment_date");
        
        // 1. Migrate statuses
        try {
            int statusUpdated = entityManager.createNativeQuery("""
                    UPDATE room_allotments
                    SET room_allotment_status = 'ACTIVE'
                    WHERE room_allotment_status IN ('CONFIRMED', 'PENDING')
                    """).executeUpdate();
            if (statusUpdated > 0) {
                log.info("Room allotment migration: status updated count={}", statusUpdated);
            }
        } catch (Exception ex) {
            log.debug("Room allotment status migration skipped or not needed: {}", ex.getMessage());
        }

        try {
            int cancelledUpdated = entityManager.createNativeQuery("""
                    UPDATE room_allotments
                    SET room_allotment_status = 'LEFT'
                    WHERE room_allotment_status = 'CANCELLED'
                    """).executeUpdate();
            if (cancelledUpdated > 0) {
                log.info("Room allotment migration: cancelled status updated count={}", cancelledUpdated);
            }
        } catch (Exception ex) {
            log.debug("Room allotment cancelled status migration skipped or not needed: {}", ex.getMessage());
        }

        // 2. Migrate legacy ARRIVED → ACTIVE (ARRIVED was removed; arrival now just sets ACTIVE)
        try {
            int arrivedUpdated = entityManager.createNativeQuery("""
                    UPDATE room_allotments
                    SET room_allotment_status = 'ACTIVE'
                    WHERE room_allotment_status = 'ARRIVED'
                    """).executeUpdate();
            if (arrivedUpdated > 0) {
                log.info("Room allotment migration: ARRIVED → ACTIVE count={}", arrivedUpdated);
            }
        } catch (Exception ex) {
            log.debug("ARRIVED migration skipped or not needed: {}", ex.getMessage());
        }

        // 3. Migrate legacy NOTICE_PERIOD → ON_NOTICE_PERIOD (renamed for clarity)
        try {
            int noticePeriodUpdated = entityManager.createNativeQuery("""
                    UPDATE room_allotments
                    SET room_allotment_status = 'ON_NOTICE_PERIOD'
                    WHERE room_allotment_status = 'NOTICE_PERIOD'
                    """).executeUpdate();
            if (noticePeriodUpdated > 0) {
                log.info("Room allotment migration: NOTICE_PERIOD → ON_NOTICE_PERIOD count={}", noticePeriodUpdated);
            }
        } catch (Exception ex) {
            log.debug("NOTICE_PERIOD migration skipped or not needed: {}", ex.getMessage());
        }

        // 5. Migrate allotment_date to start_date (only if column exists)
        if (allotmentDateColumnExists) {
            try {
                int startDateUpdated = entityManager.createNativeQuery("""
                        UPDATE room_allotments
                        SET start_date = allotment_date
                        WHERE start_date IS NULL AND allotment_date IS NOT NULL
                        """).executeUpdate();
                if (startDateUpdated > 0) {
                    log.info("Room allotment migration: start_date populated from allotment_date count={}", startDateUpdated);
                }
            } catch (Exception ex) {
                log.debug("Room allotment start_date migration skipped or not needed: {}", ex.getMessage());
            }

            // Make allotment_date nullable so inserts without it don't fail
            try {
                entityManager.createNativeQuery("""
                        ALTER TABLE room_allotments 
                        ALTER COLUMN allotment_date DROP NOT NULL
                        """).executeUpdate();
                log.info("Successfully dropped NOT NULL constraint from allotment_date column on room_allotments table");
            } catch (Exception ex) {
                log.debug("Could not alter allotment_date column (it may not exist or is already nullable): {}", ex.getMessage());
            }
        } else {
            log.debug("allotment_date column does not exist in room_allotments table, skipping migration");
        }
    }

    /**
     * Check if a column exists in a table
     */
    private boolean columnExists(String tableName, String columnName) {
        try {
            @SuppressWarnings("unchecked")
            java.util.List<Object> result = entityManager.createNativeQuery("""
                    SELECT 1 FROM information_schema.columns 
                    WHERE table_name = ? AND column_name = ?
                    """)
                    .setParameter(1, tableName)
                    .setParameter(2, columnName)
                    .getResultList();
            return !result.isEmpty();
        } catch (Exception ex) {
            log.debug("Error checking if column {} exists in {}: {}", columnName, tableName, ex.getMessage());
            return false;
        }
    }
}
