
Feature: View Activity History
  As a user, I want to view my activity history
  so that I can review my previous workouts.

  Scenario: User views their previous workouts
    Given a user exists with recorded activities
    When the user requests their activity history
    Then the response status should be 200
    And the response should contain 3 activities
    And the activities should be ordered newest first

  Scenario: User does not exist
    Given a user does not exist
    When the user requests their activity history
    Then the response status should be 404
