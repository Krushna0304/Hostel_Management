package com.krunity.HostelManagment.enums;

public enum AgreementStatus {
    DRAFT,
    PENDING_TENANT_ACTION,
    ACTIVE,
    REJECTED,
    CLOSED,
    SETTLEMENT_REQUESTED,
    SETTLED
}



// Aggrement 
/* DRAFT – created but not sent to tenant
PENDING_TENANT_ACTION – sent to tenant, awaiting their acceptance/rejection
ACTIVE – tenant has accepted; rent cycle is running
REJECTED – tenant rejected the agreement
CLOSED – formal termination of agreement (owner or tenant initiated)
SETTLEMENT_REQUESTED – tenant requested to vacate before end date
SETTLED – owner accepted settlement, final charges calculated, room available again*/