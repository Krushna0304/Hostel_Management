package com.krunity.HostelManagment.exception;

/**
 * Exception thrown when settlement calculation fails
 */
public class SettlementCalculationException extends RuntimeException {
    
    public SettlementCalculationException(String message) {
        super(message);
    }
    
    public SettlementCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}