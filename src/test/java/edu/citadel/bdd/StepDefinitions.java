package edu.citadel.bdd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Step definitions backing the Cucumber feature files in
 * {@code src/test/resources/features}. Requests are issued against the
 * running Spring Boot application via {@link TestRestTemplate}, covering
 * account management, JWT authentication, and the protected user/activity
 * endpoints.
 */
public class StepDefinitions {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, Object> requestBody;
    private ResponseEntity<String> lastResponse;
    private String authToken;
    private Long createdAccountId;
    private Long createdUserId;

    @Before
    public void resetState() {
        requestBody = null;
        lastResponse = null;
        authToken = null;
        createdAccountId = null;
        createdUserId = null;
        // Ensure each scenario starts from a clean slate so Background steps
        // (which insert fixed test usernames) never collide with data left
        // behind by a previous scenario in the same test run.
        jdbcTemplate.execute("TRUNCATE TABLE activities, users, accounts RESTART IDENTITY CASCADE");
    }

    // ---------------------------------------------------------------
    // Request body builders
    // ---------------------------------------------------------------

    @Given("I have an account request with username {string}, password {string}, and email {string}")
    public void i_have_an_account_request(String username, String password, String email) {
        requestBody = new HashMap<>();
        requestBody.put("username", username);
        requestBody.put("password", password);
        requestBody.put("email", email);
    }

    @Given("I have a registration request with username {string}, password {string}, and email {string}")
    public void i_have_a_registration_request(String username, String password, String email) {
        i_have_an_account_request(username, password, email);
    }

    @Given("I have a login request with username {string} and password {string}")
    public void i_have_a_login_request(String username, String password) {
        requestBody = new HashMap<>();
        requestBody.put("username", username);
        requestBody.put("password", password);
    }

    @Given("I have a user request with name {string} and email {string}")
    public void i_have_a_user_request(String name, String email) {
        requestBody = new HashMap<>();
        requestBody.put("name", name);
        requestBody.put("email", email);
    }

    @Given("I have an activity request for the created user with type {string}, duration {int}, and date {string}")
    public void i_have_an_activity_request(String activityType, int duration, String date) {
        assertNotNull(createdUserId, "A user must be created before an activity request can reference it");
        requestBody = new HashMap<>();
        requestBody.put("userId", createdUserId);
        requestBody.put("activityType", activityType);
        requestBody.put("durationMinutes", duration);
        requestBody.put("activityDate", date);
    }

    // ---------------------------------------------------------------
    // Authentication helper
    // ---------------------------------------------------------------

    @Given("I am authenticated as a registered user with username {string}, password {string}, and email {string}")
    public void i_am_authenticated_as_a_registered_user(String username, String password, String email) {
        Map<String, Object> registration = new HashMap<>();
        registration.put("username", username);
        registration.put("password", password);
        registration.put("email", email);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(registration, jsonHeaders());
        ResponseEntity<String> response =
                restTemplate.exchange("/auth/register", HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value(),
                "Expected registration to succeed: " + response.getBody());

        authToken = readJsonField(response.getBody(), "token");
        assertNotNull(authToken, "Registration response should contain a token");
    }

    // ---------------------------------------------------------------
    // Request execution
    // ---------------------------------------------------------------

    @When("I send a POST request to {string}")
    public void i_send_a_post_request_to(String path) {
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, jsonHeaders());
        lastResponse = restTemplate.exchange(path, HttpMethod.POST, entity, String.class);
        captureCreatedIds(path);
    }

    @When("I send an authenticated POST request to {string}")
    public void i_send_an_authenticated_post_request_to(String path) {
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, authHeaders());
        lastResponse = restTemplate.exchange(path, HttpMethod.POST, entity, String.class);
        captureCreatedIds(path);
    }

    @When("I send a GET request to {string}")
    public void i_send_a_get_request_to(String path) {
        HttpEntity<Void> entity = new HttpEntity<>(jsonHeaders());
        lastResponse = restTemplate.exchange(path, HttpMethod.GET, entity, String.class);
    }

    @When("I send an authenticated GET request to {string}")
    public void i_send_an_authenticated_get_request_to(String path) {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        lastResponse = restTemplate.exchange(path, HttpMethod.GET, entity, String.class);
    }

    @When("I send a GET request to the created account's ID endpoint")
    public void i_send_a_get_request_to_the_created_accounts_id_endpoint() {
        assertNotNull(createdAccountId, "No account has been created yet");
        i_send_a_get_request_to("/account/" + createdAccountId);
    }

    @When("I send an authenticated GET request to the created user's ID endpoint")
    public void i_send_an_authenticated_get_request_to_the_created_users_id_endpoint() {
        assertNotNull(createdUserId, "No user has been created yet");
        i_send_an_authenticated_get_request_to("/users/" + createdUserId);
    }

    @When("I send an authenticated PUT request to the created user's ID endpoint")
    public void i_send_an_authenticated_put_request_to_the_created_users_id_endpoint() {
        assertNotNull(createdUserId, "No user has been created yet");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, authHeaders());
        lastResponse = restTemplate.exchange("/users/" + createdUserId, HttpMethod.PUT, entity, String.class);
    }

    @When("I send an authenticated DELETE request to the created user's ID endpoint")
    public void i_send_an_authenticated_delete_request_to_the_created_users_id_endpoint() {
        assertNotNull(createdUserId, "No user has been created yet");
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        lastResponse = restTemplate.exchange("/users/" + createdUserId, HttpMethod.DELETE, entity, String.class);
    }

    @When("I send an authenticated GET request to the created user's activities endpoint")
    public void i_send_an_authenticated_get_request_to_the_created_users_activities_endpoint() {
        assertNotNull(createdUserId, "No user has been created yet");
        i_send_an_authenticated_get_request_to("/activities/user/" + createdUserId);
    }

    // ---------------------------------------------------------------
    // Assertions
    // ---------------------------------------------------------------

    @Then("the response status code should be {int}")
    public void the_response_status_code_should_be(int expectedStatus) {
        assertNotNull(lastResponse, "No response has been received yet");
        assertEquals(expectedStatus, lastResponse.getStatusCode().value(),
                "Unexpected status code. Response body: " + lastResponse.getBody());
    }

    @Then("the response body should contain {string}")
    public void the_response_body_should_contain(String expectedContent) {
        assertNotNull(lastResponse, "No response has been received yet");
        String body = lastResponse.getBody();
        assertNotNull(body, "Response body was null");
        assertTrue(body.contains(expectedContent),
                "Expected response body to contain '" + expectedContent + "' but was: " + body);
    }

    @Then("the response body should contain a JSON field {string}")
    public void the_response_body_should_contain_a_json_field(String fieldName) {
        assertNotNull(lastResponse, "No response has been received yet");
        String value = readJsonField(lastResponse.getBody(), fieldName);
        assertNotNull(value, "Expected response body to contain field '" + fieldName + "' but was: "
                + lastResponse.getBody());
        assertTrue(!value.isBlank(), "Expected field '" + fieldName + "' to be non-blank");
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = jsonHeaders();
        if (authToken != null) {
            headers.setBearerAuth(authToken);
        }
        return headers;
    }

    private void captureCreatedIds(String path) {
        if (lastResponse.getStatusCode() != HttpStatus.CREATED) {
            return;
        }

        if (path.equals("/account")) {
            createdAccountId = readJsonLongField(lastResponse.getBody(), "user_id");
        } else if (path.equals("/users")) {
            createdUserId = readJsonLongField(lastResponse.getBody(), "id");
        }
    }

    private String readJsonField(String json, String fieldName) {
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode value = node.get(fieldName);
            return value == null || value.isNull() ? null : value.asText();
        } catch (Exception e) {
            fail("Failed to parse JSON response: " + json + " (" + e.getMessage() + ")");
            return null;
        }
    }

    private Long readJsonLongField(String json, String fieldName) {
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode value = node.get(fieldName);
            return value == null || value.isNull() ? null : value.asLong();
        } catch (Exception e) {
            fail("Failed to parse JSON response: " + json + " (" + e.getMessage() + ")");
            return null;
        }
    }
}
