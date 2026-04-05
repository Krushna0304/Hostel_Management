package com.krunity.HostelManagment.model.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentModel {
    private String mode; // MONTHLY, QUARTERLY, YEARLY
    private String paymentTiming; // PREPAID, POSTPAID
    private Integer installments;
    private Integer dueDayOfMonth;
}

