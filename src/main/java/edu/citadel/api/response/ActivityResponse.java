package edu.citadel.api.response;

import edu.citadel.dal.model.Activity;

import java.time.LocalDate;

/**
 * An activity as returned by the write routes on /users/{userId}/activities.
 *
 * <p>Flat on purpose: serializing the {@link Activity} entity would nest the
 * whole owning {@code User} (name and email) inside the response, and a caller
 * that just created or edited its own row has no use for that. The existing GET
 * routes still serialize the entity directly -- changing their shape would break
 * clients built against them, so they are left alone.
 */
public record ActivityResponse(
        Long id,
        String activityType,
        Integer durationMinutes,
        LocalDate activityDate) {

    public static ActivityResponse from(Activity activity) {
        return new ActivityResponse(
                activity.getId(),
                activity.getActivityType(),
                activity.getDurationMinutes(),
                activity.getActivityDate());
    }
}
