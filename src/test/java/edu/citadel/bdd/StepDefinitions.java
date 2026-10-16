package edu.citadel.bdd;

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
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    private Long userId;
    private MvcResult result;
    private String accountRequestBody;
    private Long createdAccountId;

    private final List<Activity> testActivities = new ArrayList<>();
    private final List<User> testUsers = new ArrayList<>();
    private final List<String> testAccountUsernames = new ArrayList<>();

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(context).build();
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
                get("/users/" + userId + "/activities")
        ).andReturn();
    }

    @When("the user searches for activities of type {string}")
    public void searchByType(String type) throws Exception {
        result = mockMvc().perform(
                get("/users/" + userId + "/activities/search")
                        .param("type", type)
        ).andReturn();
    }

    @When("the user searches with a start date after the end date")
    public void invalidDateRange() throws Exception {
        result = mockMvc().perform(
                get("/users/" + userId + "/activities/search")
                        .param("from", "2026-10-10")
                        .param("to", "2026-10-01")
        ).andReturn();
    }

    @When("the user requests their recorded exercise types")
    public void requestExerciseTypes() throws Exception {
        result = mockMvc().perform(
                get("/users/" + userId + "/activity-types")
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

    @After
    public void cleanUp() {
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