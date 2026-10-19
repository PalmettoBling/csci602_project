package edu.citadel.auth;

import edu.citadel.config.JwtProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;

public final class JwtTestSupport {

    private JwtTestSupport() {
    }

    public static MockHttpServletRequestBuilder asUser(
            MockHttpServletRequestBuilder request,
            JwtEncoder encoder,
            JwtProperties properties,
            String subject) {
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(properties.getExpiration()))
                .subject(subject)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
}
