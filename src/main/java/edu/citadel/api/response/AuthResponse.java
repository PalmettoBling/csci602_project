package edu.citadel.api.response;

/**
 * Body of a successful POST /auth/login.
 *
 * <p>userId is echoed back so the browser can build user-scoped URLs such as
 * /users/{userId}/activities without having to decode the JWT client-side. It is
 * the same value carried in the token's subject claim, and the server always
 * re-reads the subject from the signed token -- never from the client -- when
 * deciding access.
 */
public record AuthResponse(String token, String tokenType, long expiresInSeconds,
                           Long userId, String username) {
}
