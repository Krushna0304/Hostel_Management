package com.krunity.HostelManagment.service;

import com.krunity.HostelManagment.enums.RoomAllotmentStatus;
import com.krunity.HostelManagment.exception.InvalidStatusTransitionException;
import org.springframework.stereotype.Component;

/**
 * Pure, stateless guard for allotment status transitions.
 * Delegates to the matrix encoded in {@link RoomAllotmentStatus#canTransitionTo}.
 */
@Component
public class RoomAllotmentStatusTransitionValidator {

    /**
     * Throws {@link InvalidStatusTransitionException} if the transition is not
     * in the allowed matrix.
     */
    public void validate(RoomAllotmentStatus current, RoomAllotmentStatus target) {
        if (!current.canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(current, target);
        }
    }
}
