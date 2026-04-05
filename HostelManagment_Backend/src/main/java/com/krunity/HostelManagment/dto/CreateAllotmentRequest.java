package com.krunity.HostelManagment.dto;

import com.krunity.HostelManagment.enums.AgreementStatus;
import com.krunity.HostelManagment.model.TenantPaymentPlan;
import com.krunity.HostelManagment.model.Transaction;
import com.krunity.HostelManagment.model.User;
import lombok.Data;

import java.sql.Date;

@Data
public class CreateAllotmentRequest {

    private String roomId;
    private User tenant;
    private String agreementId;
    private TenantPaymentPlan paymentPlanId;
    private Transaction depositTransactionId;
    private Date allotmentDate;
    private AgreementStatus roomAllotmentStatus;
}
