package edu.citadel.api;

import edu.citadel.api.request.WeatherRequest;
import edu.citadel.dal.ActivityRepository;
import edu.citadel.dal.model.Activity;
import edu.citadel.dal.model.ActivityWeather;
import edu.citadel.weather.HistoricalWeatherClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/users/{userId}/activities/{activityId}/weather")
public class ActivityWeatherEndpoints {
    private final ActivityRepository activities;
    private final HistoricalWeatherClient weatherClient;

    public ActivityWeatherEndpoints(ActivityRepository activities, HistoricalWeatherClient weatherClient) {
        this.activities = activities;
        this.weatherClient = weatherClient;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ActivityWeather attach(@PathVariable Long userId, @PathVariable Long activityId,
                                  @RequestBody WeatherRequest request) {
        if (userId <= 0 || activityId <= 0) {
            throw badRequest("User and activity IDs must be positive.");
        }
        if (request.latitude() == null || !Double.isFinite(request.latitude())
                || request.latitude() < -90 || request.latitude() > 90
                || request.longitude() == null || !Double.isFinite(request.longitude())
                || request.longitude() < -180 || request.longitude() > 180) {
            throw badRequest("Latitude must be between -90 and 90; longitude must be between -180 and 180.");
        }
        if (request.occurredAt() == null || request.occurredAt().toInstant().isAfter(Instant.now())
                || request.occurredAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDate()
                .isBefore(LocalDate.of(1940, 1, 1))) {
            throw badRequest("occurredAt must be a completed activity timestamp with a UTC offset, from 1940 onward.");
        }
        Activity activity = activities.findById(activityId)
                .filter(value -> value.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found for this user."));
        if (!request.occurredAt().toLocalDate().equals(activity.getActivityDate())) {
            throw badRequest("The local date of occurredAt must match the activity date.");
        }
        ActivityWeather cached = activity.getWeather();
        if (cached != null && request.latitude().equals(cached.getLatitude())
                && request.longitude().equals(cached.getLongitude())
                && request.occurredAt().toLocalDateTime().equals(cached.getLocalTime())
                && request.occurredAt().getOffset().toString().equals(cached.getUtcOffset())) {
            return cached;
        }
        ActivityWeather weather = weatherClient.fetch(request);
        activity.setWeather(weather);
        activities.save(activity);
        return weather;
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
