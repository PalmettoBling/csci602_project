Feature: Account Retrieval
  As an API consumer
  I want to retrieve user accounts by username or ID
  So that I can look up existing users

  Background:
    Given I have an account request with username "lookupuser", password "pass789", and email "lookup@example.com"
    And I send a POST request to "/account"
    And the response status code should be 201

  Scenario: Retrieve an account by username
    When I send a GET request to "/account/username/lookupuser"
    Then the response status code should be 200
    And the response body should contain "lookupuser"
    And the response body should contain "lookup@example.com"

  Scenario: Retrieve an account by ID
    When I send a GET request to the created account's ID endpoint
    Then the response status code should be 200
    And the response body should contain "lookupuser"

  Scenario: Look up a username that does not exist
    When I send a GET request to "/account/username/nonexistentuser"
    Then the response status code should be 404

  Scenario: Look up an ID that does not exist
    When I send a GET request to "/account/99999"
    Then the response status code should be 404
