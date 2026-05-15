package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.enums.ReminderType;
import com.krunity.HostelManagment.model.ReminderLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReminderLogRepository extends JpaRepository<ReminderLog, UUID> {
    
    Optional<ReminderLog> findBySchedule_ScheduleIdAndReminderType(UUID scheduleId, ReminderType reminderType);
    
    List<ReminderLog> findBySchedule_ScheduleId(UUID scheduleId);
    
    List<ReminderLog> findBySentAtBetween(LocalDateTime start, LocalDateTime end);
    
    boolean existsBySchedule_ScheduleIdAndReminderType(UUID scheduleId, ReminderType reminderType);
}
