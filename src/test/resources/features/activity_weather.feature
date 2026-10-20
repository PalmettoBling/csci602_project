@weather
Feature: Historical weather for completed activities
  Weather belongs to the activity's local date, time and coordinates, not the current weather.

  Scenario: Persist and reuse historical weather
    Given a completed activity without weather
    When the owner attaches weather for its time and coordinates
    Then the historical weather is persisted and displayed in activity history
    When the owner requests the same weather again
    Then the saved weather is reused without another provider request

  Scenario: Reject another user's token
    Given a completed activity without weather
    When another user tries to attach weather on the owner's route
    Then the weather response status is 403 without a provider request

  Scenario: Reject anonymous access
    Given a completed activity without weather
    When an anonymous visitor tries to attach weather
    Then the weather response status is 401 without a provider request

  Scenario: Reject an activity not owned by the route's user
    Given a completed activity without weather
    When another user tries to attach the owner's activity on their own route
    Then the weather response status is 404 without a provider request

  Scenario: Invalidate weather when the activity date changes
    Given a completed activity without weather
    When the owner attaches weather for its time and coordinates
    And the owner changes the activity date
    Then the old weather association is removed
