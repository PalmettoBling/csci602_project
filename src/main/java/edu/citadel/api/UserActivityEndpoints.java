package edu.citadel.api;

import edu.citadel.api.request.ActivityWriteRequest;
import edu.citadel.api.response.ActivityResponse;
import edu.citadel.dal.ActivityRepository;
import edu.citadel.dal.UserRepository;
import edu.citadel.dal.model.Activity;
import edu.citadel.dal.model.User;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * User-scoped activity reads and writes.
 *
 * <p>Every route here is owner-scoped by construction. The {@code {userId}} in
 * the path is the only thing that decides whose data is touched, and
 * SecurityConfig's {@code /users/{userId}/**} rule has already rejected a token
 * whose subject is a different id with a 403 before the dispatcher is reached.
 * Nothing in a request body can name an owner, so there is no path by which one
 * user reads or changes another user's rows.
 */
@RestController
@RequestMapping(value = "/users/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserActivityEndpoints {
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    public UserActivityEndpoints(ActivityRepository activityRepository, UserRepository userRepository) {
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/activities")
    public List<Activity> getHistory(@PathVariable Long userId) {
        requireUser(userId);
        return activityRepository.findByUserIdOrderByActivityDateDescIdDesc(userId);
    }

    @GetMapping("/activities/search")
    public List<Activity> search(@PathVariable Long userId,
                                 @RequestParam(required = false) String type,
                                 @RequestParam(required = false) String from,
                                 @RequestParam(required = false) String to) {
        if (type != null && (type.isBlank() || type.trim().length() > 100)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type must contain 1 to 100 characters");
        }
        LocalDate start = parseDate(from);
        LocalDate end = parseDate(to);
        if (start != null && end != null && start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be on or before to");
        }
        requireUser(userId);
        // Every specification starts with the user constraint; filters cannot broaden its scope.
        Specification<Activity> filter = (root, query, builder) -> builder.equal(root.get("user").get("id"), userId);
        if (type != null) {
            filter = filter.and((root, query, builder) -> builder.equal(builder.lower(root.get("activityType")), type.trim().toLowerCase(java.util.Locale.ROOT)));
        }
        if (start != null) {
            filter = filter.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("activityDate"), start));
        }
        if (end != null) {
            filter = filter.and((root, query, builder) -> builder.lessThanOrEqualTo(root.get("activityDate"), end));
        }
        return activityRepository.findAll(filter, Sort.by(Sort.Direction.DESC, "activityDate", "id"));
    }

    @GetMapping("/activity-types")
    public List<String> getActivityTypes(@PathVariable Long userId) {
        requireUser(userId);
        return activityRepository.findDistinctActivityTypesByUserId(userId);
    }

    /**
     * Records a new activity for the user named in the path.
     *
     * <p>The owner comes from {@code userId} and nowhere else:
     * {@link ActivityWriteRequest} has no owner field, so a body carrying a
     * {@code userId} or {@code user} is ignored rather than honoured.
     */
    @PostMapping(value = "/activities", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ActivityResponse> createActivity(
            @PathVariable Long userId,
            @Valid @RequestBody ActivityWriteRequest request) {
        User owner = requireUserEntity(userId);

        Activity activity = new Activity(owner, request.getActivityType(),
                request.getDurationMinutes(), request.getActivityDate());
        Activity saved = activityRepository.save(activity);

        return ResponseEntity
                .created(URI.create("/users/" + userId + "/activities/" + saved.getId()))
                .body(ActivityResponse.from(saved));
    }

    /**
     * Replaces the editable fields of one of the user's own activities.
     *
     * <p>The owner is never reassigned -- {@code setUser} is not called -- so a
     * successful edit cannot move a row to another account.
     */
    @PutMapping(value = "/activities/{activityId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ActivityResponse updateActivity(
            @PathVariable Long userId,
            @PathVariable Long activityId,
            @Valid @RequestBody ActivityWriteRequest request) {
        requireUser(userId);
        Activity activity = requireOwnedActivity(userId, activityId);

        activity.setActivityType(request.getActivityType());
        activity.setDurationMinutes(request.getDurationMinutes());
        activity.setActivityDate(request.getActivityDate());

        return ActivityResponse.from(activityRepository.save(activity));
    }

    /** Deletes one of the user's own activities. */
    @DeleteMapping("/activities/{activityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteActivity(@PathVariable Long userId, @PathVariable Long activityId) {
        requireUser(userId);
        activityRepository.delete(requireOwnedActivity(userId, activityId));
    }

    /**
     * Fetches an activity that belongs to this user.
     *
     * <p>An id that does not exist and an id owned by somebody else both produce
     * the same 404 with the same message. Distinguishing them would confirm to a
     * caller that a given activity id exists on another account.
     */
    private Activity requireOwnedActivity(Long userId, Long activityId) {
        return activityRepository.findByIdAndUserId(activityId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Activity not found"));
    }

    private void requireUser(Long userId) {
        requirePositiveUserId(userId);
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
    }

    /** Same checks as {@link #requireUser}, but hands back the row POST has to reference. */
    private User requireUserEntity(Long userId) {
        requirePositiveUserId(userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));
    }

    private void requirePositiveUserId(Long userId) {
        if (userId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId must be positive");
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null) {
            return null;
        }
        try {
            if (!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
                throw new DateTimeParseException("Invalid date format", value, 0);
            }
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dates must be valid yyyy-MM-dd values");
        }
    }
}
