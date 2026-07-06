package com.krunity.HostelManagment.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Payment flows that can be settled in cash via owner OTP verification.
 *
 * <p>Adding a new cash-payable flow (e.g. PARKING_CHARGE) only requires adding a
 * value here — a {@code cash_payment_allow} row is seeded automatically and no
 * schema change to the OTP tables is needed.</p>
 *
 * <p>{@code ownerConfigurable} flows are surfaced in the owner Settings screen and
 * are gated by the owner's {@code is_allowed} toggle. Non-configurable flows
 * (agreement activation, settlement) are core and always allowed.</p>
 */
public enum CashPaymentMethod {

    ELECTRICITY_BILL("Electricity Bills", true),
    INSTALLMENT("Installments", true),
    OTHER_CHARGE("Other Charges", true),
    AGREEMENT("Agreement Activation", false),
    SETTLEMENT("Settlement", false);

    private final String displayName;
    private final boolean ownerConfigurable;

    CashPaymentMethod(String displayName, boolean ownerConfigurable) {
        this.displayName = displayName;
        this.ownerConfigurable = ownerConfigurable;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isOwnerConfigurable() {
        return ownerConfigurable;
    }

    /** Methods shown in the owner Settings screen (the togglable ones). */
    public static List<CashPaymentMethod> configurableMethods() {
        return Arrays.stream(values())
                .filter(CashPaymentMethod::isOwnerConfigurable)
                .collect(Collectors.toList());
    }
}
