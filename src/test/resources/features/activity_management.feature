
Feature: Manage Activity Entries
  As a user, I want to add, edit and delete my own workouts
  so that my activity history stays accurate and private to me.

  Scenario: Add a new activity
    Given a user exists with recorded activities
    When the user adds an activity of type "Rowing" lasting 25 minutes
    Then the response status should be 201
    And their activity history should contain 4 activities
    And their activity history should include "Rowing"

  Scenario: Edit an existing activity
    Given a user exists with recorded activities
    When the user adds an activity of type "Rowing" lasting 25 minutes
    And the user edits that activity to type "Kayaking" lasting 55 minutes
    Then the response status should be 200
    And their activity history should include "Kayaking"
    And their activity history should not include "Rowing"
    And their activity history should contain 4 activities

  Scenario: Delete an existing activity
    Given a user exists with recorded activities
    When the user adds an activity of type "Rowing" lasting 25 minutes
    And the user deletes that activity
    Then the response status should be 204
    And their activity history should contain 3 activities
    And their activity history should not include "Rowing"

  Scenario: Add an activity with a duration of zero
    Given a user exists with recorded activities
    When the user adds an activity of type "Rowing" lasting 0 minutes
    Then the response status should be 400
    And the response body should contain "durationMinutes"
    And their activity history should contain 3 activities

  Scenario: Add an activity with a blank exercise type
    Given a user exists with recorded activities
    When the user adds an activity with a blank exercise type
    Then the response status should be 400
    And the response body should contain "activityType"
    And their activity history should contain 3 activities

  Scenario: Add an activity dated in the future
    Given a user exists with recorded activities
    When the user adds an activity dated tomorrow
    Then the response status should be 400
    And their activity history should contain 3 activities

  Scenario: Add an activity from a malformed request body
    Given a user exists with recorded activities
    When the user adds an activity with a malformed request body
    Then the response status should be 400
    And their activity history should contain 3 activities

  Scenario: Edit an activity that does not exist
    Given a user exists with recorded activities
    When the user edits an activity that does not exist
    Then the response status should be 404

  Scenario: Delete an activity that does not exist
    Given a user exists with recorded activities
    When the user deletes an activity that does not exist
    Then the response status should be 404

  Scenario: Edit another user's activity
    Given a user exists with recorded activities
    And another user exists with a recorded activity
    When the user edits the other user's activity
    Then the response status should be 404
    And the other user's activity should be unchanged

  Scenario: Delete another user's activity
    Given a user exists with recorded activities
    And another user exists with a recorded activity
    When the user deletes the other user's activity
    Then the response status should be 404
    And the other user's activity should be unchanged

  Scenario: Another user's activity never appears in this user's history
    Given a user exists with recorded activities
    And another user exists with a recorded activity
    When the user requests their activity history
    Then the response status should be 200
    And the response should contain 3 activities
    And their activity history should not include "Fencing"
