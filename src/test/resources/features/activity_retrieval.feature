Feature: Activity Retrieval by User
  As an API consumer
  I want to retrieve all activities logged by a specific user
  So that I can review an individual's activity history

  Background:
    Given I am authenticated as a registered user with username "activityuser1", password "secret123", and email "activityuser1@example.com"
    And I have a user request with name "Jamie Fox" and email "jamie.fox@example.com"
    And I send an authenticated POST request to "/users"
    And the response status code should be 201
    And I have an activity request for the created user with type "Running", duration 30, and date "2024-01-15"
    And I send an authenticated POST request to "/activities"
    And the response status code should be 201

  Scenario: Retrieve all activities for a user
    When I send an authenticated GET request to the created user's activities endpoint
    Then the response status code should be 200
    And the response body should contain "Running"

  Scenario: Retrieving activities for a non-existent user fails
    When I send an authenticated GET request to "/activities/user/999999"
    Then the response status code should be 404

  Scenario: Accessing the activities endpoint without a token is rejected
    When I send a GET request to "/activities"
    Then the response status code should be 401

  Scenario: A different account cannot retrieve another user's activities
    Given I am authenticated as a different registered user
    When I send an authenticated GET request to "/activities"
    Then the response status code should be 200
    And the response body should not contain "Running"
    When I send an authenticated GET request to the created user's activities endpoint
    Then the response status code should be 404
