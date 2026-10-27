package edu.citadel.api;

import edu.citadel.dal.ActivityRepository;
import edu.citadel.dal.model.Activity;
import edu.citadel.dal.model.ActivityWeather;
import edu.citadel.dal.model.User;
import edu.citadel.weather.HistoricalWeatherClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ActivityWeatherEndpointsTest {
    private final ActivityRepository activities = mock(ActivityRepository.class);
    private final HistoricalWeatherClient client = mock(HistoricalWeatherClient.class);
    private MockMvc mvc;
    private Activity activity;
    private static final String BODY = """
            {"latitude":32.78,"longitude":-79.93,"occurredAt":"2026-10-01T14:35:00-04:00"}
            """;

    @BeforeEach
    void setup() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        activity = new Activity(user, "Running", 20, LocalDate.of(2026, 10, 1));
        when(activities.findById(2L)).thenReturn(Optional.of(activity));
        mvc = MockMvcBuilders.standaloneSetup(new ActivityWeatherEndpoints(activities, client))
                .setControllerAdvice(new WeatherExceptionHandler()).build();
    }

    @Test
    void savesSnapshotAndReturnsCachedResultWithoutCallingProviderAgain() throws Exception {
        ActivityWeather weather = new ActivityWeather();
        weather.setLatitude(32.78);
        weather.setLongitude(-79.93);
        weather.setLocalTime(LocalDateTime.parse("2026-10-01T14:35:00"));
        weather.setUtcOffset("-04:00");
        weather.setTemperatureC(24.5);
        when(client.fetch(any())).thenReturn(weather);
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/users/1/activities/2/weather").contentType("application/json").content(BODY))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.temperatureC").value(24.5));
        }
        verify(client, times(1)).fetch(any());
        verify(activities, times(1)).save(activity);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}", "null", "garbage",
            "{\"latitude\":91,\"longitude\":0,\"occurredAt\":\"2026-10-01T14:00:00Z\"}",
            "{\"latitude\":-91,\"longitude\":0,\"occurredAt\":\"2026-10-01T14:00:00Z\"}",
            "{\"latitude\":0,\"longitude\":181,\"occurredAt\":\"2026-10-01T14:00:00Z\"}",
            "{\"latitude\":0,\"longitude\":-181,\"occurredAt\":\"2026-10-01T14:00:00Z\"}",
            "{\"latitude\":0,\"longitude\":0,\"occurredAt\":\"2999-10-01T14:00:00Z\"}",
            "{\"latitude\":0,\"longitude\":0,\"occurredAt\":\"1939-10-01T14:00:00Z\"}",
            "{\"latitude\":0,\"longitude\":0,\"occurredAt\":\"2026-10-02T14:00:00Z\"}",
            "{\"latitude\":0,\"longitude\":0,\"occurredAt\":\"2026-10-01T14:00:00\"}"
    })
    void rejectsInvalidInputWithMeaningfulJson(String body) throws Exception {
        mvc.perform(post("/users/1/activities/2/weather").contentType("application/json").content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
        verifyNoInteractions(client);
        verify(activities, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0/activities/2", "1/activities/0", "abc/activities/2"})
    void rejectsInvalidIds(String path) throws Exception {
        mvc.perform(post("/users/" + path + "/weather").contentType("application/json").content(BODY))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
        verifyNoInteractions(client);
    }

    @Test
    void missingOrForeignActivitiesReturn404WithoutCallingProvider() throws Exception {
        for (String path : new String[]{"1/activities/999", "3/activities/2"}) {
            mvc.perform(post("/users/" + path + "/weather").contentType("application/json").content(BODY))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").isNotEmpty());
        }
        verifyNoInteractions(client);
    }

    @Test
    void failedRefreshPreservesExistingSnapshot() throws Exception {
        ActivityWeather existing = new ActivityWeather();
        activity.setWeather(existing);
        when(client.fetch(any())).thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Provider unavailable."));
        mvc.perform(post("/users/1/activities/2/weather").contentType("application/json").content(BODY))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.message").value("Provider unavailable."));
        assertSame(existing, activity.getWeather());
        verify(activities, never()).save(any());
    }

    @Test
    void changingActivityDateClearsWeatherButSameDatePreservesIt() {
        ActivityWeather weather = new ActivityWeather();
        activity.setWeather(weather);
        activity.setActivityDate(LocalDate.of(2026, 10, 1));
        assertSame(weather, activity.getWeather());
        activity.setActivityDate(LocalDate.of(2026, 10, 2));
        assertNull(activity.getWeather());
    }
}
