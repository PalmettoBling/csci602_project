package edu.citadel.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Sweeps every surviving route and asserts no response body carries a credential.
 *
 * <p>Drives the real routes rather than inspecting the entity classes, because
 * the historical bug was not a missing annotation -- it was a controller
 * returning the entity directly. Only an actual response body proves anything.
 *
 * <p>Collects the bodies first and asserts at the end, so a failure names every
 * offending route at once instead of stopping at the first.
 *
 * <p>Not {@code @Transactional}; {@link #cleanUp()} removes the exact ids
 * created, by id. No network calls: the weather route is not exercised here
 * (it is covered, without a provider request, by {@link CrossUserAccessTest}).
 */
@SpringBootTest(classes = RestApiApplication.class)
@TestPropertySource(locations = "classpath:jwt-test.properties")
class NoSecretDisclosureTest {

    /** Field names that must never carry a value in a response body. */
    private static final List<String> CREDENTIAL_FIELD_NAMES =
            List.of("password", "passwordhash", "hash", "secret", "credentials");

    /** Every BCrypt variant prefix; none may appear anywhere in a response. */
    private static final List<String> HASH_PREFIXES = List.of("$2a$", "$2b$", "$2y$");

    @Autowired UserRepository users;
    @Autowired ActivityRepository activities;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired JwtProperties jwtProperties;
    @Autowired FilterChainProxy springSecurityFilterChain;
    @Autowired WebApplicationContext context;

    private MockMvc mvc;
    private User user;
    private String password;
    private String username;

    private final List<Long> createdActivityIds = new ArrayList<>();
    private final List<Long> createdUserIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();

        String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        username = "sweep_" + unique;
        password = "correct-horse-battery-" + unique;

        user = users.save(new User("Sweep user", UUID.randomUUID() + "@example.com"));
        createdUserIds.add(user.getId());

        Activity activity = activities.save(
                new Activity(user, "Running", 20, LocalDate.of(2026, 10, 1)));
        createdActivityIds.add(activity.getId());
    }

    @AfterEach
    void cleanUp() {
        for (Long activityId : createdActivityIds) {
            activities.findById(activityId).ifPresent(activities::delete);
        }
        for (Long userId : createdUserIds) {
            users.findById(userId).ifPresent(users::delete);
        }
        createdActivityIds.clear();
        createdUserIds.clear();
    }

    private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder request) {
        return JwtTestSupport.asUser(request, jwtEncoder, jwtProperties, user.getId().toString());
    }

    @Test
    @DisplayName("no response body from any surviving route contains a password or hash")
    void noRouteDisclosesACredential() throws Exception {
        Map<String, String> bodies = new LinkedHashMap<>();
        String base = "/users/" + user.getId();

        // Public routes.
        bodies.put("GET /health", perform(get("/health")));
        bodies.put("GET /info", perform(get("/info")));
        bodies.put("GET /verification", perform(get("/verification")));

        // Registration and login, which are the only routes that ever receive a
        // password. Registered through the API so the real hashing path runs.
        String registration = """
                {"name":"Sweep User","username":"%s","email":"%s@example.test","password":"%s"}
                """.formatted(username, username, password);

        String registerBody = perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(registration));
        bodies.put("POST /auth/register", registerBody);
        rememberRegisteredUser(registerBody);

        bodies.put("POST /auth/login", perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"%s","password":"%s"}
                        """.formatted(username, password))));

        bodies.put("POST /auth/login (wrong password)", perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"%s","password":"the-wrong-password"}
                        """.formatted(username))));

        bodies.put("POST /auth/register (duplicate)", perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(registration)));

        // Owner-scoped reads.
        bodies.put("GET history", perform(asUser(get(base + "/activities"))));
        bodies.put("GET search", perform(asUser(get(base + "/activities/search"))));
        bodies.put("GET activity-types", perform(asUser(get(base + "/activity-types"))));

        // Owner-scoped writes.
        String created = perform(asUser(post(base + "/activities")
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"activityType":"Hiking","durationMinutes":30,"activityDate":"2026-10-01"}
                        """)));
        bodies.put("POST activity", created);

        long createdId = objectMapper.readTree(created).get("id").asLong();
        createdActivityIds.add(createdId);

        bodies.put("PUT activity", perform(asUser(put(base + "/activities/" + createdId)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"activityType":"Kayaking","durationMinutes":45,"activityDate":"2026-10-02"}
                        """))));
        bodies.put("DELETE activity", perform(asUser(delete(base + "/activities/" + createdId))));

        // Error bodies, which is where an internal message would surface.
        bodies.put("401 anonymous", perform(get(base + "/activities")));
        bodies.put("403 other user", perform(asUser(get("/users/" + Long.MAX_VALUE + "/activities"))));
        bodies.put("404 retired route", perform(asUser(get("/activities"))));
        bodies.put("404 missing activity",
                perform(asUser(delete(base + "/activities/" + Long.MAX_VALUE))));

        List<String> failures = new ArrayList<>();
        bodies.forEach((route, body) -> collectDisclosures(route, body, failures));

        assertTrue(failures.isEmpty(), "credential disclosure:\n" + String.join("\n", failures));
    }

    /**
     * Records every way {@code body} could be disclosing a credential.
     *
     * <p>Structural rather than a plain substring scan, because two responses
     * contain the word "password" for entirely legitimate reasons: a validation
     * 400 names the field that failed ({@code "fields":{"password":...}}), and a
     * failed login answers {@code "Invalid username or password."} -- wording
     * that is deliberately identical for an unknown user and a wrong password so
     * the endpoint cannot be used to enumerate accounts. Both are prose and field
     * names written in this codebase, not data from the database.
     *
     * <p>So the three things actually checked are: no field <em>named</em> like a
     * credential carries a value anywhere in the tree, no BCrypt hash appears in
     * any form, and the password this test actually submitted is never echoed
     * back. A controller returning an entity -- the bug that made
     * {@code GET /account/{id}} hand out plaintext passwords -- fails the first
     * check immediately.
     */
    private void collectDisclosures(String route, String body, List<String> failures) {
        for (String prefix : HASH_PREFIXES) {
            if (body.contains(prefix)) {
                failures.add(route + " contains a BCrypt hash: " + body);
            }
        }
        if (body.contains(password)) {
            failures.add(route + " echoes the submitted password: " + body);
        }
        if (body.isBlank()) {
            return;
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (Exception notJson) {
            // The plain-text /verification code has no fields to inspect; the
            // hash and echo checks above already covered it.
            return;
        }
        collectCredentialFields(route, root, "$", failures);
    }

    /** Walks the tree flagging any field named like a credential that carries a value. */
    private void collectCredentialFields(String route, JsonNode node, String path,
                                         List<String> failures) {
        if (node.isObject()) {
            node.fieldNames().forEachRemaining(name -> {
                // The ApiError "fields" map is keyed by the names of the request
                // fields that failed validation; its values are our own messages.
                if (path.equals("$") && name.equals("fields")) {
                    return;
                }
                if (CREDENTIAL_FIELD_NAMES.contains(name.toLowerCase())) {
                    failures.add(route + " carries a \"" + name + "\" field at " + path);
                }
                collectCredentialFields(route, node.get(name), path + "." + name, failures);
            });
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                collectCredentialFields(route, node.get(i), path + "[" + i + "]", failures);
            }
        }
    }

    /**
     * The one body where the word "password" legitimately appears: a validation
     * 400 names the field that failed. Asserted separately and structurally --
     * what must not leak is the submitted value or a hash, not the field name.
     */
    @Test
    @DisplayName("a validation 400 names the password field without disclosing a value")
    void validationErrorNamesFieldWithoutDisclosingValue() throws Exception {
        String body = perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"No Password","username":"%s","email":"%s@example.test","password":""}
                        """.formatted(username, username)));

        JsonNode error = objectMapper.readTree(body);

        assertEquals(400, error.get("status").asInt(), body);
        assertTrue(error.get("fields").has("password"),
                "the 400 should name the offending field: " + body);

        // The field name is fine; the value and any hash are not.
        assertFalse(body.contains(password), "the 400 must not echo a password value: " + body);
        for (String prefix : new String[]{"$2a$", "$2b$", "$2y$"}) {
            assertFalse(body.contains(prefix), "the 400 must not contain a hash: " + body);
        }
        assertFalse(error.has("trace"), "the error body must never carry a stack trace: " + body);
        assertFalse(error.has("exception"), "the error body must never name an exception: " + body);
    }

    private void rememberRegisteredUser(String registerBody) throws Exception {
        JsonNode body = objectMapper.readTree(registerBody);
        if (body.has("id")) {
            createdUserIds.add(body.get("id").asLong());
        }
    }

    private String perform(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request).andReturn().getResponse().getContentAsString();
    }
}
