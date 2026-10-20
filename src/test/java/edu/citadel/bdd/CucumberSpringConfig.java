package edu.citadel.bdd;

import edu.citadel.main.RestApiApplication;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import edu.citadel.weather.WeatherStub;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@CucumberContextConfiguration
@SpringBootTest(
        classes = RestApiApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("ghci")
@Import(CucumberSpringConfig.WeatherTestConfig.class)
public class CucumberSpringConfig {
    static final WeatherStub WEATHER = new WeatherStub();

    @DynamicPropertySource
    static void weatherProperties(DynamicPropertyRegistry registry) {
        registry.add("weather.archive-url", WEATHER::url);
    }

    @TestConfiguration
    static class WeatherTestConfig {
        @Bean(destroyMethod = "close")
        WeatherStub weatherStub() {
            return WEATHER;
        }
    }
}
