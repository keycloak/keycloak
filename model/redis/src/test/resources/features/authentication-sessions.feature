Feature: Authentication Session Management
  As a Keycloak administrator
  I want to verify Redis authentication session management
  So that login flows work correctly with Redis storage

  Background:
    Given Redis is running on "localhost:6379"
    And Keycloak is configured with Redis providers
    And a test realm "test-realm" exists
    And a test client "test-client" exists

  @critical @auth-session
  Scenario: Create authentication session on login initiation
    When I initiate login for client "test-client"
    Then an authentication session should be created in Redis
    And the auth session key should match pattern "kc:authSessions:*"
    And the auth session should have a root session ID
    And the auth session should have a tab ID
    And the auth session TTL should be 1800 seconds

  @critical @auth-session
  Scenario: Complete login converts auth session to user session
    Given an authentication session exists for client "test-client"
    When user "testuser" completes login with password "Test123!"
    Then the authentication session should be deleted from Redis
    And a user session should be created in Redis
    And the user session should have the correct userId

  @auth-session
  Scenario: Abandoned login causes auth session expiration
    Given an authentication session exists for client "test-client"
    And the auth session timeout is 5 seconds
    When I wait for 6 seconds without completing login
    Then the authentication session should be expired in Redis
    And the auth session key should not exist

  @auth-session @multiple-tabs
  Scenario: Multiple browser tabs create separate auth sessions
    When I initiate login in tab "tab-1" for client "test-client"
    And I initiate login in tab "tab-2" for client "test-client"
    Then 2 authentication sessions should exist in Redis
    And each session should have the same root session ID
    And each session should have a different tab ID

  @auth-session @multiple-tabs
  Scenario: Complete login in one tab handles independently
    Given I have authentication sessions in "tab-1" and "tab-2"
    When user completes login in "tab-1"
    Then the "tab-1" auth session should be deleted
    And the "tab-2" auth session should still exist
    And a user session should be created

  @auth-session @notes
  Scenario: Store authentication execution state
    Given an authentication session exists for client "test-client"
    When I update auth session with execution "username-password" status "SUCCESS"
    Then the auth session in Redis should contain the execution status
    And retrieving the session should return the execution state

  @auth-session @client-notes
  Scenario: Store client-specific notes in auth session
    Given an authentication session exists for client "test-client"
    When I add client note "redirect_uri" with value "http://localhost:8080/app"
    Then the auth session should contain the client note in Redis
    And retrieving the session should return the client note

  @auth-session @restart
  Scenario: Auth sessions survive server restart
    Given an authentication session exists with 180 seconds TTL
    When Keycloak is restarted
    Then the authentication session should still exist in Redis
    And the session TTL should be preserved

  @auth-session @ttl
  Scenario: Auth session TTL reflects realm access code lifespan configuration
    Given a test realm "test-realm-custom-ttl" exists with accessCodeLifespanLogin 600 seconds
    And a test client "test-client" exists in realm "test-realm-custom-ttl"
    When I initiate login for client "test-client" in realm "test-realm-custom-ttl"
    Then an authentication session should be created in Redis
    And the auth session TTL should be approximately 600 seconds
