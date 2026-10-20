package edu.citadel.bdd;

import edu.citadel.auth.JwtTestSupport;
import edu.citadel.config.JwtProperties;
import edu.citadel.dal.ActivityRepository;
import edu.citadel.dal.UserRepository;
import edu.citadel.dal.model.Activity;
import edu.citadel.dal.model.User;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class WeatherStepDefinitions {
    @Autowired ActivityRepository activities;
    @Autowired UserRepository users;
    @Autowired JwtEncoder encoder;
    @Autowired JwtProperties properties;
    @Autowired FilterChainProxy security;
    @Autowired WebApplicationContext context;
    private User user;
    private Activity activity;
    private MockMvc mvc;
    private MvcResult result;
    private int initialCalls;
    private static final String BODY = """
            {"latitude":32.78,"longitude":-79.93,"occurredAt":"2026-10-01T14:35:00-04:00"}
            """;

    @Given("a completed activity without weather")
    public void completedActivity() {
        user = users.save(new User("Weather scenario", UUID.randomUUID() + "@example.com"));
        activity = activities.save(new Activity(user, "Running", 30, LocalDate.of(2026, 10, 1)));
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
        initialCalls = CucumberSpringConfig.WEATHER.calls.get();
    }

    private String path() {
        return "/users/" + user.getId() + "/activities/" + activity.getId() + "/weather";
    }

    @When("the owner attaches weather for its time and coordinates")
    public void attach() throws Exception {
        result = mvc.perform(JwtTestSupport.asUser(post(path()).contentType("application/json").content(BODY),
                encoder, properties, user.getId().toString())).andReturn();
    }

    @Then("the historical weather is persisted and displayed in activity history")
    public void persistedWeather() throws Exception {
        assertEquals(200, result.getResponse().getStatus());
        // A fresh repository transaction must reconstruct the embedded snapshot.
        var stored = activities.findById(activity.getId()).orElseThrow().getWeather();
        assertNotNull(stored);
        assertEquals(24.5, stored.getTemperatureC());
        assertEquals("-04:00", stored.getUtcOffset());
        assertEquals(14, stored.getLocalTime().getHour());
        mvc.perform(JwtTestSupport.asUser(get("/users/" + user.getId() + "/activities"),
                        encoder, properties, user.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].weather.temperatureC").value(24.5))
                .andExpect(jsonPath("$[0].weather.weatherCode").value(51));
    }

    @When("the owner requests the same weather again")
    public void attachAgain() throws Exception {
        attach();
        assertEquals(200, result.getResponse().getStatus());
    }

    @Then("the saved weather is reused without another provider request")
    public void cached() {
        assertEquals(initialCalls + 1, CucumberSpringConfig.WEATHER.calls.get());
    }

    @When("another user tries to attach weather on the owner's route")
    public void otherUser() throws Exception {
        result = mvc.perform(JwtTestSupport.asUser(post(path()).contentType("application/json").content(BODY),
                encoder, properties, Long.toString(user.getId() + 100000))).andReturn();
    }

    @When("an anonymous visitor tries to attach weather")
    public void anonymous() throws Exception {
        result = mvc.perform(post(path()).contentType("application/json").content(BODY)).andReturn();
    }

    @When("another user tries to attach the owner's activity on their own route")
    public void foreignActivity() throws Exception {
        String otherId = Long.toString(user.getId() + 100000);
        result = mvc.perform(JwtTestSupport.asUser(
                post("/users/" + otherId + "/activities/" + activity.getId() + "/weather")
                        .contentType("application/json").content(BODY), encoder, properties, otherId)).andReturn();
    }

    @Then("the weather response status is {int} without a provider request")
    public void denied(int status) {
        assertEquals(status, result.getResponse().getStatus());
        assertEquals(initialCalls, CucumberSpringConfig.WEATHER.calls.get());
        assertNull(activities.findById(activity.getId()).orElseThrow().getWeather());
    }

    @When("the owner changes the activity date")
    public void changeDate() throws Exception {
        mvc.perform(JwtTestSupport.asUser(put("/activities/" + activity.getId())
                .contentType("application/json").content("""
                        {"activityType":"Running","durationMinutes":30,"activityDate":"2026-10-02"}
                        """), encoder, properties, user.getId().toString())).andExpect(status().isOk());
    }

    @Then("the old weather association is removed")
    public void invalidated() {
        assertNull(activities.findById(activity.getId()).orElseThrow().getWeather());
    }

    @After("@weather")
    public void cleanup() {
        if (activity != null) activities.deleteById(activity.getId());
        if (user != null) users.deleteById(user.getId());
    }

}
