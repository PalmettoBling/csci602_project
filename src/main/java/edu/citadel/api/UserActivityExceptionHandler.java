package edu.citadel.api;

import edu.citadel.api.response.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Renders failures from {@link UserActivityEndpoints} in the shared
 * {@link ApiError} envelope.
 *
 * <p>Scoped with {@code assignableTypes} for the same reason
 * {@link AuthExceptionHandler} is: a repository-wide advice would quietly change
 * the error bodies of the account, user and /activities endpoints that other
 * branches are built against. The two advices never overlap, so neither one has
 * to know about the other.
 *
 * <p>Without this, Bean Validation on the write routes would produce Boot's
 * default /error body, which carries no per-field detail and (with
 * {@code server.error.include-message} unset) no message either. The read routes
 * gain a {@code message} on their existing 400s and 404s; their status codes are
 * unchanged.
 */
@RestControllerAdvice(assignableTypes = UserActivityEndpoints.class)
public class UserActivityExceptionHandler {

    /** Bean-validation failures on the request body -- a missing field is one. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationFailure(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            // First message per field; later ones would just overwrite with noise.
            fields.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "The request body is missing required fields or contains invalid values.",
                fields));
    }

    /** An absent, empty or syntactically broken JSON body. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "A JSON request body is required."));
    }

    /**
     * The 400s and 404s the controller raises by hand. The reason text is written
     * in this codebase, never echoed from the request, so it is safe to return --
     * and it deliberately says only "Activity not found", which is the same answer
     * whether the id is unknown or belongs to somebody else.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex) {
        HttpStatusCode statusCode = ex.getStatusCode();
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        String reasonPhrase = status == null ? "Error" : status.getReasonPhrase();

        return ResponseEntity.status(statusCode).body(new ApiError(
                statusCode.value(),
                reasonPhrase,
                ex.getReason() == null ? reasonPhrase : ex.getReason()));
    }
}
