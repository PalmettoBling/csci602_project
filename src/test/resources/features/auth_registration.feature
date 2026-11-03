Feature: Account Registration
  As an API consumer
  I want to register accounts through /auth/register
  So that passwords are hashed on the way in and never handed back out

  Scenario: Successfully register a new account
    Given I have a registration request for a new user
    When I send a POST request to "/auth/register"
    Then the response status code should be 201
    And the response body should contain the registered username
    And the response body should contain the registered email
    And the response body should not expose any password or hash

  Scenario: Registering the same username twice is rejected
    Given I have a registration request for a new user
    And I send a POST request to "/auth/register"
    And the response status code should be 201
    When I send the same registration request again
    Then the response status code should be 409
    And the response body should not expose any password or hash

  Scenario: Registering without a password is rejected
    Given I have a registration request with no password
    When I send a POST request to "/auth/register"
    Then the response status code should be 400
    And the response body should not expose any password or hash
