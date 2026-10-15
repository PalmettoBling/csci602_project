package edu.citadel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Binds the security.jwt.* configuration.
 *
 * <p>There is intentionally no default for {@link #secret} in application.yaml:
 * it resolves from the JWT_SECRET environment variable, so a deployment that
 * forgets to set it fails loudly at startup instead of silently signing tokens
 * with a value an attacker could read out of this repository.
 */
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    /** HMAC signing secret, used as raw UTF-8 bytes. Must be at least 32 bytes. */
    private String secret;

    /** How long an issued token stays valid. */
    private Duration expiration = Duration.ofHours(1);

    /** Value placed in, and required of, the "iss" claim. */
    private String issuer = "csci-602";

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Duration getExpiration() {
        return expiration;
    }

    public void setExpiration(Duration expiration) {
        this.expiration = expiration;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}
