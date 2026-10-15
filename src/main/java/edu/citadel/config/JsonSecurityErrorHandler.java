package edu.citadel.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Renders security failures as JSON.
 *
 * <p>Without this, an unauthenticated request gets the resource server's default
 * empty-bodied 401, and a browser hitting a protected path could be bounced to a
 * generated HTML login form. This API has no such form: it answers 401 and 403
 * with the same JSON envelope the auth endpoints use, so a fetch() caller can
 * always read the body.
 *
 * <p>The JSON is assembled by hand rather than through Jackson on purpose. This
 * code runs inside the filter chain, before any message converter is involved,
 * and the project has both Jackson 2 and Jackson 3 on the classpath -- binding
 * to either one here would be an avoidable coupling for a four-field object.
 */
@Component
public class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authenticationException) throws IOException {
        // Advertises the scheme without naming a realm, so no browser dialog appears.
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        write(request, response, HttpStatus.UNAUTHORIZED,
                "A valid bearer token is required to access this resource.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(request, response, HttpStatus.FORBIDDEN,
                "This token is not permitted to access this resource.");
    }

    private void write(HttpServletRequest request, HttpServletResponse response,
                       HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"status\":" + status.value()
                        + ",\"error\":\"" + escape(status.getReasonPhrase()) + "\""
                        + ",\"message\":\"" + escape(message) + "\""
                        + ",\"path\":\"" + escape(request.getRequestURI()) + "\"}");
    }

    /**
     * Escapes the characters JSON forbids inside a string literal. The request
     * URI reaches this method straight from the client, so it must never be
     * concatenated in raw.
     */
    private static String escape(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
