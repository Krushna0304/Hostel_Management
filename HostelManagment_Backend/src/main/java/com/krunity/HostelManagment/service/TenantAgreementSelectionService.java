package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.exception.NotFoundException;
import com.krunity.HostelManagment.model.Agreement;
import com.krunity.HostelManagment.repository.AgreementRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Selects the agreement shown in the tenant portal.
 *
 * Agreements are considered in end-date order. The first agreement that has
 * not ended is selected; when all agreements have ended, the most recent one
 * in that order is retained so the tenant can still view their last agreement.
 */
@Service
public class TenantAgreementSelectionService {

    private final AgreementRepository agreementRepository;

    public TenantAgreementSelectionService(AgreementRepository agreementRepository) {
        this.agreementRepository = agreementRepository;
    }

    public Agreement selectForTenant(UUID tenantId) {
        List<Agreement> agreements = agreementRepository.findByUserId(tenantId).stream()
                .filter(agreement -> Boolean.TRUE.equals(agreement.getIsAccepted()))
                .sorted(Comparator.comparing(
                        Agreement::getEndDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        if (agreements.isEmpty()) {
            throw new NotFoundException("No accepted agreement found for tenant");
        }

        LocalDate today = LocalDate.now();
        return agreements.stream()
                .filter(agreement -> agreement.getEndDate() != null
                        && !agreement.getEndDate().isBefore(today))
                .findFirst()
                .orElse(agreements.get(agreements.size() - 1));
    }
}
