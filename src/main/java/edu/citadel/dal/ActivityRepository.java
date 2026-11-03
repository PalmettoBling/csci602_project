package edu.citadel.dal;

import edu.citadel.dal.model.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, Long>, JpaSpecificationExecutor<Activity> {
    List<Activity> findByUserIdOrderByActivityDateDescIdDesc(Long userId);

    /**
     * Looks an activity up by id and owner in one query. The owner is part of the
     * lookup rather than checked after the fact, so a row belonging to another
     * user is simply absent -- there is no loaded entity for a caller to leak,
     * and "unknown id" and "somebody else's id" are indistinguishable from here.
     */
    Optional<Activity> findByIdAndUserId(Long id, Long userId);

    @Query("select distinct a.activityType from Activity a where a.user.id = :userId order by a.activityType")
    List<String> findDistinctActivityTypesByUserId(@Param("userId") Long userId);
}
