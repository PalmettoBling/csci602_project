package edu.citadel.dal;

import edu.citadel.dal.model.Verification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationRepository extends JpaRepository<Verification, Integer> {
}
