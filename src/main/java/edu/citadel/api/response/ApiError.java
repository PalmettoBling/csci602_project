package edu.citadel.api.response;

import java.util.Map;

/**
 * Error body shared by the auth endpoints. The same shape is produced by the
 * security filter chain for 401/403 responses, so a client can parse one
 * structure regardless of where the failure happened.
 */
public record ApiError(int status, String error, String message, Map<String, String> fields) {

    public ApiError(int status, String error, String message) {
        this(status, error, message, null);
    }
}
