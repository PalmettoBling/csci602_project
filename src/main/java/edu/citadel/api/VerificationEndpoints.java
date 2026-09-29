package edu.citadel.api;

import edu.citadel.dal.VerificationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
