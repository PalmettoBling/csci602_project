package edu.citadel.auth;

import edu.citadel.config.JwtProperties;
import edu.citadel.dal.model.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

/** Issues the signed tokens handed out by POST /auth/login. */
@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;

    public TokenService(JwtEncoder jwtEncoder, JwtProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    /**
     * Issues a token whose subject is the user's database id. The subject is what
     * every authorization decision reads, so it must be the id that
     * activities.user_id references -- not the username, which a user can share
     * the spelling of case-insensitively.
     */
    public String issueToken(User user) {
        Instant issuedAt = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(properties.getExpiration()))
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .build();

        // The header has to name HS256 explicitly; NimbusJwtEncoder otherwise
        // defaults to RS256 and fails against a symmetric key.
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long getExpirationSeconds() {
        return properties.getExpiration().toSeconds();
    }
}
