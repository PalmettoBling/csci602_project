package edu.citadel.api;

import edu.citadel.dal.*;
import edu.citadel.dal.model.*;
import edu.citadel.auth.JwtTestSupport;
import edu.citadel.config.JwtProperties;
import edu.citadel.main.RestApiApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Uses real JPA queries and the configured PostgreSQL database; all fixture writes roll back. */
@SpringBootTest(classes = RestApiApplication.class)
@Transactional
class UserActivityEndpointsTest {
    @Autowired ActivityRepository activities;
    @Autowired UserRepository users;
    @Autowired VerificationRepository verification;
    @Autowired AccountRepository accounts;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired JwtProperties jwtProperties;
    @Autowired FilterChainProxy springSecurityFilterChain;
    @Autowired WebApplicationContext context;
    private MockMvc mvc;
    private User user;
    private String base;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();
        user = users.save(new User("History test", UUID.randomUUID() + "@example.com"));
        User other = users.save(new User("Other user", UUID.randomUUID() + "@example.com"));
        activities.save(new Activity(user, "Running", 20, LocalDate.of(2026, 10, 1)));
        activities.save(new Activity(user, "Cycling", 30, LocalDate.of(2026, 10, 5)));
        activities.save(new Activity(user, "Running", 40, LocalDate.of(2026, 10, 10)));
        activities.save(new Activity(other, "Swimming", 50, LocalDate.of(2026, 10, 10)));
        activities.flush();
        base = "/users/" + user.getId();
    }

    private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder request, String userId) {
        return JwtTestSupport.asUser(request, jwtEncoder, jwtProperties, userId);
    }

    @Test
    void historyIsUserScopedAndNewestFirst() throws Exception {
        mvc.perform(asUser(get(base + "/activities"), user.getId().toString())).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].activityDate").value("2026-10-10"))
                .andExpect(jsonPath("$[1].activityDate").value("2026-10-05"))
                .andExpect(jsonPath("$[2].activityDate").value("2026-10-01"));
    }

    @Test
    void combinedFiltersAreInclusiveAndCaseInsensitive() throws Exception {
        mvc.perform(asUser(get(base + "/activities/search").param("type", " running ")
                        .param("from", "2026-10-01").param("to", "2026-10-10"),
                        user.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].activityDate").value("2026-10-10"))
                .andExpect(jsonPath("$[1].activityDate").value("2026-10-01"));
    }

    @Test
    void filtersWorkIndependentlyAndWithoutParameters() throws Exception {
        mvc.perform(asUser(get(base + "/activities/search"), user.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
        mvc.perform(asUser(get(base + "/activities/search").param("type", "Cycling"), user.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(asUser(get(base + "/activities/search").param("from", "2026-10-05"), user.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(asUser(get(base + "/activities/search").param("to", "2026-10-05"), user.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(asUser(get(base + "/activities/search").param("type", "Swimming"), user.getId().toString()))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void typesAreDistinctSortedAndUserScoped() throws Exception {
        mvc.perform(asUser(get(base + "/activity-types"), user.getId().toString())).andExpect(status().isOk())
                .andExpect(content().json("[\"Cycling\",\"Running\"]"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/activities", "/activities/search", "/activity-types"})
    void existingUserWithNoActivitiesReturnsEmptyList(String route) throws Exception {
        User empty = users.save(new User("Empty", UUID.randomUUID() + "@example.com"));
        mvc.perform(asUser(get("/users/" + empty.getId() + route), empty.getId().toString()))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/activities", "/activities/search", "/activity-types"})
    void nonexistentUserReturns404(String route) throws Exception {
        mvc.perform(asUser(get("/users/9223372036854775807" + route), "9223372036854775807"))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/activities", "/activities/search", "/activity-types"})
    void invalidUserIdsReturn400(String route) throws Exception {
        mvc.perform(asUser(get("/users/0" + route), "0")).andExpect(status().isBadRequest());
        mvc.perform(asUser(get("/users/not-a-number" + route), "not-a-number")).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "2026-02-30", "2026-13-01", "10/01/2026", "2026-1-1", "garbage"})
    void invalidDatesReturn400(String date) throws Exception {
        mvc.perform(asUser(get(base + "/activities/search").param("from", date), user.getId().toString()))
                .andExpect(status().isBadRequest());
        mvc.perform(asUser(get(base + "/activities/search").param("to", date), user.getId().toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reversedDatesAndInvalidTypesReturn400() throws Exception {
        mvc.perform(asUser(get(base + "/activities/search").param("from", "2026-10-10")
                        .param("to", "2026-10-01"), user.getId().toString()))
                .andExpect(status().isBadRequest());
        for (String type : new String[]{"", " ", "x".repeat(101)}) {
            mvc.perform(asUser(get(base + "/activities/search").param("type", type), user.getId().toString()))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void existingCrudAndVerificationStillWork() throws Exception {
        String body = "{\"userId\":" + user.getId() + ",\"activityType\":\"Walking\",\"durationMinutes\":15,\"activityDate\":\"2026-10-03\"}";
        mvc.perform(asUser(post("/activities").contentType("application/json").content(body), user.getId().toString()))
                .andExpect(status().isCreated());
        Activity created = activities.findByUserIdOrderByActivityDateDescIdDesc(user.getId()).stream()
                .filter(a -> a.getActivityType().equals("Walking")).findFirst().orElseThrow();
        mvc.perform(asUser(get("/activities/" + created.getId()), user.getId().toString())).andExpect(status().isOk());
        mvc.perform(asUser(get("/activities"), user.getId().toString())).andExpect(status().isOk());
        mvc.perform(asUser(put("/activities/" + created.getId()).contentType("application/json")
                        .content(body.replace("Walking", "Hiking")), user.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activityType").value("Hiking"));
        mvc.perform(asUser(delete("/activities/" + created.getId()), user.getId().toString()))
                .andExpect(status().isNoContent());
        mvc.perform(asUser(get("/activities/" + created.getId()), user.getId().toString()))
                .andExpect(status().isNotFound());
        mvc.perform(asUser(post("/users").contentType("application/json")
                        .content("{\"name\":\"Created user\",\"email\":\"" + UUID.randomUUID() + "@example.com\"}"),
                        user.getId().toString())).andExpect(status().isCreated());
        mvc.perform(asUser(get("/users"), user.getId().toString())).andExpect(status().isOk());
        mvc.perform(get("/info")).andExpect(status().isOk());
        mvc.perform(get("/verification")).andExpect(status().isOk())
                .andExpect(content().string(verification.findAll().stream().findFirst().map(Verification::getCode).orElse("")));
        mvc.perform(get("/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
        String unique = UUID.randomUUID().toString();
        mvc.perform(post("/account").contentType("application/json").content("{\"username\":\"" + unique + "\",\"password\":\"testpass\",\"email\":\"" + unique + "@example.com\"}"))
                .andExpect(status().isCreated());
        Account account = accounts.findAccountByUsername(unique);
        mvc.perform(get("/account/" + account.getUser_id())).andExpect(status().isOk());
        mvc.perform(get("/account/username/" + unique)).andExpect(status().isOk());
    }
}
