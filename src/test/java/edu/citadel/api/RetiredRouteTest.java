package edu.citadel.api;

import edu.citadel.auth.JwtTestSupport;
import edu.citadel.config.JwtProperties;
import edu.citadel.dal.UserRepository;
import edu.citadel.dal.model.User;
import edu.citadel.main.RestApiApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The 401 matrix for every route removed by the security hardening pass.
 *
 * <p>Three routes families are covered:
 * <ul>
 *   <li>{@code /account/**} -- returned a plaintext password to any anonymous
 *       caller, and was on the public allowlist.</li>
 *   <li>{@code /activities/**} -- any valid token could read, edit or delete any
 *       user's rows, and POST took the owner from the request body.</li>
 *   <li>{@code /users} -- any valid token listed every registered user's name
 *       and email; POST created arbitrary rows.</li>
 * </ul>
 *
 * <p>Two statuses are asserted, and the difference between them is the point.
 * Anonymous gets <strong>401</strong>: authorization is decided by
 * AuthorizationFilter before the dispatcher runs, so a path with no handler
 * still demands a token first. Authenticated gets <strong>404</strong>: the
 * token is accepted, the request reaches the dispatcher, and there is simply
 * nothing mapped there any more. A 200 from either column would mean a route
 * came back.
 *
 * <p>{@code @Transactional} so the one fixture user rolls back. Nothing here
 * writes through a removed route -- they no longer exist -- so there is nothing
 * else to clean up.
 */
@SpringBootTest(classes = RestApiApplication.class)
@TestPropertySource(locations = "classpath:jwt-test.properties")
@Transactional
class RetiredRouteTest {

    @Autowired UserRepository users;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired JwtProperties jwtProperties;
    @Autowired FilterChainProxy springSecurityFilterChain;
    @Autowired WebApplicationContext context;

    private MockMvc mvc;
    private User user;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();
        user = users.save(new User("Retired route test", UUID.randomUUID() + "@example.com"));
    }

    private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder request) {
        return JwtTestSupport.asUser(request, jwtEncoder, jwtProperties, user.getId().toString());
    }

    // --------------------------------------------------------------- anonymous

    @ParameterizedTest
    @ValueSource(strings = {
            "/account/1",
            "/account/username/anyone",
            "/activities",
            "/activities/1",
            "/users"
    })
    @DisplayName("anonymous GET on a retired route returns 401, never data")
    void anonymousGetIsUnauthorized(String path) throws Exception {
        mvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("anonymous POST /account no longer creates an account")
    void anonymousAccountCreationIsUnauthorized() throws Exception {
        mvc.perform(post("/account").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"anyone","password":"hunter2","email":"anyone@example.test"}
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("anonymous POST /users no longer creates a user")
    void anonymousUserCreationIsUnauthorized() throws Exception {
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Anyone","email":"anyone@example.test"}
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("anonymous POST /activities no longer creates an activity for anyone")
    void anonymousActivityCreationIsUnauthorized() throws Exception {
        mvc.perform(post("/activities").contentType(MediaType.APPLICATION_JSON).content("""
                        {"userId":1,"activityType":"Running","durationMinutes":20,"activityDate":"2026-10-01"}
                        """))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @CsvSource({"PUT,/activities/1", "DELETE,/activities/1"})
    @DisplayName("anonymous writes to a retired activity route return 401")
    void anonymousActivityWritesAreUnauthorized(String method, String path) throws Exception {
        MockHttpServletRequestBuilder request = method.equals("PUT")
                ? put(path).contentType(MediaType.APPLICATION_JSON).content("""
                        {"activityType":"Hiking","durationMinutes":30,"activityDate":"2026-10-01"}
                        """)
                : delete(path);

        mvc.perform(request).andExpect(status().isUnauthorized());
    }

    // ----------------------------------------------------------- authenticated

    @ParameterizedTest
    @ValueSource(strings = {
            "/account/1",
            "/account/username/anyone",
            "/activities",
            "/activities/1",
            "/users"
    })
    @DisplayName("a valid token finds no handler at a retired route: 404, not 200")
    void authenticatedGetIsNotFound(String path) throws Exception {
        mvc.perform(asUser(get(path))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a valid token cannot create an activity for another user via POST /activities")
    void authenticatedActivityCreationIsNotFound() throws Exception {
        // The body names a different owner -- exactly the escalation the removed
        // route allowed. There is no handler to honour it.
        mvc.perform(asUser(post("/activities").contentType(MediaType.APPLICATION_JSON).content("""
                        {"userId":999999,"activityType":"Running","durationMinutes":20,"activityDate":"2026-10-01"}
                        """)))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @CsvSource({"PUT,/activities/1", "DELETE,/activities/1"})
    @DisplayName("a valid token cannot edit or delete via a retired activity route")
    void authenticatedActivityWritesAreNotFound(String method, String path) throws Exception {
        MockHttpServletRequestBuilder request = method.equals("PUT")
                ? put(path).contentType(MediaType.APPLICATION_JSON).content("""
                        {"activityType":"Hiking","durationMinutes":30,"activityDate":"2026-10-01"}
                        """)
                : delete(path);

        mvc.perform(asUser(request)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a valid token cannot create a user via POST /users")
    void authenticatedUserCreationIsNotFound() throws Exception {
        mvc.perform(asUser(post("/users").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Squatter","email":"squatter@example.test"}
                        """)))
                .andExpect(status().isNotFound());
    }
}
