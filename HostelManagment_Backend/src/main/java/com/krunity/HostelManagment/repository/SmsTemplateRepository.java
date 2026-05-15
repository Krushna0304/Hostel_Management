package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.enums.ReminderType;
import com.krunity.HostelManagment.model.SmsTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SmsTemplateRepository extends JpaRepository<SmsTemplate, UUID> {
    
    Optional<SmsTemplate> findByOwner_UserIdAndReminderTypeAndIsActiveTrue(
        UUID ownerId, 
        ReminderType reminderType
    );
    
    List<SmsTemplate> findByOwner_UserIdAndIsActiveTrue(UUID ownerId);
    
    List<SmsTemplate> findByOwner_UserId(UUID ownerId);
}
