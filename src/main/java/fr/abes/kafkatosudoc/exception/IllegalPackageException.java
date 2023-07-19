package fr.abes.kafkatosudoc.exception;

public class IllegalPackageException extends Throwable {
    public IllegalPackageException(Exception e) {
        super(e);
    }
}
