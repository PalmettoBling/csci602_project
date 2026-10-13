package edu.citadel.dal;

import edu.citadel.dal.model.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long>, JpaSpecificationExecutor<Activity> {
    List<Activity> findByUserIdOrderByActivityDateDescIdDesc(Long userId);

    @Query("select distinct a.activityType from Activity a where a.user.id = :userId order by a.activityType")
    List<String> findDistinctActivityTypesByUserId(@Param("userId") Long userId);
}
