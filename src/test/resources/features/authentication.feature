Feature: User Registration and Login
  As an API consumer
  I want to register a new account and log in
  So that I can obtain a JWT access token for authenticated requests

  Scenario: Successfully register a new account
    Given I have a registration request with username "authuser1", password "secret123", and email "authuser1@example.com"
    When I send a POST request to "/auth/register"
    Then the response status code should be 201
    And the response body should contain "authuser1"
    And the response body should contain a JSON field "token"

  Scenario: Registering with a username that already exists fails
    Given I have a registration request with username "authuser2", password "secret123", and email "authuser2@example.com"
    And I send a POST request to "/auth/register"
    And the response status code should be 201
    When I have a registration request with username "authuser2", password "different456", and email "authuser2b@example.com"
    And I send a POST request to "/auth/register"
    Then the response status code should be 409

  Scenario: Successfully log in with valid credentials
    Given I have a registration request with username "authuser3", password "secret123", and email "authuser3@example.com"
    And I send a POST request to "/auth/register"
    And the response status code should be 201
    When I have a login request with username "authuser3" and password "secret123"
    And I send a POST request to "/auth/login"
    Then the response status code should be 200
    And the response body should contain a JSON field "token"

  Scenario: Logging in with an incorrect password fails
    Given I have a registration request with username "authuser4", password "secret123", and email "authuser4@example.com"
    And I send a POST request to "/auth/register"
    And the response status code should be 201
    When I have a login request with username "authuser4" and password "wrongpassword"
    And I send a POST request to "/auth/login"
    Then the response status code should be 401

  Scenario: Logging in with a username that does not exist fails
    When I have a login request with username "nosuchuser" and password "whatever"
    And I send a POST request to "/auth/login"
    Then the response status code should be 401
