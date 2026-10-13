package edu.citadel.dal;

import edu.citadel.dal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Case-insensitive lookup, matching the lower(username) unique index added
     * in V4 so that "Alice" and "alice" are the same login.
     */
    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);
}
