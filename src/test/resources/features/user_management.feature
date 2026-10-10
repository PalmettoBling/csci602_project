Feature: User Management
  As an API consumer
  I want to retrieve, update, and delete user records
  So that user information can be managed through the API

  Background:
    Given I am authenticated as a registered user with username "manageuser1", password "secret123", and email "manageuser1@example.com"
    And I have a user request with name "Taylor Reed" and email "taylor.reed@example.com"
    And I send an authenticated POST request to "/users"
    And the response status code should be 201

  Scenario: Retrieve a user by ID
    When I send an authenticated GET request to the created user's ID endpoint
    Then the response status code should be 200
    And the response body should contain "Taylor Reed"

  Scenario: Update an existing user
    Given I have a user request with name "Taylor R. Reed" and email "taylor.r.reed@example.com"
    When I send an authenticated PUT request to the created user's ID endpoint
    Then the response status code should be 200
    And the response body should contain "Taylor R. Reed"

  Scenario: Delete an existing user
    When I send an authenticated DELETE request to the created user's ID endpoint
    Then the response status code should be 204
    When I send an authenticated GET request to the created user's ID endpoint
    Then the response status code should be 404

  Scenario: Retrieve a user that does not exist
    When I send an authenticated GET request to "/users/999999"
    Then the response status code should be 404

  Scenario: Accessing the users endpoint without a token is rejected
    When I send a GET request to "/users"
    Then the response status code should be 401

  Scenario: A different account cannot access or change this user's record
    Given I am authenticated as a different registered user
    When I send an authenticated GET request to "/users"
    Then the response status code should be 200
    And the response body should not contain "Taylor Reed"
    When I send an authenticated GET request to the created user's ID endpoint
    Then the response status code should be 404
    Given I have a user request with name "Unauthorized Change" and email "unauthorized@example.com"
    When I send an authenticated PUT request to the created user's ID endpoint
    Then the response status code should be 404
    When I send an authenticated DELETE request to the created user's ID endpoint
    Then the response status code should be 404
