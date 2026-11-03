package edu.citadel.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.citadel.auth.JwtTestSupport;
import edu.citadel.config.JwtProperties;
import edu.citadel.dal.ActivityRepository;
import edu.citadel.dal.UserRepository;
import edu.citadel.dal.model.Activity;
import edu.citadel.dal.model.User;
import edu.citadel.main.RestApiApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST, PUT and DELETE on /users/{userId}/activities.
 *
 * <p>Runs against the configured PostgreSQL database with real JPA queries.
 * {@code @Transactional} rolls back every fixture row and every row created
 * through the API at the end of each test, so nothing is left behind and no
 * cleanup step deletes anything this class did not create. The database is
 * shared, so there is deliberately no delete-by-pattern anywhere here.
 */
@SpringBootTest(classes = RestApiApplication.class)
@TestPropertySource(locations = "classpath:jwt-test.properties")
@Transactional
class UserActivityWriteEndpointsTest {

    /** Relative dates, so the suite does not start failing on a future calendar day. */
    private static final LocalDate TODAY = LocalDate.now();
    private static final LocalDate YESTERDAY = TODAY.minusDays(1);

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired ActivityRepository activities;
    @Autowired UserRepository users;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired JwtProperties jwtProperties;
    @Autowired FilterChainProxy springSecurityFilterChain;
    @Autowired WebApplicationContext context;

    private MockMvc mvc;
    private User owner;
    private User other;
    /** An activity belonging to {@link #other}, for the cross-user cases. */
    private Activity otherUsersActivity;
    private String base;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();
        owner = users.save(new User("Write owner", UUID.randomUUID() + "@example.com"));
        other = users.save(new User("Write other", UUID.randomUUID() + "@example.com"));
        otherUsersActivity = activities.save(new Activity(other, "Swimming", 45, YESTERDAY));
        activities.flush();
        base = "/users/" + owner.getId() + "/activities";
    }

    private MockHttpServletRequestBuilder asOwner(MockHttpServletRequestBuilder request) {
        return JwtTestSupport.asUser(request, jwtEncoder, jwtProperties, owner.getId().toString());
    }

    private MockHttpServletRequestBuilder asOther(MockHttpServletRequestBuilder request) {
        return JwtTestSupport.asUser(request, jwtEncoder, jwtProperties, other.getId().toString());
    }

    private static String body(String activityType, Object durationMinutes, Object activityDate) {
        return "{\"activityType\":" + quoted(activityType)
                + ",\"durationMinutes\":" + durationMinutes
                + ",\"activityDate\":" + quoted(activityDate) + "}";
    }

    private static String quoted(Object value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    /** Creates an activity through the API and returns the id the server assigned. */
    private long createActivity(String type, int minutes, LocalDate date) throws Exception {
        String response = mvc.perform(asOwner(post(base)
                        .contentType("application/json")
                        .content(body(type, minutes, date))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JSON.readTree(response).get("id").asLong();
    }

    // ------------------------------------------------------------------ POST

    @Test
    void postReturns201WithFlatBodyAndLocationThenGetShowsIt() throws Exception {
        mvc.perform(asOwner(post(base).contentType("application/json")
                        .content(body("Running", 30, YESTERDAY))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        matchesRegex("/users/" + owner.getId() + "/activities/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.activityType").value("Running"))
                .andExpect(jsonPath("$.durationMinutes").value(30))
                .andExpect(jsonPath("$.activityDate").value(YESTERDAY.toString()))
                // Flat DTO: the owning user is not serialized into the write responses.
                .andExpect(jsonPath("$.user").doesNotExist());

        mvc.perform(asOwner(get(base)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].activityType").value("Running"))
                .andExpect(jsonPath("$[0].durationMinutes").value(30))
                .andExpect(jsonPath("$[0].activityDate").value(YESTERDAY.toString()));
    }

    @Test
    void postTrimsTheActivityType() throws Exception {
        mvc.perform(asOwner(post(base).contentType("application/json")
                        .content(body("  Trail Running  ", 30, YESTERDAY))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activityType").value("Trail Running"));
    }

    @Test
    void postAcceptsTheBoundaryValues() throws Exception {
        mvc.perform(asOwner(post(base).contentType("application/json")
                        .content(body("Walking", 1, TODAY))))
                .andExpect(status().isCreated());
        mvc.perform(asOwner(post(base).contentType("application/json")
                        .content(body("Ultramarathon", 1440, TODAY))))
                .andExpect(status().isCreated());
        mvc.perform(asOwner(post(base).contentType("application/json")
                        .content(body("x".repeat(100), 30, LocalDate.of(1900, 1, 1)))))
                .andExpect(status().isCreated());
    }

    static Stream<Arguments> invalidBodies() {
        return Stream.of(
                Arguments.of("missing activityType",
                        "{\"durationMinutes\":30,\"activityDate\":\"" + YESTERDAY + "\"}",
                        "activityType"),
                Arguments.of("null activityType", body(null, 30, YESTERDAY), "activityType"),
                Arguments.of("blank activityType", body("   ", 30, YESTERDAY), "activityType"),
                Arguments.of("overlong activityType", body("x".repeat(101), 30, YESTERDAY),
                        "activityType"),
                Arguments.of("missing durationMinutes",
                        "{\"activityType\":\"Running\",\"activityDate\":\"" + YESTERDAY + "\"}",
                        "durationMinutes"),
                Arguments.of("null durationMinutes", body("Running", "null", YESTERDAY),
                        "durationMinutes"),
                Arguments.of("zero durationMinutes", body("Running", 0, YESTERDAY),
                        "durationMinutes"),
                Arguments.of("negative durationMinutes", body("Running", -30, YESTERDAY),
                        "durationMinutes"),
                Arguments.of("excessive durationMinutes", body("Running", 1441, YESTERDAY),
                        "durationMinutes"),
                Arguments.of("missing activityDate",
                        "{\"activityType\":\"Running\",\"durationMinutes\":30}", "activityDate"),
                Arguments.of("null activityDate", body("Running", 30, null), "activityDate"),
                Arguments.of("future activityDate", body("Running", 30, TODAY.plusDays(1)),
                        "activityDate"),
                Arguments.of("prehistoric activityDate",
                        body("Running", 30, LocalDate.of(1899, 12, 31)),
                        "activityDateInSupportedRange"));
    }

    @ParameterizedTest(name = "POST with {0} answers 400")
    @MethodSource("invalidBodies")
    void postRejectsInvalidFieldsWithTheApiErrorShape(String name, String json, String field)
            throws Exception {
        mvc.perform(asOwner(post(base).contentType("application/json").content(json)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.fields." + field).isString());

        // A rejected create stores nothing.
        mvc.perform(asOwner(get(base))).andExpect(jsonPath("$.length()").value(0));
    }

    static Stream<Arguments> malformedBodies() {
        return Stream.of(
                Arguments.of("truncated object", "{"),
                Arguments.of("not json at all", "not json at all"),
                Arguments.of("empty body", ""),
                Arguments.of("array instead of object", "[]"),
                Arguments.of("non-numeric duration",
                        "{\"activityType\":\"Running\",\"durationMinutes\":\"thirty\","
                                + "\"activityDate\":\"2026-10-01\"}"),
                Arguments.of("unparseable date",
                        "{\"activityType\":\"Running\",\"durationMinutes\":30,"
                                + "\"activityDate\":\"10/01/2026\"}"));
    }

    @ParameterizedTest(name = "POST with {0} answers 400")
    @MethodSource("malformedBodies")
    void postRejectsMalformedJson(String name, String json) throws Exception {
        mvc.perform(asOwner(post(base).contentType("application/json").content(json)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void postWithoutATokenReturns401() throws Exception {
        mvc.perform(post(base).contentType("application/json")
                        .content(body("Running", 30, YESTERDAY)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postToAnotherUsersIdReturns403AndWritesNothing() throws Exception {
        mvc.perform(asOwner(post("/users/" + other.getId() + "/activities")
                        .contentType("application/json").content(body("Running", 30, YESTERDAY))))
                .andExpect(status().isForbidden());

        mvc.perform(asOther(get("/users/" + other.getId() + "/activities")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].activityType").value("Swimming"));
    }

    @Test
    void postIgnoresAnOwnerIdSuppliedInTheBody() throws Exception {
        String hostileBody = "{\"userId\":" + other.getId()
                + ",\"user\":{\"id\":" + other.getId() + "}"
                + ",\"id\":999999"
                + ",\"activityType\":\"Rowing\",\"durationMinutes\":25"
                + ",\"activityDate\":\"" + YESTERDAY + "\"}";

        String response = mvc.perform(asOwner(post(base)
                        .contentType("application/json").content(hostileBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activityType").value("Rowing"))
                // The server assigns the id; the body cannot choose one.
                .andExpect(jsonPath("$.id").value(not(999999)))
                .andReturn().getResponse().getContentAsString();

        long createdId = JSON.readTree(response).get("id").asLong();

        assertTrue(activities.findByIdAndUserId(createdId, owner.getId()).isPresent(),
                "the new activity should belong to the user named in the path");
        assertTrue(activities.findByIdAndUserId(createdId, other.getId()).isEmpty(),
                "the new activity must not belong to the user named in the body");

        mvc.perform(asOther(get("/users/" + other.getId() + "/activities")))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].activityType").value("Swimming"));
    }

    // ------------------------------------------------------------------- PUT

    @Test
    void putReturns200AndPersistsTheChange() throws Exception {
        long id = createActivity("Running", 30, YESTERDAY);

        mvc.perform(asOwner(put(base + "/" + id).contentType("application/json")
                        .content(body("Hiking", 95, TODAY))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.activityType").value("Hiking"))
                .andExpect(jsonPath("$.durationMinutes").value(95))
                .andExpect(jsonPath("$.activityDate").value(TODAY.toString()))
                .andExpect(jsonPath("$.user").doesNotExist());

        mvc.perform(asOwner(get(base)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].activityType").value("Hiking"))
                .andExpect(jsonPath("$[0].durationMinutes").value(95))
                .andExpect(jsonPath("$[0].activityDate").value(TODAY.toString()));
    }

    @Test
    void putNeverReassignsTheOwner() throws Exception {
        long id = createActivity("Running", 30, YESTERDAY);

        mvc.perform(asOwner(put(base + "/" + id).contentType("application/json")
                        .content("{\"userId\":" + other.getId()
                                + ",\"user\":{\"id\":" + other.getId() + "}"
                                + ",\"activityType\":\"Hiking\",\"durationMinutes\":95"
                                + ",\"activityDate\":\"" + TODAY + "\"}")))
                .andExpect(status().isOk());

        assertTrue(activities.findByIdAndUserId(id, owner.getId()).isPresent(),
                "the activity should still belong to its original owner");
        assertTrue(activities.findByIdAndUserId(id, other.getId()).isEmpty(),
                "a request body must not be able to hand a row to another user");
    }

    @ParameterizedTest(name = "PUT with {0} answers 400")
    @MethodSource("invalidBodies")
    void putRejectsInvalidFields(String name, String json, String field) throws Exception {
        long id = createActivity("Running", 30, YESTERDAY);

        mvc.perform(asOwner(put(base + "/" + id).contentType("application/json").content(json)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fields." + field).isString());

        // A rejected edit leaves the stored row exactly as it was.
        mvc.perform(asOwner(get(base)))
                .andExpect(jsonPath("$[0].activityType").value("Running"))
                .andExpect(jsonPath("$[0].durationMinutes").value(30));
    }

    @ParameterizedTest(name = "PUT with {0} answers 400")
    @MethodSource("malformedBodies")
    void putRejectsMalformedJson(String name, String json) throws Exception {
        long id = createActivity("Running", 30, YESTERDAY);
        mvc.perform(asOwner(put(base + "/" + id).contentType("application/json").content(json)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void putAgainstANonexistentActivityReturns404() throws Exception {
        mvc.perform(asOwner(put(base + "/9223372036854775807").contentType("application/json")
                        .content(body("Hiking", 95, TODAY))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Activity not found"));
    }

    @Test
    void putAgainstAnotherUsersActivityReturns404WithoutRevealingWhy() throws Exception {
        mvc.perform(asOwner(put(base + "/" + otherUsersActivity.getId())
                        .contentType("application/json").content(body("Hiking", 95, TODAY))))
                .andExpect(status().isNotFound())
                // The same message an unknown id produces: the response must not
                // confirm that this id exists on another account.
                .andExpect(jsonPath("$.message").value("Activity not found"));

        Activity reloaded = activities.findById(otherUsersActivity.getId()).orElseThrow();
        assertEquals("Swimming", reloaded.getActivityType());
        assertEquals(45, reloaded.getDurationMinutes());
    }

    @Test
    void putWithoutATokenReturns401() throws Exception {
        mvc.perform(put(base + "/" + otherUsersActivity.getId())
                        .contentType("application/json").content(body("Hiking", 95, TODAY)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void putUnderAnotherUsersIdReturns403() throws Exception {
        mvc.perform(asOwner(put("/users/" + other.getId() + "/activities/"
                        + otherUsersActivity.getId())
                        .contentType("application/json").content(body("Hiking", 95, TODAY))))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- DELETE

    @Test
    void deleteReturns204AndTheActivityIsGoneFromGet() throws Exception {
        long id = createActivity("Running", 30, YESTERDAY);

        mvc.perform(asOwner(delete(base + "/" + id)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mvc.perform(asOwner(get(base)))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void deletingTheSameActivityTwiceReturns404TheSecondTime() throws Exception {
        long id = createActivity("Running", 30, YESTERDAY);
        mvc.perform(asOwner(delete(base + "/" + id))).andExpect(status().isNoContent());
        mvc.perform(asOwner(delete(base + "/" + id))).andExpect(status().isNotFound());
    }

    @Test
    void deleteOfANonexistentActivityReturns404() throws Exception {
        mvc.perform(asOwner(delete(base + "/9223372036854775807")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Activity not found"));
    }

    @Test
    void deleteOfAnotherUsersActivityReturns404AndLeavesItAlone() throws Exception {
        mvc.perform(asOwner(delete(base + "/" + otherUsersActivity.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Activity not found"));

        assertTrue(activities.findByIdAndUserId(otherUsersActivity.getId(), other.getId())
                        .isPresent(),
                "another user's activity must survive a delete attempt");
    }

    @Test
    void deleteWithoutATokenReturns401() throws Exception {
        mvc.perform(delete(base + "/" + otherUsersActivity.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteUnderAnotherUsersIdReturns403() throws Exception {
        mvc.perform(asOwner(delete("/users/" + other.getId() + "/activities/"
                        + otherUsersActivity.getId())))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------- isolation

    @Test
    void oneUsersActivitiesNeverAppearInAnothersList() throws Exception {
        createActivity("Running", 30, YESTERDAY);
        createActivity("Cycling", 60, TODAY);

        mvc.perform(asOwner(get(base)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.activityType == 'Swimming')]").isEmpty());

        mvc.perform(asOther(get("/users/" + other.getId() + "/activities")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].activityType").value("Swimming"))
                .andExpect(jsonPath("$[?(@.activityType == 'Running')]").isEmpty())
                .andExpect(jsonPath("$[?(@.activityType == 'Cycling')]").isEmpty());

        // The search and type routes are scoped the same way.
        mvc.perform(asOther(get("/users/" + other.getId() + "/activities/search")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(asOther(get("/users/" + other.getId() + "/activity-types")))
                .andExpect(status().isOk())
                .andExpect(content().json("[\"Swimming\"]"));
    }

    @Test
    void newestFirstOrderingHoldsForActivitiesCreatedThroughTheApi() throws Exception {
        createActivity("Oldest", 30, TODAY.minusDays(5));
        createActivity("Newest", 30, TODAY);
        createActivity("Middle", 30, TODAY.minusDays(2));

        mvc.perform(asOwner(get(base)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].activityType").value("Newest"))
                .andExpect(jsonPath("$[1].activityType").value("Middle"))
                .andExpect(jsonPath("$[2].activityType").value("Oldest"));
    }

    @Test
    void writeRoutesRejectAnInvalidOrUnknownUserId() throws Exception {
        mvc.perform(JwtTestSupport.asUser(post("/users/0/activities")
                                .contentType("application/json").content(body("Running", 30, YESTERDAY)),
                        jwtEncoder, jwtProperties, "0"))
                .andExpect(status().isBadRequest());
        mvc.perform(JwtTestSupport.asUser(post("/users/9223372036854775807/activities")
                                .contentType("application/json").content(body("Running", 30, YESTERDAY)),
                        jwtEncoder, jwtProperties, "9223372036854775807"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }
}
