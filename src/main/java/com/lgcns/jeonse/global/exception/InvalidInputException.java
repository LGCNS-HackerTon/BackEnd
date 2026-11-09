package com.lgcns.jeonse.global.exception;

public class InvalidInputException extends RuntimeException {
    private final String field;

    public InvalidInputException(String message, String field) {
        super(message);
        this.field = field;
    }

    public String getField() { return field; }
}
