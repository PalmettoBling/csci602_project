package edu.citadel.weather;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.citadel.api.request.WeatherRequest;
import edu.citadel.dal.model.ActivityWeather;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Component
public class HistoricalWeatherClient {
    private final RestClient client;
    private final ObjectMapper mapper;

    public HistoricalWeatherClient(@Value("${weather.archive-url:https://archive-api.open-meteo.com}") String baseUrl,
                                   ObjectMapper mapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(8000);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.mapper = mapper;
    }

    public ActivityWeather fetch(WeatherRequest request) {
        // Query UTC hours so repeated local hours at DST transitions remain distinct.
        Instant hour = request.occurredAt().toInstant().truncatedTo(ChronoUnit.HOURS);
        String date = hour.atOffset(ZoneOffset.UTC).toLocalDate().toString();
        String body;
        try {
            body = client.get().uri(builder -> builder.path("/v1/archive")
                    .queryParam("latitude", request.latitude())
                    .queryParam("longitude", request.longitude())
                    .queryParam("start_date", date).queryParam("end_date", date)
                    .queryParam("timezone", "GMT")
                    .queryParam("temperature_unit", "celsius")
                    .queryParam("wind_speed_unit", "kmh")
                    .queryParam("precipitation_unit", "mm")
                    .queryParam("hourly", "temperature_2m,precipitation,weather_code,wind_speed_10m")
                    .build()).retrieve().body(String.class);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Historical weather provider is unavailable. Please try again later.", exception);
        }
        JsonNode hourly;
        try {
            JsonNode root = mapper.readTree(body == null ? "" : body);
            if (root == null) {
                throw invalidResponse(null);
            }
            hourly = root.path("hourly");
        } catch (JsonProcessingException exception) {
            throw invalidResponse(exception);
        }
        if (!hourly.path("time").isArray()) {
            throw invalidResponse(null);
        }
        String target = hour.atOffset(ZoneOffset.UTC).toLocalDateTime().toString();
        for (int index = 0; index < hourly.path("time").size(); index++) {
            if (target.equals(hourly.path("time").path(index).asText())) {
                ActivityWeather weather = new ActivityWeather();
                weather.setLatitude(request.latitude());
                weather.setLongitude(request.longitude());
                weather.setLocalTime(request.occurredAt().toLocalDateTime());
                weather.setUtcOffset(request.occurredAt().getOffset().toString());
                weather.setSampledAt(hour);
                weather.setTemperatureC(number(hourly, "temperature_2m", index));
                weather.setPrecipitationMm(number(hourly, "precipitation", index));
                weather.setWindSpeedKmh(number(hourly, "wind_speed_10m", index));
                JsonNode code = hourly.path("weather_code").path(index);
                if (!code.isIntegralNumber() || !code.canConvertToInt()) {
                    throw unavailable();
                }
                weather.setWeatherCode(code.intValue());
                weather.setFetchedAt(Instant.now());
                return weather;
            }
        }
        throw unavailable();
    }

    private double number(JsonNode hourly, String name, int index) {
        JsonNode value = hourly.path(name).path(index);
        if (!value.isNumber() || !Double.isFinite(value.doubleValue())) {
            throw unavailable();
        }
        return value.doubleValue();
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Historical weather is not available for this location and hour yet. Recent dates can take several days.");
    }

    private ResponseStatusException invalidResponse(Exception cause) {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Historical weather provider returned an invalid response.", cause);
    }
}
