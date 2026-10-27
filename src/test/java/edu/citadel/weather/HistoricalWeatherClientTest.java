package edu.citadel.weather;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.citadel.api.request.WeatherRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class HistoricalWeatherClientTest {
    private final WeatherStub server = new WeatherStub();
    private final HistoricalWeatherClient client = new HistoricalWeatherClient(server.url(), new ObjectMapper());
    private final WeatherRequest request = new WeatherRequest(32.78, -79.93,
            OffsetDateTime.parse("2026-10-01T14:35:00-04:00"));

    @AfterEach
    void close() {
        server.close();
    }

    @Test
    void fetchesUtcHourAndMapsAllValuesWithExplicitUnits() {
        var weather = client.fetch(request);
        assertEquals(24.5, weather.getTemperatureC());
        assertEquals(0.3, weather.getPrecipitationMm());
        assertEquals(12.0, weather.getWindSpeedKmh());
        assertEquals(51, weather.getWeatherCode());
        assertEquals(request.occurredAt().toLocalDateTime(), weather.getLocalTime());
        assertEquals("-04:00", weather.getUtcOffset());
        assertEquals(Instant.parse("2026-10-01T18:00:00Z"), weather.getSampledAt());
        assertEquals(32.78, weather.getLatitude());
        assertEquals(-79.93, weather.getLongitude());
        assertNotNull(weather.getFetchedAt());
        assertTrue(server.query.contains("timezone=GMT"));
        assertTrue(server.query.contains("start_date=2026-10-01"));
        assertTrue(server.query.contains("temperature_unit=celsius"));
        assertTrue(server.query.contains("wind_speed_unit=kmh"));
    }

    @Test
    void usesUtcDateWhenLocalOffsetCrossesMidnight() {
        server.body = server.body.replace("2026-10-01T18:00", "2026-10-01T23:00");
        var weather = client.fetch(new WeatherRequest(32.0, 10.0,
                OffsetDateTime.parse("2026-10-02T01:30:00+02:00")));
        assertEquals(Instant.parse("2026-10-01T23:00:00Z"), weather.getSampledAt());
        assertTrue(server.query.contains("end_date=2026-10-01"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "garbage", "{}", "{\"hourly\":{\"time\":42}}"})
    void rejectsMalformedProviderResponses(String body) {
        server.body = body;
        assertEquals(502, assertThrows(ResponseStatusException.class, () -> client.fetch(request))
                .getStatusCode().value());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"hourly\":{\"time\":[]}}",
            "{\"hourly\":{\"time\":[\"2026-10-01T18:00\"],\"temperature_2m\":[null]}}",
            "{\"hourly\":{\"time\":[\"2026-10-01T18:00\"],\"temperature_2m\":[24],\"precipitation\":[null]}}",
            "{\"hourly\":{\"time\":[\"2026-10-01T18:00\"],\"temperature_2m\":[24],\"precipitation\":[0],\"wind_speed_10m\":[null]}}",
            "{\"hourly\":{\"time\":[\"2026-10-01T18:00\"],\"temperature_2m\":[24],\"precipitation\":[0],\"wind_speed_10m\":[12],\"weather_code\":[1.5]}}"
    })
    void missingMeasurementsAreNotFabricatedAsZero(String body) {
        server.body = body;
        assertEquals(422, assertThrows(ResponseStatusException.class, () -> client.fetch(request))
                .getStatusCode().value());
    }

    @Test
    void providerErrorAndConnectionFailureBecome502() {
        server.status = 429;
        assertEquals(502, assertThrows(ResponseStatusException.class, () -> client.fetch(request))
                .getStatusCode().value());
        server.close();
        assertEquals(502, assertThrows(ResponseStatusException.class, () -> client.fetch(request))
                .getStatusCode().value());
    }
}
