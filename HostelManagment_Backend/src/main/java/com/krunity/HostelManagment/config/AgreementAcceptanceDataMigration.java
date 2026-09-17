package com.krunity.HostelManagment.config;

import com.krunity.HostelManagment.enums.AgreementStatus;
import com.krunity.HostelManagment.model.Agreement;
import com.krunity.HostelManagment.repository.AgreementRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;

/** Backfills the acceptance flag for agreements created before it existed. */
@Slf4j
@Component
public class AgreementAcceptanceDataMigration implements CommandLineRunner {

    private static final EnumSet<AgreementStatus> ACCEPTED_STATUSES = EnumSet.of(
            AgreementStatus.ACTIVE,
            AgreementStatus.PENDING_PREVIOUS_SETTLEMENT,
            AgreementStatus.SETTLEMENT_REQUESTED,
            AgreementStatus.SETTLED);

    private final AgreementRepository agreementRepository;

    public AgreementAcceptanceDataMigration(AgreementRepository agreementRepository) {
        this.agreementRepository = agreementRepository;
    }

    @Override
    public void run(String... args) {
        try {
            List<Agreement> legacyAcceptedAgreements = agreementRepository.findAll().stream()
                    .filter(agreement -> agreement.getIsAccepted() == null)
                    .filter(agreement -> ACCEPTED_STATUSES.contains(agreement.getStatus()))
                    .peek(agreement -> agreement.setIsAccepted(true))
                    .toList();

            if (!legacyAcceptedAgreements.isEmpty()) {
                agreementRepository.saveAll(legacyAcceptedAgreements);
                log.info("Backfilled isAccepted=true for {} legacy agreements", legacyAcceptedAgreements.size());
            }
        } catch (Exception ex) {
            // Do not prevent application startup if MongoDB is unavailable.
            log.warn("Agreement acceptance migration skipped: {}", ex.getMessage());
        }
    }
}
