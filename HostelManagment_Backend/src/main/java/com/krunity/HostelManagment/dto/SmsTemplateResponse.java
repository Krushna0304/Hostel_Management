package com.krunity.HostelManagment.dto;

import com.krunity.HostelManagment.enums.ReminderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsTemplateResponse {
    private UUID templateId;
    private ReminderType reminderType;
    private String templateContent;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
