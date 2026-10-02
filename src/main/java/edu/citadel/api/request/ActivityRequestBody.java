package edu.citadel.api.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ActivityRequestBody {
    private Long userId;
    private String activityType;
    private Integer durationMinutes;
    private LocalDate activityDate;
}