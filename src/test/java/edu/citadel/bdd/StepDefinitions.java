package edu.citadel.bdd;

import edu.citadel.auth.JwtTestSupport;
import edu.citadel.config.JwtProperties;
import edu.citadel.dal.model.Activity;
import edu.citadel.dal.model.User;
import edu.citadel.dal.ActivityRepository;
import edu.citadel.dal.UserRepository;
import edu.citadel.dal.AccountRepository;
import edu.citadel.dal.model.Account;
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
    private AccountRepository accountRepository;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtProperties jwtProperties;

    private Long userId;
    private MvcResult result;
    private String accountRequestBody;
    private Long createdAccountId;

    /** The activity most recently created through the API, for the edit and delete steps. */
    private Long createdActivityId;
    private Long otherUserId;
    private Long otherUserActivityId;

    private final List<Activity> testActivities = new ArrayList<>();
    private final List<User> testUsers = new ArrayList<>();
    private final List<String> testAccountUsernames = new ArrayList<>();
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

    @Given("I have an account request with username {string}, password {string}, and email {string}")
    public void createAccountRequest(String username, String password, String email) throws Exception {
        Map<String, String> account = new HashMap<>();
        account.put("username", username);
        account.put("password", password);
        account.put("email", email);

        accountRequestBody = objectMapper.writeValueAsString(account);
        testAccountUsernames.add(username);
    }

    @When("I send a POST request to {string}")
    public void sendPostRequest(String endpoint) throws Exception {
        result = mockMvc().perform(
                post(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountRequestBody)
        ).andReturn();

        if (result.getResponse().getStatus() == 201) {
            JsonNode body = objectMapper.readTree(
                    result.getResponse().getContentAsString()
            );

            if (body.has("id")) {
                createdAccountId = body.get("id").asLong();
            }

            if (body.has("username")) {
                testAccountUsernames.add(body.get("username").asText());
            }
        }
    }

    @When("I send a GET request to {string}")
    public void sendGetRequest(String endpoint) throws Exception {
        result = mockMvc().perform(get(endpoint)).andReturn();
    }

    @When("I send a GET request to the created account's ID endpoint")
    public void getCreatedAccountById() throws Exception {
        Account account = accountRepository.findAccountByUsername("lookupuser");

        assertNotNull(account, "The test account was not found.");

        result = mockMvc().perform(
                get("/account/" + account.getUser_id())
        ).andReturn();
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

        for (String username : testAccountUsernames) {
            Account account = accountRepository.findAccountByUsername(username);

            if (account != null) {
                accountRepository.delete(account);
            }
        }

        testAccountUsernames.clear();

        testActivities.clear();
        testUsers.clear();
    }
}