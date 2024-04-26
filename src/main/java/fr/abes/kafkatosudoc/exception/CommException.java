package fr.abes.kafkatosudoc.exception;

/**
 * Exception utilisée uniquement pour requalifier les exceptions lors d'erreurs de communication avec le CBS
 */
public class CommException extends Exception {
    public CommException(Exception exception) {
        super(exception);
    }
}
