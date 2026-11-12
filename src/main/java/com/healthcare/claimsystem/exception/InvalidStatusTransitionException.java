package com.healthcare.claimsystem.exception;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(String entity, Object currentStatus, Object targetStatus) {
        super("Cannot transition " + entity + " from " + currentStatus + " to " + targetStatus);
    }

    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
