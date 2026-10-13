package edu.citadel.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Symmetric (HS256) JWT signing and verification.
 *
 * <p>This uses Spring Security's own Nimbus support rather than a third-party
 * JWT library: spring-boot-starter-oauth2-resource-server already supplies both
 * {@link NimbusJwtEncoder} for issuing and {@link NimbusJwtDecoder} for
 * validating, and the resource-server filter then enforces the token on every
 * request without any hand-written filter.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    /** HS256 requires a key of at least 256 bits. */
    private static final int MINIMUM_SECRET_BYTES = 32;

    @Bean
    public SecretKey jwtSigningKey(JwtProperties properties) {
        if (StringUtils.isBlank(properties.getSecret())) {
            throw new IllegalStateException(
                    "security.jwt.secret is not set. Export JWT_SECRET with at least "
                            + MINIMUM_SECRET_BYTES + " bytes of random data before starting the API.");
        }

        byte[] keyBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException(
                    "security.jwt.secret must be at least " + MINIMUM_SECRET_BYTES
                            + " bytes for HS256 but was " + keyBytes.length + ".");
        }

        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSigningKey, JwtProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // Rejects an expired token and anything not issued by this application,
        // on top of the signature check the decoder already performs.
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.getIssuer()));
        return decoder;
    }
}
