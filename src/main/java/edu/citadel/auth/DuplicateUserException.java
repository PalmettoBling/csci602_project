package edu.citadel.auth;

/** Raised when a registration collides with an existing username or email. */
public class DuplicateUserException extends RuntimeException {

    public DuplicateUserException(String message) {
        super(message);
    }
}
