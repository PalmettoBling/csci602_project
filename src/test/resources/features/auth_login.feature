Feature: Account Login
  As a registered user
  I want to exchange my credentials for a token
  So that I can reach my own user-scoped routes

  Background:
    Given I have a registration request for a new user
    And I send a POST request to "/auth/register"
    And the response status code should be 201

  Scenario: Log in with the registered credentials
    When I log in with the registered credentials
    Then the response status code should be 200
    And the response body should contain a bearer token
    And the response body should not expose any password or hash

  Scenario: Log in with the wrong password
    When I log in with the wrong password
    Then the response status code should be 401
    And the response body should not expose any password or hash

  Scenario: Log in as a user that was never registered
    When I log in as a user that does not exist
    Then the response status code should be 401
    And the response body should not expose any password or hash
