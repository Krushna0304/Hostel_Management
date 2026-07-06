package com.krunity.HostelManagment.exception;

import com.krunity.HostelManagment.enums.RoomAllotmentStatus;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(RoomAllotmentStatus from, RoomAllotmentStatus to) {
        super("Invalid allotment status transition: " + from + " → " + to
              + ". Check allowed transitions in RoomAllotmentStatus.");
    }

    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
