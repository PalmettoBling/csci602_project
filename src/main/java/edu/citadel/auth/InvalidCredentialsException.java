package edu.citadel.auth;

/**
 * Raised for every failed login, whatever the underlying cause.
 *
 * <p>Deliberately carries no detail about which half of the credentials was
 * wrong. An unknown username, a wrong password and an account with no password
 * hash all surface as the same message, so the endpoint cannot be used to
 * enumerate who has an account.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
