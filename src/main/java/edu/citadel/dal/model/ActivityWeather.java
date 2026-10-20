package edu.citadel.dal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class ActivityWeather {
    private Double latitude;
    private Double longitude;
    @Column(name = "weather_local_time")
    private LocalDateTime localTime;
    @Column(name = "weather_utc_offset")
    private String utcOffset;
    private Instant sampledAt;
    @Column(name = "temperature_c")
    private Double temperatureC;
    private Double precipitationMm;
    private Double windSpeedKmh;
    private Integer weatherCode;
    private Instant fetchedAt;
}
