Feature: Application Health and Info
  As a developer
  I want to verify the application is running and reporting its status
  So that I can confirm the service is healthy before using it

  Scenario: Health endpoint returns OK status
    When I send a GET request to "/health"
    Then the response status code should be 200
    And the response body should contain "ok"

  Scenario: Info endpoint returns application details
    When I send a GET request to "/info"
    Then the response status code should be 200
    And the response body should contain "csci-602"
    And the response body should contain "1.0.0"
