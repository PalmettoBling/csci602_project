package edu.citadel.api;

import edu.citadel.auth.JwtTestSupport;
import edu.citadel.config.JwtProperties;
import edu.citadel.dal.ActivityRepository;
import edu.citadel.dal.UserRepository;
import edu.citadel.dal.model.Activity;
import edu.citadel.dal.model.User;
import edu.citadel.main.RestApiApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * User A versus user B across every surviving route.
 *
 * <p>Two distinct denials are asserted, because they come from different places
 * and a reader should not have to guess which applies where:
 *
 * <ul>
 *   <li><strong>403</strong> when A addresses B's path. AuthorizationFilter
 *       compares {@code {userId}} against the token subject and rejects before
 *       the dispatcher runs, so no handler code is involved at all.</li>
 *   <li><strong>404</strong> when A addresses its own path but names one of B's
 *       activity ids. The repository resolves the row by id <em>and</em> owner,
 *       so B's row is simply absent. The same 404 answers an id that never
 *       existed, which is what stops the route being used to discover which
 *       ids belong to other accounts.</li>
 * </ul>
 *
 * <p>Every denial is followed by a re-read of B's row proving it is byte for
 * byte unchanged -- a 403 or 404 that still performed the write would otherwise
 * pass unnoticed.
 *
 * <p>Deliberately <strong>not</strong> {@code @Transactional}: a rollback would
 * hide a write that actually reached the database. Instead
 * {@link #cleanUp()} deletes the exact ids this test created, by id, never by
 * pattern -- the database is shared.
 *
 * <p>No network calls. The weather route is exercised only on paths that are
 * rejected before {@code HistoricalWeatherClient} is ever consulted, which is
 * asserted by checking that no weather snapshot is attached afterwards.
 */
@SpringBootTest(classes = RestApiApplication.class)
@TestPropertySource(locations = "classpath:jwt-test.properties")
class CrossUserAccessTest {

    @Autowired UserRepository users;
    @Autowired ActivityRepository activities;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired JwtProperties jwtProperties;
    @Autowired FilterChainProxy springSecurityFilterChain;
    @Autowired WebApplicationContext context;

    private MockMvc mvc;
    private User userA;
    private User userB;
    private Activity usersBActivity;

    private final List<Long> createdActivityIds = new ArrayList<>();
    private final List<Long> createdUserIds = new ArrayList<>();

    private static final String VALID_ACTIVITY_BODY = """
            {"activityType":"Hiking","durationMinutes":30,"activityDate":"2026-10-01"}
            """;

    /**
     * A well-formed weather request. The coordinates and timestamp are valid so
     * that the request survives field validation and reaches the ownership
     * check -- otherwise a 400 would mask whatever the ownership rule does.
     */
    private static final String VALID_WEATHER_BODY = """
            {"latitude":32.78,"longitude":-79.93,"occurredAt":"2026-10-01T14:35:00-04:00"}
            """;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();

        userA = rememberUser(users.save(new User("User A", UUID.randomUUID() + "@example.com")));
        userB = rememberUser(users.save(new User("User B", UUID.randomUUID() + "@example.com")));

        usersBActivity = rememberActivity(activities.save(
                new Activity(userB, "Fencing", 35, LocalDate.of(2026, 10, 1))));
    }

    @AfterEach
    void cleanUp() {
        // Only the exact ids created here, resolved individually. No bulk or
        // prefix delete: this database is shared with teammates.
        for (Long activityId : createdActivityIds) {
            activities.findById(activityId).ifPresent(activities::delete);
        }
        for (Long userId : createdUserIds) {
            users.findById(userId).ifPresent(users::delete);
        }
        createdActivityIds.clear();
        createdUserIds.clear();
    }

    private User rememberUser(User user) {
        createdUserIds.add(user.getId());
        return user;
    }

    private Activity rememberActivity(Activity activity) {
        createdActivityIds.add(activity.getId());
        return activity;
    }

    private MockHttpServletRequestBuilder asA(MockHttpServletRequestBuilder request) {
        return JwtTestSupport.asUser(request, jwtEncoder, jwtProperties, userA.getId().toString());
    }

    private String pathA() {
        return "/users/" + userA.getId();
    }

    private String pathB() {
        return "/users/" + userB.getId();
    }

    // ------------------------------- A addressing B's path: 403 before dispatch

    @ParameterizedTest
    @ValueSource(strings = {"/activities", "/activities/search", "/activity-types"})
    @DisplayName("A reading B's path is forbidden")
    void readingAnotherUsersPathIsForbidden(String route) throws Exception {
        mvc.perform(asA(get(pathB() + route))).andExpect(status().isForbidden());
        assertUsersBRowUnchanged();
    }

    @Test
    @DisplayName("A creating an activity on B's path is forbidden")
    void creatingOnAnotherUsersPathIsForbidden() throws Exception {
        mvc.perform(asA(post(pathB() + "/activities")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_ACTIVITY_BODY)))
                .andExpect(status().isForbidden());

        assertEquals(1, activities.findByUserIdOrderByActivityDateDescIdDesc(userB.getId()).size(),
                "the forbidden POST must not have added a row to B's history");
        assertUsersBRowUnchanged();
    }

    @Test
    @DisplayName("A editing B's activity on B's path is forbidden")
    void editingOnAnotherUsersPathIsForbidden() throws Exception {
        mvc.perform(asA(put(pathB() + "/activities/" + usersBActivity.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_ACTIVITY_BODY)))
                .andExpect(status().isForbidden());
        assertUsersBRowUnchanged();
    }

    @Test
    @DisplayName("A deleting B's activity on B's path is forbidden")
    void deletingOnAnotherUsersPathIsForbidden() throws Exception {
        mvc.perform(asA(delete(pathB() + "/activities/" + usersBActivity.getId())))
                .andExpect(status().isForbidden());
        assertUsersBRowUnchanged();
    }

    @Test
    @DisplayName("A attaching weather on B's path is forbidden and calls no provider")
    void attachingWeatherOnAnotherUsersPathIsForbidden() throws Exception {
        mvc.perform(asA(post(pathB() + "/activities/" + usersBActivity.getId() + "/weather")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_WEATHER_BODY)))
                .andExpect(status().isForbidden());
        assertUsersBRowUnchanged();
    }

    // --------------- A addressing its own path with B's activity id: 404 only

    @Test
    @DisplayName("A editing B's activity id through A's own path is not found")
    void editingAnotherUsersActivityIdIsNotFound() throws Exception {
        mvc.perform(asA(put(pathA() + "/activities/" + usersBActivity.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_ACTIVITY_BODY)))
                .andExpect(status().isNotFound());
        assertUsersBRowUnchanged();
    }

    @Test
    @DisplayName("A deleting B's activity id through A's own path is not found")
    void deletingAnotherUsersActivityIdIsNotFound() throws Exception {
        mvc.perform(asA(delete(pathA() + "/activities/" + usersBActivity.getId())))
                .andExpect(status().isNotFound());
        assertUsersBRowUnchanged();
    }

    @Test
    @DisplayName("A attaching weather to B's activity id through A's own path is not found")
    void attachingWeatherToAnotherUsersActivityIdIsNotFound() throws Exception {
        mvc.perform(asA(post(pathA() + "/activities/" + usersBActivity.getId() + "/weather")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_WEATHER_BODY)))
                .andExpect(status().isNotFound());
        assertUsersBRowUnchanged();
    }

    /**
     * An id belonging to another user and an id that never existed must be
     * indistinguishable. If they differed, the route would confirm which
     * activity ids exist on other accounts.
     */
    @Test
    @DisplayName("another user's id and a nonexistent id give the same 404")
    void foreignAndMissingIdsAreIndistinguishable() throws Exception {
        MvcResult foreign = mvc.perform(asA(delete(pathA() + "/activities/" + usersBActivity.getId())))
                .andExpect(status().isNotFound()).andReturn();
        MvcResult missing = mvc.perform(asA(delete(pathA() + "/activities/" + Long.MAX_VALUE)))
                .andExpect(status().isNotFound()).andReturn();

        assertEquals(missing.getResponse().getContentAsString(),
                foreign.getResponse().getContentAsString(),
                "a foreign id must answer exactly like an unknown id");
        assertUsersBRowUnchanged();
    }

    // ------------------------------------------------------- positive control

    /**
     * A can still do everything on its own path. Without this, every assertion
     * above would also pass if the routes were simply broken for everyone.
     */
    @Test
    @DisplayName("A's own routes all still work")
    void ownRoutesStillWork() throws Exception {
        mvc.perform(asA(get(pathA() + "/activities"))).andExpect(status().isOk());
        mvc.perform(asA(get(pathA() + "/activities/search"))).andExpect(status().isOk());
        mvc.perform(asA(get(pathA() + "/activity-types"))).andExpect(status().isOk());

        MvcResult created = mvc.perform(asA(post(pathA() + "/activities")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_ACTIVITY_BODY)))
                .andExpect(status().isCreated()).andReturn();

        long createdId = Long.parseLong(created.getResponse().getContentAsString()
                .replaceAll(".*\"id\"\\s*:\\s*(\\d+).*", "$1"));
        createdActivityIds.add(createdId);

        mvc.perform(asA(put(pathA() + "/activities/" + createdId)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"activityType":"Kayaking","durationMinutes":45,"activityDate":"2026-10-02"}
                        """)))
                .andExpect(status().isOk());

        mvc.perform(asA(delete(pathA() + "/activities/" + createdId)))
                .andExpect(status().isNoContent());

        assertUsersBRowUnchanged();
    }

    /**
     * Re-reads B's activity from the database and asserts nothing about it moved.
     * The owner check matters as much as the field values: a successful edit must
     * never be able to reassign a row to a different account.
     */
    private void assertUsersBRowUnchanged() {
        Activity stored = activities.findById(usersBActivity.getId()).orElse(null);

        assertNotNull(stored, "B's activity must still exist");
        assertEquals("Fencing", stored.getActivityType(), "B's activity type must be untouched");
        assertEquals(35, stored.getDurationMinutes(), "B's duration must be untouched");
        assertEquals(LocalDate.of(2026, 10, 1), stored.getActivityDate(),
                "B's activity date must be untouched");
        assertEquals(userB.getId(), stored.getUser().getId(),
                "B's activity must still belong to B");
        assertNull(stored.getWeather(),
                "no weather may be attached: every weather attempt was rejected before "
                        + "the provider was consulted");
    }

    /** A's history must never contain one of B's rows, whatever route is used. */
    @Test
    @DisplayName("B's activity never appears in A's history or type list")
    void anotherUsersRowsNeverLeakIntoOwnHistory() throws Exception {
        String history = mvc.perform(asA(get(pathA() + "/activities")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String types = mvc.perform(asA(get(pathA() + "/activity-types")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String search = mvc.perform(asA(get(pathA() + "/activities/search")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        for (String body : List.of(history, types, search)) {
            assertFalse(body.contains("Fencing"),
                    "B's activity must not appear in A's results: " + body);
            assertFalse(body.contains(userB.getEmail()),
                    "B's email must not appear in A's results: " + body);
        }
    }
}
