package edu.citadel.api;

import edu.citadel.dal.ActivityRepository;
import edu.citadel.dal.UserRepository;
import edu.citadel.dal.model.Activity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/** User-scoped reads. Future authorization can be enforced in requireUser before any activity query. */
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

    private void requireUser(Long userId) {
        if (userId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId must be positive");
        }
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
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
