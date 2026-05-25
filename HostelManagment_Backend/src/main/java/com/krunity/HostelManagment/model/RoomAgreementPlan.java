package com.krunity.HostelManagment.model;

import com.krunity.HostelManagment.enums.PlanStatus;
import com.krunity.HostelManagment.model.plan.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "room_agreement_plans")
public class RoomAgreementPlan {
    @Id
    private String id;

    private String planType; // PG_ROOM or FLAT; absent/null treated as PG_ROOM
    private String planName;
    private PlanStatus status;

    // Owner who created this plan — null means it's a global/system plan visible to all
    private java.util.UUID ownerId;
    
    // Flag to track if plan is in use (0 = New/Editable, 1 = In Use/Not Editable)
    @Builder.Default
    private Integer inUseFlag = 0;
    
    // Flag to track if plan is active (true = Active, false = Inactive/Deactivated)
    @Builder.Default
    private Boolean isActive = true;

    // Custom fields — owner can add arbitrary key-value metadata
    private java.util.Map<String, Object> customFields;

    private Duration duration;
    private PaymentModel paymentModel;
    private RentDetails rentDetails;
    private Charges charges;
    private FreeFacilities freeFacilities;
    private LatePaymentPolicy latePaymentPolicy;
    private RulesAndRegulations rulesAndRegulations;
    private Restrictions restrictions;
    private AgreementCancellationRules agreementCancellationRules;
    private Legal legal;
    private PlanAudit audit;
}
