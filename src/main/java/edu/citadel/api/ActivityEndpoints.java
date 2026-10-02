package edu.citadel.api;

import edu.citadel.api.request.ActivityRequestBody;
import edu.citadel.dal.ActivityRepository;
import edu.citadel.dal.UserRepository;
import edu.citadel.dal.model.Activity;
import edu.citadel.dal.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/activities")
public class ActivityEndpoints {

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    @Autowired
    public ActivityEndpoints(ActivityRepository activityRepository,
                             UserRepository userRepository) {
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Activity> createActivity(
            @RequestBody ActivityRequestBody activityRequestBody) {

        User user = userRepository.findById(activityRequestBody.getUserId()).orElse(null);

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
    public ResponseEntity<List<Activity>> getAllActivities() {
        return new ResponseEntity<>(activityRepository.findAll(), HttpStatus.OK);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Activity> getById(@PathVariable Long id) {
        Activity activity = activityRepository.findById(id).orElse(null);

        if (activity != null) {
            return new ResponseEntity<>(activity, HttpStatus.OK);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Activity> updateActivity(
            @PathVariable Long id,
            @RequestBody ActivityRequestBody activityRequestBody) {

        Activity activity = activityRepository.findById(id).orElse(null);

        if (activity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        activity.setActivityType(activityRequestBody.getActivityType());
        activity.setDurationMinutes(activityRequestBody.getDurationMinutes());
        activity.setActivityDate(activityRequestBody.getActivityDate());

        return new ResponseEntity<>(activityRepository.save(activity), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteActivity(@PathVariable Long id) {
        if (!activityRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        activityRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}