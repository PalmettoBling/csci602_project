package edu.citadel.bdd;

import edu.citadel.auth.JwtTestSupport;
import edu.citadel.config.JwtProperties;
import edu.citadel.dal.model.Activity;
import edu.citadel.dal.model.User;
import edu.citadel.dal.ActivityRepository;
import edu.citadel.dal.UserRepository;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import org.springframework.http.MediaType;
import java.util.HashMap;
import java.util.Map;

public class StepDefinitions {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtProperties jwtProperties;

    private Long userId;
    private MvcResult result;

    /** Body for the generic "I send a POST request to {string}" step. */
    private String requestBody;

    /** Credentials this scenario registered through /auth/register. */
    private String registeredUsername;
    private String registeredPassword;
    private String registeredEmail;

    /** Bearer token from the most recent successful login, if any. */
    private String bearerToken;

    /** The activity most recently created through the API, for the edit and delete steps. */
    private Long createdActivityId;
    private Long otherUserId;
    private Long otherUserActivityId;

    private final List<Activity> testActivities = new ArrayList<>();
    private final List<User> testUsers = new ArrayList<>();

    /**
     * Ids of users created through /auth/register, so cleanUp can delete those
     * exact rows. The database is shared between teammates, so nothing here is
     * ever deleted by username pattern or prefix.
     */
    private final List<Long> registeredUserIds = new ArrayList<>();
    /**
     * Ids of activities created through the API rather than through the
     * repository, so cleanUp can remove those exact rows. The database is shared
     * between teammates, so nothing here is ever deleted by pattern or prefix.
     */
    private final List<Long> createdActivityIds = new ArrayList<>();

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder request) {
        return JwtTestSupport.asUser(request, jwtEncoder, jwtProperties, String.valueOf(userId));
    }

    @Given("a user exists with recorded activities")
    public void userExistsWithActivities() {
        User user = userRepository.save(
                new User("Test User", UUID.randomUUID() + "@example.com")
        );

        userId = user.getId();
        testUsers.add(user);

        testActivities.add(activityRepository.save(
                new Activity(user, "Running", 20, LocalDate.of(2026, 10, 1))
        ));

        testActivities.add(activityRepository.save(
                new Activity(user, "Cycling", 30, LocalDate.of(2026, 10, 5))
        ));

        testActivities.add(activityRepository.save(
                new Activity(user, "Running", 40, LocalDate.of(2026, 10, 10))
        ));
    }

    @Given("a user does not exist")
    public void userDoesNotExist() {
        userId = Long.MAX_VALUE;
    }

    @Given("an invalid user ID")
    public void invalidUserId() {
        userId = 0L;
    }

    @When("the user requests their activity history")
    public void requestActivityHistory() throws Exception {
        result = mockMvc().perform(
                asUser(get("/users/" + userId + "/activities"))
        ).andReturn();
    }

    @When("the user searches for activities of type {string}")
    public void searchByType(String type) throws Exception {
        result = mockMvc().perform(
                asUser(get("/users/" + userId + "/activities/search")
                        .param("type", type)
                )
        ).andReturn();
    }

    @When("the user searches with a start date after the end date")
    public void invalidDateRange() throws Exception {
        result = mockMvc().perform(
                asUser(get("/users/" + userId + "/activities/search")
                        .param("from", "2026-10-10")
                        .param("to", "2026-10-01")
                )
        ).andReturn();
    }

    @When("the user requests their recorded exercise types")
    public void requestExerciseTypes() throws Exception {
        result = mockMvc().perform(
                asUser(get("/users/" + userId + "/activity-types"))
        ).andReturn();
    }

    @Then("the response status should be {int}")
    public void checkStatus(int expectedStatus) {
        assertEquals(expectedStatus, result.getResponse().getStatus());
    }

    @Then("the response should contain {int} activities")
    public void checkActivityCount(int expectedCount) throws Exception {
        JsonNode body = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        assertEquals(expectedCount, body.size());
    }

    @Then("the activities should be ordered newest first")
    public void checkActivityOrder() throws Exception {
        JsonNode body = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        for (int i = 1; i < body.size(); i++) {
            LocalDate previous = LocalDate.parse(
                    body.get(i - 1).get("activityDate").asText()
            );

            LocalDate current = LocalDate.parse(
                    body.get(i).get("activityDate").asText()
            );

            assertFalse(previous.isBefore(current));
        }
    }

    @Then("all returned activities should have type {string}")
    public void checkActivityType(String expectedType) throws Exception {
        JsonNode body = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        for (JsonNode activity : body) {
            assertEquals(
                    expectedType,
                    activity.get("activityType").asText()
            );
        }
    }

    @Then("the response should contain {string} and {string}")
    public void checkExerciseTypes(String first, String second) throws Exception {
        JsonNode body = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        List<String> types = new ArrayList<>();

        for (JsonNode item : body) {
            types.add(item.asText());
        }

        assertTrue(types.contains(first));
        assertTrue(types.contains(second));
    }

    // ------------------------------------------------------------------ auth

    /**
     * Builds a registration body for a brand new user.
     *
     * <p>The username and email are randomised rather than fixed, because the
     * database is shared: a hardcoded "testuser" would collide with a teammate's
     * row and turn a 201 into a 409 depending on who ran the suite last.
     */
    @Given("I have a registration request for a new user")
    public void createRegistrationRequest() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 12);

        registeredUsername = "bdd-" + unique;
        registeredPassword = "correct-horse-battery-" + unique;
        registeredEmail = "bdd-" + unique + "@example.test";

        Map<String, String> registration = new HashMap<>();
        registration.put("name", "BDD Test User");
        registration.put("username", registeredUsername);
        registration.put("password", registeredPassword);
        registration.put("email", registeredEmail);

        requestBody = objectMapper.writeValueAsString(registration);
    }

    /** Same as above but with the password omitted, to exercise the 400 path. */
    @Given("I have a registration request with no password")
    public void createRegistrationRequestWithoutPassword() throws Exception {
        createRegistrationRequest();

        Map<String, String> registration = new HashMap<>();
        registration.put("name", "BDD Test User");
        registration.put("username", registeredUsername);
        registration.put("email", registeredEmail);

        requestBody = objectMapper.writeValueAsString(registration);
    }

    @When("I send a POST request to {string}")
    public void sendPostRequest(String endpoint) throws Exception {
        result = mockMvc().perform(
                post(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        ).andReturn();

        rememberRegisteredUser();
    }

    @When("I send the same registration request again")
    public void sendSameRegistrationAgain() throws Exception {
        sendPostRequest("/auth/register");
    }

    @When("I log in with the registered credentials")
    public void loginWithRegisteredCredentials() throws Exception {
        login(registeredUsername, registeredPassword);
    }

    @When("I log in with the wrong password")
    public void loginWithWrongPassword() throws Exception {
        login(registeredUsername, "definitely-not-the-password");
    }

    @When("I log in as a user that does not exist")
    public void loginAsUnknownUser() throws Exception {
        login("no-such-user-" + UUID.randomUUID(), registeredPassword);
    }

    private void login(String username, String password) throws Exception {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", username);
        credentials.put("password", password);

        result = mockMvc().perform(
                post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(credentials))
        ).andReturn();

        if (result.getResponse().getStatus() == 200) {
            bearerToken = objectMapper
                    .readTree(result.getResponse().getContentAsString())
                    .get("token").asText();
        }
    }

    /**
     * Records the id of a user that /auth/register just created, so cleanUp can
     * remove that exact row.
     */
    private void rememberRegisteredUser() throws Exception {
        if (result.getResponse().getStatus() != 201) {
            return;
        }

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());

        if (body.has("id")) {
            registeredUserIds.add(body.get("id").asLong());
        }
    }

    @When("I send a GET request to {string}")
    public void sendGetRequest(String endpoint) throws Exception {
        result = mockMvc().perform(get(endpoint)).andReturn();
    }

    @When("I send an authenticated GET request to {string}")
    public void sendAuthenticatedGetRequest(String endpoint) throws Exception {
        assertNotNull(bearerToken, "no login has produced a token for this scenario");

        result = mockMvc().perform(
                get(endpoint).header("Authorization", "Bearer " + bearerToken)
        ).andReturn();
    }

    @Then("the response body should contain the registered username")
    public void bodyContainsRegisteredUsername() throws Exception {
        checkResponseBody(registeredUsername);
    }

    @Then("the response body should contain the registered email")
    public void bodyContainsRegisteredEmail() throws Exception {
        checkResponseBody(registeredEmail);
    }

    @Then("the response body should contain a bearer token")
    public void bodyContainsBearerToken() throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());

        assertTrue(body.hasNonNull("token"), "the login response must carry a token");
        assertEquals("Bearer", body.get("tokenType").asText());
        // Three dot-separated base64url segments: a real signed JWT, not a stub.
        assertEquals(3, body.get("token").asText().split("\\.").length,
                "the token should be a signed JWT");
    }

    /**
     * The BDD half of the no-secrets sweep.
     *
     * <p>Deliberately structural rather than a plain substring scan. A 400 from
     * /auth/register legitimately reads
     * {@code {"fields":{"password":"password is required"}}} -- that is a field
     * name inside a validation message, not a disclosed credential, and a raw
     * "body must not contain the word password" check would fail on it while
     * telling us nothing about safety. So this asserts the three things that
     * actually matter: no credential-bearing field anywhere outside the
     * validation map, no BCrypt hash in any form, and no echo of the password
     * this scenario actually submitted.
     */
    @Then("the response body should not expose any password or hash")
    public void bodyExposesNoSecret() throws Exception {
        String body = result.getResponse().getContentAsString();

        for (String prefix : new String[]{"$2a$", "$2b$", "$2y$"}) {
            assertFalse(body.contains(prefix),
                    "response body must not contain a BCrypt hash: " + body);
        }

        if (registeredPassword != null) {
            assertFalse(body.contains(registeredPassword),
                    "response body must not echo the submitted password: " + body);
        }

        if (body.isBlank()) {
            return;
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (Exception notJson) {
            // A non-JSON body (the plain-text /verification code) has no fields
            // to inspect; the hash and echo checks above already covered it.
            return;
        }

        assertNoCredentialFields(root, "$");
    }

    /**
     * Fails if any field named like a credential carries a value, at any depth.
     * The {@code fields} object of an ApiError is skipped: its keys are the names
     * of the request fields that failed validation, and its values are messages
     * written in this codebase.
     */
    private void assertNoCredentialFields(JsonNode node, String path) {
        if (node.isObject()) {
            node.fieldNames().forEachRemaining(name -> {
                if (path.equals("$") && name.equals("fields")) {
                    return;
                }

                String lower = name.toLowerCase();
                assertFalse(
                        lower.equals("password") || lower.equals("passwordhash")
                                || lower.equals("hash") || lower.equals("secret"),
                        "response must not carry a " + name + " field at " + path);

                assertNoCredentialFields(node.get(name), path + "." + name);
            });
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                assertNoCredentialFields(node.get(i), path + "[" + i + "]");
            }
        }
    }

    @Then("the response status code should be {int}")
    public void checkResponseStatusCode(int expectedStatus) {
        assertEquals(expectedStatus, result.getResponse().getStatus());
    }

    @Then("the response body should contain {string}")
    public void checkResponseBody(String expectedText) throws Exception {
        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains(expectedText),
                "Expected response to contain: " + expectedText);
    }

    // ------------------------------------------------- activity management

    @Given("another user exists with a recorded activity")
    public void anotherUserExistsWithAnActivity() {
        User otherUser = userRepository.save(
                new User("Other Test User", UUID.randomUUID() + "@example.com")
        );

        otherUserId = otherUser.getId();
        testUsers.add(otherUser);

        Activity otherActivity = activityRepository.save(
                new Activity(otherUser, "Fencing", 35, LocalDate.of(2026, 10, 7))
        );

        otherUserActivityId = otherActivity.getId();
        testActivities.add(otherActivity);
    }

    @When("the user adds an activity of type {string} lasting {int} minutes")
    public void addActivity(String type, int minutes) throws Exception {
        postActivity(activityBody(type, minutes, LocalDate.now().toString()));
    }

    @When("the user adds an activity with a blank exercise type")
    public void addActivityWithBlankType() throws Exception {
        postActivity(activityBody("   ", 25, LocalDate.now().toString()));
    }

    @When("the user adds an activity dated tomorrow")
    public void addActivityDatedTomorrow() throws Exception {
        postActivity(activityBody("Rowing", 25, LocalDate.now().plusDays(1).toString()));
    }

    @When("the user adds an activity with a malformed request body")
    public void addActivityWithMalformedBody() throws Exception {
        postActivity("{\"activityType\":\"Rowing\",");
    }

    @When("the user edits that activity to type {string} lasting {int} minutes")
    public void editCreatedActivity(String type, int minutes) throws Exception {
        assertNotNull(createdActivityId, "no activity has been created to edit");

        result = mockMvc().perform(
                asUser(put("/users/" + userId + "/activities/" + createdActivityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activityBody(type, minutes, LocalDate.now().toString())))
        ).andReturn();
    }

    @When("the user deletes that activity")
    public void deleteCreatedActivity() throws Exception {
        assertNotNull(createdActivityId, "no activity has been created to delete");

        result = mockMvc().perform(
                asUser(delete("/users/" + userId + "/activities/" + createdActivityId))
        ).andReturn();
    }

    @When("the user edits an activity that does not exist")
    public void editMissingActivity() throws Exception {
        result = mockMvc().perform(
                asUser(put("/users/" + userId + "/activities/" + Long.MAX_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activityBody("Kayaking", 55, LocalDate.now().toString())))
        ).andReturn();
    }

    @When("the user deletes an activity that does not exist")
    public void deleteMissingActivity() throws Exception {
        result = mockMvc().perform(
                asUser(delete("/users/" + userId + "/activities/" + Long.MAX_VALUE))
        ).andReturn();
    }

    /*
     * The other user's activity id is addressed through THIS user's path, so the
     * request passes the /users/{userId}/** owner check and the 404 comes from the
     * endpoint itself. Using the other user's path would be a 403 from the
     * security filter instead, which the JUnit tests cover.
     */
    @When("the user edits the other user's activity")
    public void editOtherUsersActivity() throws Exception {
        assertNotNull(otherUserActivityId, "the other user's activity was not created");

        result = mockMvc().perform(
                asUser(put("/users/" + userId + "/activities/" + otherUserActivityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activityBody("Kayaking", 55, LocalDate.now().toString())))
        ).andReturn();
    }

    @When("the user deletes the other user's activity")
    public void deleteOtherUsersActivity() throws Exception {
        assertNotNull(otherUserActivityId, "the other user's activity was not created");

        result = mockMvc().perform(
                asUser(delete("/users/" + userId + "/activities/" + otherUserActivityId))
        ).andReturn();
    }

    @Then("their activity history should contain {int} activities")
    public void historyContainsCount(int expectedCount) throws Exception {
        assertEquals(expectedCount, fetchHistory().size());
    }

    @Then("their activity history should include {string}")
    public void historyIncludesType(String type) throws Exception {
        assertTrue(historyTypes().contains(type),
                "Expected the history to include: " + type);
    }

    @Then("their activity history should not include {string}")
    public void historyExcludesType(String type) throws Exception {
        assertFalse(historyTypes().contains(type),
                "Expected the history not to include: " + type);
    }

    @Then("the other user's activity should be unchanged")
    public void otherUsersActivityUnchanged() {
        Activity stored = activityRepository.findById(otherUserActivityId).orElse(null);

        assertNotNull(stored, "the other user's activity should still exist");
        assertEquals("Fencing", stored.getActivityType());
        assertEquals(35, stored.getDurationMinutes());
        assertEquals(otherUserId, stored.getUser().getId());
    }

    private String activityBody(String type, int minutes, String date) throws Exception {
        Map<String, Object> activity = new HashMap<>();
        activity.put("activityType", type);
        activity.put("durationMinutes", minutes);
        activity.put("activityDate", date);

        return objectMapper.writeValueAsString(activity);
    }

    private void postActivity(String body) throws Exception {
        result = mockMvc().perform(
                asUser(post("/users/" + userId + "/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
        ).andReturn();

        if (result.getResponse().getStatus() == 201) {
            createdActivityId = objectMapper
                    .readTree(result.getResponse().getContentAsString())
                    .get("id").asLong();
            createdActivityIds.add(createdActivityId);
        }
    }

    /**
     * Re-reads the history through the API. Deliberately does not overwrite
     * {@code result}, so a scenario can assert on an earlier response's status or
     * body after checking what was actually stored.
     */
    private JsonNode fetchHistory() throws Exception {
        MvcResult history = mockMvc().perform(
                asUser(get("/users/" + userId + "/activities"))
        ).andReturn();

        assertEquals(200, history.getResponse().getStatus(),
                "the activity history should be readable");

        return objectMapper.readTree(history.getResponse().getContentAsString());
    }

    private List<String> historyTypes() throws Exception {
        List<String> types = new ArrayList<>();

        for (JsonNode activity : fetchHistory()) {
            types.add(activity.get("activityType").asText());
        }

        return types;
    }

    @After
    public void cleanUp() {
        // Only the exact ids this scenario created through the API.
        for (Long activityId : createdActivityIds) {
            activityRepository.findById(activityId).ifPresent(activityRepository::delete);
        }

        createdActivityIds.clear();
        createdActivityId = null;
        otherUserId = null;
        otherUserActivityId = null;

        activityRepository.deleteAll(testActivities);
        userRepository.deleteAll(testUsers);

        // Only the exact ids /auth/register handed back. Never by username
        // pattern: the database is shared with teammates.
        for (Long registeredUserId : registeredUserIds) {
            userRepository.findById(registeredUserId).ifPresent(userRepository::delete);
        }

        registeredUserIds.clear();
        registeredUsername = null;
        registeredPassword = null;
        registeredEmail = null;
        bearerToken = null;

        testActivities.clear();
        testUsers.clear();
    }
}