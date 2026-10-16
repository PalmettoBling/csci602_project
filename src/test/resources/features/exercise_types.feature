
Feature: Select Exercise Type
  As a user, I want to select an exercise type
  so that my workouts are categorized by activity.

  Scenario: View previously recorded exercise types
    Given a user exists with recorded activities
    When the user requests their recorded exercise types
    Then the response status should be 200
    And the response should contain "Cycling" and "Running"

  Scenario: Request exercise types using an invalid user ID
    Given an invalid user ID
    When the user requests their recorded exercise types
    Then the response status should be 400
