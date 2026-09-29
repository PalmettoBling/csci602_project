Feature: Account Creation
  As an API consumer
  I want to create new user accounts
  So that users can be registered in the system

  Scenario: Successfully create a new account
    Given I have an account request with username "testuser", password "secret123", and email "test@example.com"
    When I send a POST request to "/account"
    Then the response status code should be 201
    And the response body should contain "testuser"
    And the response body should contain "test@example.com"

  Scenario: Create a second account with different credentials
    Given I have an account request with username "janedoe", password "pass456", and email "jane@example.com"
    When I send a POST request to "/account"
    Then the response status code should be 201
    And the response body should contain "janedoe"
