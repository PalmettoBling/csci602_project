package edu.citadel.dal;

import edu.citadel.dal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findAllByOwnerAccountId(Long ownerAccountId);

    Optional<User> findByIdAndOwnerAccountId(Long id, Long ownerAccountId);
}