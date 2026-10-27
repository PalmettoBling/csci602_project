package edu.citadel.api.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * Body of POST and PUT /users/{userId}/activities.
 *
 * <p>There is deliberately no owner field. The owner is the {@code userId} in the
 * path, which SecurityConfig has already checked against the token subject, so a
 * body that carries a {@code userId} or a {@code user} object cannot influence
 * who the row belongs to -- Jackson ignores unknown properties and there is
 * nothing here for them to bind to. This is why the existing
 * {@link ActivityRequestBody}, which does take a userId, is not reused.
 *
 * <p>Bounds mirror the V3 columns so an oversized value is a 400 rather than a
 * database error, and keep obvious nonsense out of the table.
 */
@Data
public class ActivityWriteRequest {

    /** Activities before this were not recorded with a fitness app. */
    private static final LocalDate EARLIEST_ACTIVITY_DATE = LocalDate.of(1900, 1, 1);

    /** 24 hours. Long enough for an ultramarathon, short enough to catch a typo. */
    private static final int MAX_DURATION_MINUTES = 1440;

    @NotBlank(message = "activityType is required")
    @Size(max = 100, message = "activityType must be at most 100 characters")
    private String activityType;

    @NotNull(message = "durationMinutes is required")
    @Min(value = 1, message = "durationMinutes must be at least 1")
    @Max(value = MAX_DURATION_MINUTES,
            message = "durationMinutes must be at most " + MAX_DURATION_MINUTES)
    private Integer durationMinutes;

    @NotNull(message = "activityDate is required")
    @PastOrPresent(message = "activityDate must not be in the future")
    private LocalDate activityDate;

    /**
     * Trims on the way in rather than in the controller, so {@code @NotBlank} and
     * {@code @Size} both judge the value that will actually be stored. Lombok
     * skips generating a setter that is already declared.
     */
    public void setActivityType(String activityType) {
        this.activityType = activityType == null ? null : activityType.trim();
    }

    /**
     * Lower bound on the date. Expressed as an assertion because Bean Validation
     * has no date equivalent of {@code @Min}; {@code @PastOrPresent} covers the
     * upper bound. Reported under the property name
     * {@code activityDateInSupportedRange}.
     */
    @AssertTrue(message = "activityDate must be on or after 1900-01-01")
    public boolean isActivityDateInSupportedRange() {
        // A null date is @NotNull's complaint to make, not this one's.
        return activityDate == null || !activityDate.isBefore(EARLIEST_ACTIVITY_DATE);
    }
}
