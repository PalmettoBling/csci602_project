package edu.citadel.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.citadel.dal.UserRepository;
import edu.citadel.dal.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import edu.citadel.main.RestApiApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests for registration, login and token enforcement.
 *
 * <p>Drives a real HTTP port with the JDK's own client rather than going through
 * MockMvc, so the security filter chain, the JSON error bodies and the status
 * codes asserted here are the ones a curl command would actually see. The client
 * is also configured never to follow redirects, which turns a bounce to a
 * generated HTML login form into a visible test failure instead of a 200.
 *
 * <p>There is deliberately no {@code @ActiveProfiles}: the suite inherits
 * whichever profile the build passes, so the same tests run against the local
 * database and against the ghci Postgres service container in CI.
 *
 * <p>Both of those databases are persistent and shared, so every test invents a
 * random username and deletes the rows it created in {@link #cleanUp()}.
 */
@SpringBootTest(
        classes = RestApiApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:jwt-test.properties")
public class AuthEndpointsTest {

    private static final String PASSWORD = "correct-horse-battery";

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** Ids of users created by a test, removed afterwards. */
    private final List<Long> createdUserIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        // activities.user_id is ON DELETE CASCADE, so removing the user suffices.
        createdUserIds.forEach(id -> userRepository.deleteById(id));
        createdUserIds.clear();
    }

    // ---------------------------------------------------------------- register

    @Test
    @DisplayName("register: valid input returns 201 and never the password hash")
    void registerSucceeds() throws Exception {
        String username = uniqueUsername();

        Response response = postJson("/auth/register", registrationBody(username));

        assertEquals(201, response.status(), response.body());

        JsonNode body = objectMapper.readTree(response.body());
        // Recorded before any other assertion: if one below fails, the row still
        // has to be cleaned out of the shared database.
        remember(body.get("id").asLong());

        assertEquals(username, body.get("username").asText());
        assertEquals(username + "@example.test", body.get("email").asText());
        assertEquals("Integration Test User", body.get("name").asText());
        assertTrue(body.get("id").asLong() > 0, "a generated id should come back");

        assertFalse(body.has("passwordHash"), "response must not expose passwordHash");
        assertFalse(body.has("password"), "response must not expose password");
        assertFalse(response.body().contains("$2a$"),
                "response must not contain a BCrypt hash: " + response.body());
        assertFalse(response.body().contains(PASSWORD),
                "response must not echo the plaintext password");
    }

    @Test
    @DisplayName("register: the password is stored BCrypt-hashed, not in plaintext")
    void registerHashesThePassword() {
        long id = registerAndGetId(uniqueUsername());

        String storedHash = userRepository.findById(id).orElseThrow().getPasswordHash();

        assertNotNull(storedHash, "a hash should have been stored");
        assertTrue(storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$"),
                "stored value should be a BCrypt hash but was: " + storedHash);
        assertFalse(storedHash.contains(PASSWORD), "the plaintext must not be stored");
    }

    @Test
    @DisplayName("register: a duplicate username returns 409")
    void registerRejectsDuplicateUsername() throws Exception {
        String username = uniqueUsername();
        registerAndGetId(username);

        // Same username, fresh email, so only the username can be the clash.
        String secondAttempt = """
                {"name":"Someone Else","username":"%s","email":"%s@example.test","password":"%s"}
                """.formatted(username, uniqueUsername(), PASSWORD);

        Response response = postJson("/auth/register", secondAttempt);

        assertEquals(409, response.status(), response.body());
        assertEquals(409, objectMapper.readTree(response.body()).get("status").asInt());
    }

    @Test
    @DisplayName("register: a duplicate username in a different case still returns 409")
    void registerRejectsDuplicateUsernameIgnoringCase() {
        String username = uniqueUsername();
        registerAndGetId(username);

        String upperCased = """
                {"name":"Someone Else","username":"%s","email":"%s@example.test","password":"%s"}
                """.formatted(username.toUpperCase(), uniqueUsername(), PASSWORD);

        Response response = postJson("/auth/register", upperCased);

        assertEquals(409, response.status(),
                "usernames are compared case-insensitively, so this is a duplicate");
    }

    @Test
    @DisplayName("register: a duplicate email returns 409")
    void registerRejectsDuplicateEmail() {
        String first = uniqueUsername();
        registerAndGetId(first);

        String sameEmail = """
                {"name":"Someone Else","username":"%s","email":"%s@example.test","password":"%s"}
                """.formatted(uniqueUsername(), first, PASSWORD);

        assertEquals(409, postJson("/auth/register", sameEmail).status());
    }

    @Test
    @DisplayName("register: a missing required field returns 400 naming that field")
    void registerRejectsMissingFields() throws Exception {
        String noName = """
                {"username":"%s","email":"%s@example.test","password":"%s"}
                """.formatted(uniqueUsername(), uniqueUsername(), PASSWORD);
        String noUsername = """
                {"name":"No Username","email":"%s@example.test","password":"%s"}
                """.formatted(uniqueUsername(), PASSWORD);
        String noPassword = """
                {"name":"No Password","username":"%s","email":"%s@example.test"}
                """.formatted(uniqueUsername(), uniqueUsername());

        assertBadRequestNaming(noName, "name");
        assertBadRequestNaming(noUsername, "username");
        assertBadRequestNaming(noPassword, "password");
    }

    @Test
    @DisplayName("register: a blank password and a malformed email return 400")
    void registerRejectsInvalidValues() throws Exception {
        String blankPassword = """
                {"name":"Blank","username":"%s","email":"%s@example.test","password":"   "}
                """.formatted(uniqueUsername(), uniqueUsername());
        String badEmail = """
                {"name":"Bad Email","username":"%s","email":"not-an-email","password":"%s"}
                """.formatted(uniqueUsername(), PASSWORD);

        assertBadRequestNaming(blankPassword, "password");
        assertBadRequestNaming(badEmail, "email");
    }

    @Test
    @DisplayName("register: an empty body returns 400, not 500")
    void registerRejectsEmptyBody() {
        assertEquals(400, postJson("/auth/register", "").status());
    }

    // ------------------------------------------------------------------- login

    @Test
    @DisplayName("login: valid credentials return 200 and a 1 hour HS256 token whose subject is the user id")
    void loginSucceeds() throws Exception {
        String username = uniqueUsername();
        long id = registerAndGetId(username);

        Response response = postJson("/auth/login", loginBody(username, PASSWORD));

        assertEquals(200, response.status(), response.body());

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("Bearer", body.get("tokenType").asText());
        assertEquals(id, body.get("userId").asLong());
        assertEquals(username, body.get("username").asText());

        String token = body.get("token").asText();
        assertEquals(3, token.split("\\.").length, "a JWS has three dot-separated parts");
        assertEquals("HS256", decodeSegment(token, 0).get("alg").asText());

        JsonNode claims = decodeSegment(token, 1);
        assertEquals(String.valueOf(id), claims.get("sub").asText(),
                "the subject must be the user id, since that is what authorization compares");
        assertEquals("csci-602", claims.get("iss").asText());
        assertEquals(3600, claims.get("exp").asLong() - claims.get("iat").asLong(),
                "the token should last one hour");
    }

    @Test
    @DisplayName("login: a wrong password returns 401 with a JSON body and no token")
    void loginRejectsWrongPassword() throws Exception {
        String username = uniqueUsername();
        registerAndGetId(username);

        Response response = postJson("/auth/login", loginBody(username, "the-wrong-password"));

        assertEquals(401, response.status(), response.body());
        assertJsonBody(response);

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(401, body.get("status").asInt());
        assertFalse(body.has("token"), "a failed login must not return a token");
    }

    @Test
    @DisplayName("login: an unknown username returns the same 401 as a wrong password")
    void loginRejectsUnknownUser() throws Exception {
        Response wrongPassword;
        String username = uniqueUsername();
        registerAndGetId(username);
        wrongPassword = postJson("/auth/login", loginBody(username, "the-wrong-password"));

        Response unknownUser = postJson("/auth/login", loginBody(uniqueUsername(), PASSWORD));

        assertEquals(401, unknownUser.status());
        assertJsonBody(unknownUser);
        // Identical wording, so the endpoint cannot be used to enumerate accounts.
        assertEquals(
                objectMapper.readTree(wrongPassword.body()).get("message").asText(),
                objectMapper.readTree(unknownUser.body()).get("message").asText(),
                "an unknown user and a wrong password must be indistinguishable");
    }

    @Test
    @DisplayName("login: a user row with no password hash cannot log in")
    void loginRejectsUserWithoutHash() {
        // Mirrors a row created before V4, or created through POST /users.
        String username = uniqueUsername();
        User credentialless = new User();
        credentialless.setName("No Credentials");
        credentialless.setUsername(username);
        credentialless.setEmail(username + "@example.test");
        remember(userRepository.saveAndFlush(credentialless).getId());

        Response response = postJson("/auth/login", loginBody(username, PASSWORD));

        assertEquals(401, response.status(), "a null hash must never be treated as a match");
    }

    // ------------------------------------------------------- token enforcement

    @Test
    @DisplayName("protected endpoint: no token returns 401 as JSON, not an HTML login page")
    void protectedEndpointRejectsMissingToken() throws Exception {
        Response response = get("/activities", null);

        assertEquals(401, response.status(), response.body());
        assertJsonBody(response);
        assertEquals(401, objectMapper.readTree(response.body()).get("status").asInt());

        String body = response.body().toLowerCase();
        assertFalse(body.contains("<html"), "the body must not be HTML: " + response.body());
        assertFalse(body.contains("<form"), "there must be no login form: " + response.body());
    }

    @Test
    @DisplayName("protected endpoint: a garbage token returns 401 as JSON")
    void protectedEndpointRejectsMalformedToken() {
        Response response = get("/activities", "not.a.real.token");

        assertEquals(401, response.status(), response.body());
        assertJsonBody(response);
    }

    @Test
    @DisplayName("protected endpoint: a token signed with the wrong key returns 401")
    void protectedEndpointRejectsForgedSignature() {
        String username = uniqueUsername();
        registerAndGetId(username);
        String token = login(username);

        // Keep the header and claims, corrupt only the signature.
        String[] parts = token.split("\\.");
        String forged = parts[0] + "." + parts[1] + "." + parts[2].substring(1) + "A";

        assertEquals(401, get("/activities", forged).status(),
                "a token whose signature does not verify must be rejected");
    }

    @Test
    @DisplayName("protected endpoint: a valid token returns 200")
    void protectedEndpointAcceptsValidToken() {
        String username = uniqueUsername();
        registerAndGetId(username);
        String token = login(username);

        Response response = get("/activities", token);

        assertEquals(200, response.status(), response.body());
    }

    @Test
    @DisplayName("GET /users with a valid token never discloses a password hash")
    void userListingHidesHashes() {
        String username = uniqueUsername();
        registerAndGetId(username);
        String token = login(username);

        Response response = get("/users", token);

        assertEquals(200, response.status(), response.body());
        assertFalse(response.body().contains("passwordHash"),
                "GET /users must not expose the hash field");
        assertFalse(response.body().contains("$2a$"), "GET /users must not expose a BCrypt hash");
        assertFalse(response.body().contains("$2b$"), "GET /users must not expose a BCrypt hash");
    }

    @Test
    @DisplayName("user-scoped route: user A with a valid token gets 403 for user B")
    void userScopedRouteRejectsOtherUser() throws Exception {
        String userA = uniqueUsername();
        long idA = registerAndGetId(userA);
        String tokenA = login(userA);

        long idB = registerAndGetId(uniqueUsername());

        Response response = get("/users/" + idB + "/activities", tokenA);

        assertEquals(403, response.status(),
                "a token for user " + idA + " must not reach user " + idB
                        + " but got: " + response.body());
        assertJsonBody(response);
        assertEquals(403, objectMapper.readTree(response.body()).get("status").asInt());
    }

    @Test
    @DisplayName("user-scoped route: the owner reaches the real handler and gets 200")
    void userScopedRouteAllowsOwnIdThroughToHandler() {
        String username = uniqueUsername();
        long id = registerAndGetId(username);
        String token = login(username);

        Response response = get("/users/" + id + "/activities", token);

        // UserActivityEndpoints now serves this path, so own-id access clears
        // authorization and is answered by the handler itself. A freshly
        // registered user has no activities, hence the empty array rather than
        // any error. SecurityConfig needed no change for this to start working:
        // the /users/{userId}/** rule already matched the route.
        assertEquals(200, response.status(),
                "own-id access should clear authorization and be served by "
                        + "UserActivityEndpoints, but got: " + response.body());
        assertEquals("[]", response.body().trim(),
                "a newly registered user has no activity history yet");
    }

    @Test
    @DisplayName("user-scoped route: no token returns 401 rather than 403")
    void userScopedRouteRequiresAToken() {
        assertEquals(401, get("/users/1/activities", null).status());
    }

    // -------------------------------------------------------- public endpoints

    @Test
    @DisplayName("the public allowlist still answers without a token")
    void publicEndpointsStayPublic() {
        assertEquals(200, get("/health", null).status(),
                "/health is asserted public by health.feature");
        assertEquals(200, get("/info", null).status(),
                "/info is asserted public by health.feature");
        assertEquals(200, get("/verification", null).status(),
                "/verification is called by the React home page before login");
        assertEquals(404, get("/account/username/definitely-not-a-real-user", null).status(),
                "/account/** is asserted public by account_retrieval.feature, so an unknown "
                        + "username must answer 404 rather than 401");
    }

    // ----------------------------------------------------------------- helpers

    /** Minimal view of an HTTP response: everything these tests assert on. */
    private record Response(int status, String contentType, String body) {
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    /** Random enough to survive reruns against a shared, persistent database. */
    private static String uniqueUsername() {
        return "itest_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private void remember(long userId) {
        createdUserIds.add(userId);
    }

    private static String registrationBody(String username) {
        return """
                {"name":"Integration Test User","username":"%s","email":"%s@example.test","password":"%s"}
                """.formatted(username, username, PASSWORD);
    }

    private static String loginBody(String username, String password) {
        return """
                {"username":"%s","password":"%s"}
                """.formatted(username, password);
    }

    private Response postJson(String path, String json) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url(path)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        return send(request);
    }

    /** GET with an optional bearer token; pass null to send no Authorization header. */
    private Response get(String path, String token) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url(path))).GET();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return send(builder.build());
    }

    private Response send(HttpRequest request) {
        try {
            HttpResponse<String> response =
                    http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Response(
                    response.statusCode(),
                    response.headers().firstValue("content-type").orElse(null),
                    response.body());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("request to " + request.uri() + " failed", e);
        }
    }

    /** Registers a user, records it for cleanup, and returns its id. */
    private long registerAndGetId(String username) {
        Response response = postJson("/auth/register", registrationBody(username));
        assertEquals(201, response.status(), "setup registration failed: " + response.body());
        try {
            long id = objectMapper.readTree(response.body()).get("id").asLong();
            remember(id);
            return id;
        } catch (Exception e) {
            throw new IllegalStateException("could not read the registration response", e);
        }
    }

    private String login(String username) {
        Response response = postJson("/auth/login", loginBody(username, PASSWORD));
        assertEquals(200, response.status(), "setup login failed: " + response.body());
        try {
            return objectMapper.readTree(response.body()).get("token").asText();
        } catch (Exception e) {
            throw new IllegalStateException("could not read the login response", e);
        }
    }

    private void assertBadRequestNaming(String json, String expectedField) throws Exception {
        Response response = postJson("/auth/register", json);

        assertEquals(400, response.status(),
                "expected 400 for a request violating " + expectedField + ": " + response.body());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(400, body.get("status").asInt());
        assertTrue(body.get("fields").has(expectedField),
                "the 400 body should name the offending field " + expectedField
                        + " but was: " + response.body());
    }

    private void assertJsonBody(Response response) {
        assertNotNull(response.contentType(), "an error response must declare a content type");
        assertTrue(response.contentType().toLowerCase().startsWith("application/json"),
                "expected a JSON error body but got " + response.contentType());
    }

    /** Decodes a JWS segment: 0 is the header, 1 is the claims. */
    private JsonNode decodeSegment(String token, int index) throws Exception {
        byte[] decoded = Base64.getUrlDecoder().decode(token.split("\\.")[index]);
        return objectMapper.readTree(new String(decoded, StandardCharsets.UTF_8));
    }
}
