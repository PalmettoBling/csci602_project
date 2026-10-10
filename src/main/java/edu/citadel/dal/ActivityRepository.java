package edu.citadel.dal;

import edu.citadel.dal.model.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    List<Activity> findByUserId(Long userId);

    List<Activity> findAllByUser_OwnerAccountId(Long ownerAccountId);

    Optional<Activity> findByIdAndUser_OwnerAccountId(Long id, Long ownerAccountId);
}