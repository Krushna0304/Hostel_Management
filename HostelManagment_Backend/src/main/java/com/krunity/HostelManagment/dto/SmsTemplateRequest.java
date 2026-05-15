package com.krunity.HostelManagment.dto;

import com.krunity.HostelManagment.enums.ReminderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SmsTemplateRequest {
    
    @NotNull(message = "Reminder type is required")
    private ReminderType reminderType;
    
    @NotBlank(message = "Template content is required")
    @Size(max = 500, message = "Template content must not exceed 500 characters")
    private String templateContent;
}
