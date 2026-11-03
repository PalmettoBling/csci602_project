Feature: Retired insecure routes
  The /account routes returned a plaintext password to any anonymous caller,
  and the unscoped /activities and /users routes let any logged-in caller read
  and modify another user's rows. All of them have been removed: /auth/register,
  /auth/login and /users/{userId}/activities replace them.

  Scenario Outline: An anonymous caller gets 401 from a retired route
    When I send a GET request to "<path>"
    Then the response status code should be 401
    And the response body should not expose any password or hash

    Examples:
      | path                        |
      | /account/1                  |
      | /account/username/lookupuser|
      | /activities                 |
      | /activities/1               |
      | /users                      |

  Scenario: An anonymous caller cannot create an account on the retired route
    Given I have a registration request for a new user
    When I send a POST request to "/account"
    Then the response status code should be 401

  Scenario Outline: An authenticated caller finds no handler at a retired route
    Given I have a registration request for a new user
    And I send a POST request to "/auth/register"
    And the response status code should be 201
    And I log in with the registered credentials
    When I send an authenticated GET request to "<path>"
    Then the response status code should be 404
    And the response body should not expose any password or hash

    Examples:
      | path                        |
      | /account/1                  |
      | /account/username/lookupuser|
      | /activities                 |
      | /activities/1               |
      | /users                      |
