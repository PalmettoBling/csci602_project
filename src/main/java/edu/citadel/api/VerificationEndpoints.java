package edu.citadel.api;

import edu.citadel.dal.VerificationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public connectivity check for the React home page, which calls it before
 * anyone has logged in.
 *
 * <p>There is deliberately no {@code @CrossOrigin} here. This route used to
 * carry a hardcoded {@code http://localhost:5173} origin, which bypassed the
 * configurable {@code CorsConfigurationSource} in
 * {@link edu.citadel.config.SecurityConfig} and advertised a localhost origin on
 * the deployed API. CORS for every route now comes from one place:
 * {@code security.cors.allowed-origins}, which the render profile fills from the
 * FRONTEND_ORIGIN environment variable.
 */
@RestController
public class VerificationEndpoints {

    private final VerificationRepository verificationRepository;

    public VerificationEndpoints(VerificationRepository verificationRepository) {
        this.verificationRepository = verificationRepository;
    }

    @GetMapping("/verification")
    public String getVerificationCode() {
        return verificationRepository.findAll()
                .stream()
                .findFirst()
                .map(v -> v.getCode())
                .orElse("");
    }
}
