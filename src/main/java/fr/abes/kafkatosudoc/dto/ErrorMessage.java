package fr.abes.kafkatosudoc.dto;

import lombok.Data;

@Data
public class ErrorMessage {
    private ERROR_TYPE type;
    private String message;

    public ErrorMessage(ERROR_TYPE errorType, String message) {
        this.type = errorType;
        this.message = message;
    }
}
