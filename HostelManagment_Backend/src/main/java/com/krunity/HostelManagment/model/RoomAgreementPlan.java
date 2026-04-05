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

    private String planType; // ROOM_AGREEMENT
    private String planName;
    private PlanStatus status;

    private Duration duration;
    private PaymentModel paymentModel;
    private RentDetails rentDetails; // monthly rent amount and due date
    private Charges charges;
    private FreeFacilities freeFacilities;
    private LatePaymentPolicy latePaymentPolicy;
    private RulesAndRegulations rulesAndRegulations;
    private Restrictions restrictions;
    private AgreementCancellationRules agreementCancellationRules;
    private Legal legal;
    private PlanAudit audit;
}
