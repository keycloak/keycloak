Feature: Single-Use Objects (Action Tokens)
  As a Keycloak administrator
  I want to verify Redis single-use object storage
  So that action tokens work correctly for one-time operations

  Background:
    Given Redis is running on "localhost:6379"
    And Keycloak is configured with Redis providers
    And a test realm "test-realm" exists

  @critical @single-use
  Scenario: Store action token in Redis
    When I create an action token for password reset
    Then the token should be stored in Redis
    And the token key should match pattern "kc:actionTokens:*"
    And the token should have a TTL of 300 seconds

  @critical @single-use
  Scenario: Action token can only be used once
    Given an action token exists for password reset
    When I consume the action token for the first time
    Then the consumption should succeed
    And the token should be marked as used in Redis
    When I attempt to consume the token again
    Then the consumption should fail
    And an error indicating "already used" should be returned

  @single-use @expiration
  Scenario: Expired action token cannot be consumed
    Given an action token with TTL of 3 seconds
    When I wait for 4 seconds
    Then the token should be expired in Redis
    And attempting to consume the token should fail
    And an error indicating "expired" should be returned

  @single-use @types
  Scenario: Different token types stored independently
    When I create a "VERIFY_EMAIL" action token
    And I create a "RESET_PASSWORD" action token
    Then 2 action tokens should exist in Redis
    And each token should have its own unique key
    And each token type should be identifiable

  @single-use @concurrent
  Scenario: Concurrent consumption attempts prevented
    Given an action token exists
    When 2 concurrent requests attempt to consume the token
    Then only 1 consumption should succeed
    And the other should fail with "already used" error
    And Redis optimistic locking should prevent double usage

  @single-use @cleanup
  Scenario: Expired tokens are cleaned up automatically
    Given 10 action tokens exist with 2 second TTL
    When I wait for 3 seconds
    Then all tokens should be expired and removed from Redis
    And the token count in Redis should be 0

  @single-use @restart
  Scenario: Tokens survive server restart
    Given an action token exists with 180 second TTL
    When Keycloak is restarted
    Then the token should still exist in Redis
    And the token TTL should be preserved
    And the token can be successfully consumed
