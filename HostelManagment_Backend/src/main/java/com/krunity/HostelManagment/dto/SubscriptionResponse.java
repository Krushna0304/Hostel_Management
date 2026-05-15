package com.krunity.HostelManagment.dto;

import com.krunity.HostelManagment.enums.SubscriptionTier;
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
public class SubscriptionResponse {
    private UUID subscriptionId;
    private SubscriptionTier tier;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
    private Boolean smsRemindersEnabled;
    private Boolean emailRemindersEnabled;
    private Boolean customTemplatesEnabled;
    private Integer maxHostels;
    private Integer maxTenants;
}
