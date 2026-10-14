
Feature: Filter Activity History
  As a user, I want to filter my activity history by exercise type
  so that I can review specific types of workouts.

  Scenario: Filter workouts by exercise type
    Given a user exists with recorded activities
    When the user searches for activities of type "Running"
    Then the response status should be 200
    And the response should contain 2 activities
    And all returned activities should have type "Running"

  Scenario: Filter workouts using an invalid date range
    Given a user exists with recorded activities
    When the user searches with a start date after the end date
    Then the response status should be 400
