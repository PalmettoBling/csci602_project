package edu.citadel.api;

import edu.citadel.dal.ActivityRepository;
import edu.citadel.dal.UserRepository;
import edu.citadel.dal.VerificationRepository;
import edu.citadel.dal.model.Activity;
import edu.citadel.dal.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HTTP contract checks that can run without a database; integration tests cover actual queries. */
class UserActivityEndpointsUnitTest {
    private ActivityRepository activities;
    private UserRepository users;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        activities = mock(ActivityRepository.class);
        users = mock(UserRepository.class);
        when(users.existsById(1L)).thenReturn(true);
        mvc = MockMvcBuilders.standaloneSetup(new UserActivityEndpoints(activities, users),
                new VerificationEndpoints(mock(VerificationRepository.class))).build();
    }

    @Test
    void historyReturnsRepositoryResultsAndUsesUserScopedQuery() throws Exception {
        Activity activity = new Activity(new User("Test", "test@example.com"), "Running", 20, LocalDate.of(2026, 10, 10));
        when(activities.findByUserIdOrderByActivityDateDescIdDesc(1L)).thenReturn(List.of(activity));
        mvc.perform(get("/users/1/activities")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].activityType").value("Running"));
        verify(activities).findByUserIdOrderByActivityDateDescIdDesc(1L);
    }

    @Test
    void validSearchUsesDescendingDateAndIdOrder() throws Exception {
        mvc.perform(get("/users/1/activities/search").param("type", " running ")
                .param("from", "2026-10-01").param("to", "2026-10-10")).andExpect(status().isOk());
        verify(activities).findAll(any(Specification.class), eq(Sort.by(Sort.Direction.DESC, "activityDate", "id")));
    }

    @Test
    void typesReturnRepositoryResults() throws Exception {
        when(activities.findDistinctActivityTypesByUserId(1L)).thenReturn(List.of("Cycling", "Running"));
        mvc.perform(get("/users/1/activity-types")).andExpect(status().isOk())
                .andExpect(content().json("[\"Cycling\",\"Running\"]"));
        verify(activities).findDistinctActivityTypesByUserId(1L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/activities", "/activities/search", "/activity-types"})
    void missingUserReturns404WithoutQueryingActivities(String route) throws Exception {
        mvc.perform(get("/users/2" + route)).andExpect(status().isNotFound());
        verifyNoInteractions(activities);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/activities", "/activities/search", "/activity-types"})
    void invalidUserIdsReturn400(String route) throws Exception {
        mvc.perform(get("/users/0" + route)).andExpect(status().isBadRequest());
        mvc.perform(get("/users/-1" + route)).andExpect(status().isBadRequest());
        mvc.perform(get("/users/abc" + route)).andExpect(status().isBadRequest());
        verifyNoInteractions(activities);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "2026-02-30", "2026-13-01", "10/01/2026", "2026-1-1", "garbage"})
    void invalidDatesReturn400BeforeQuerying(String date) throws Exception {
        mvc.perform(get("/users/1/activities/search").param("from", date)).andExpect(status().isBadRequest());
        mvc.perform(get("/users/1/activities/search").param("to", date)).andExpect(status().isBadRequest());
        verifyNoInteractions(activities);
    }

    @Test
    void reversedRangeAndInvalidTypesReturn400() throws Exception {
        mvc.perform(get("/users/1/activities/search").param("from", "2026-10-10").param("to", "2026-10-01"))
                .andExpect(status().isBadRequest());
        for (String type : new String[]{"", " ", "x".repeat(101)}) {
            mvc.perform(get("/users/1/activities/search").param("type", type)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(activities);
    }

    @Test
    void existingVerificationRouteRemainsAvailable() throws Exception {
        mvc.perform(get("/verification")).andExpect(status().isOk()).andExpect(content().string(""));
    }
}
