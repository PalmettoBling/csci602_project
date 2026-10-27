package edu.citadel.api.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;

public record WeatherRequest(Double latitude, Double longitude,
                             @JsonFormat(without = JsonFormat.Feature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                             OffsetDateTime occurredAt) {
}
