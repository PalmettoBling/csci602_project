package edu.citadel.api;

import edu.citadel.api.request.ActivityRequestBody;
import edu.citadel.dal.AccountRepository;
import edu.citadel.dal.ActivityRepository;
import edu.citadel.dal.UserRepository;
import edu.citadel.dal.model.Account;
import edu.citadel.dal.model.Activity;
import edu.citadel.dal.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/activities")
public class ActivityEndpoints {

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    @Autowired
    public ActivityEndpoints(ActivityRepository activityRepository,
                             UserRepository userRepository,
                             AccountRepository accountRepository) {
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Activity> createActivity(
            @RequestBody ActivityRequestBody activityRequestBody,
            Principal principal) {
        Long ownerAccountId = currentAccountId(principal);
        if (ownerAccountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findByIdAndOwnerAccountId(activityRequestBody.getUserId(), ownerAccountId)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Activity activity = new Activity();
        activity.setUser(user);
        activity.setActivityType(activityRequestBody.getActivityType());
        activity.setDurationMinutes(activityRequestBody.getDurationMinutes());
        activity.setActivityDate(activityRequestBody.getActivityDate());

        Activity savedActivity = activityRepository.save(activity);

        return new ResponseEntity<>(savedActivity, HttpStatus.CREATED);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Activity>> getAllActivities(Principal principal) {
        Long ownerAccountId = currentAccountId(principal);
        if (ownerAccountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return new ResponseEntity<>(activityRepository.findAllByUser_OwnerAccountId(ownerAccountId), HttpStatus.OK);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Activity> getById(@PathVariable Long id, Principal principal) {
        Long ownerAccountId = currentAccountId(principal);
        if (ownerAccountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Activity activity = activityRepository.findByIdAndUser_OwnerAccountId(id, ownerAccountId).orElse(null);

        if (activity != null) {
            return new ResponseEntity<>(activity, HttpStatus.OK);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping(value = "/user/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Activity>> getActivitiesByUser(@PathVariable Long userId, Principal principal) {
        Long ownerAccountId = currentAccountId(principal);
        if (ownerAccountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (userRepository.findByIdAndOwnerAccountId(userId, ownerAccountId).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return new ResponseEntity<>(activityRepository.findByUserId(userId), HttpStatus.OK);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Activity> updateActivity(
            @PathVariable Long id,
            @RequestBody ActivityRequestBody activityRequestBody,
            Principal principal) {
        Long ownerAccountId = currentAccountId(principal);
        if (ownerAccountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Activity activity = activityRepository.findByIdAndUser_OwnerAccountId(id, ownerAccountId).orElse(null);

        if (activity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        activity.setActivityType(activityRequestBody.getActivityType());
        activity.setDurationMinutes(activityRequestBody.getDurationMinutes());
        activity.setActivityDate(activityRequestBody.getActivityDate());

        return new ResponseEntity<>(activityRepository.save(activity), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteActivity(@PathVariable Long id, Principal principal) {
        Long ownerAccountId = currentAccountId(principal);
        if (ownerAccountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Activity activity = activityRepository.findByIdAndUser_OwnerAccountId(id, ownerAccountId).orElse(null);
        if (activity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        activityRepository.delete(activity);
        return ResponseEntity.noContent().build();
    }

    private Long currentAccountId(Principal principal) {
        Account account = accountRepository.findAccountByUsername(principal.getName());
        return account == null ? null : account.getUser_id();
    }
}