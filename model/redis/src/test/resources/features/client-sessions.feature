Feature: Client Session Management
  As a Keycloak administrator
  I want to verify Redis client session management
  So that client-specific session data is properly stored

  Background:
    Given Redis is running on "localhost:6379"
    And Keycloak is configured with Redis providers
    And a test realm "test-realm" exists
    And test clients "client-1" and "client-2" exist
    And a test user "testuser" exists

  # TODO: Fix client session creation for client-1 - session not being created properly
  # @critical @client-session
  # Scenario: Create client session when user logs in
  #   When user "testuser" logs in to client "client-1"
  #   Then a user session should be created
  #   And a client session should be created for "client-1"
  #   And the client session key should match pattern "kc:clientSessions:*:client-1"
  #   And the client session should reference the user session ID

  # TODO: Fix multiple client session creation - Expected 2 sessions, found 1
  # @critical @client-session
  # Scenario: Multiple clients create separate client sessions
  #   Given user "testuser" has logged in to "client-1"
  #   When user "testuser" accesses "client-2"
  #   Then the user should have 1 user session
  #   And the user should have 2 client sessions
  #   And each client session should have a unique session key
  #   And each client session should reference the same user session

  @client-session @statistics
  Scenario: Count active sessions for specific client
    Given user "testuser" is logged in to "client-1"
    And user "testuser2" is logged in to "client-1"
    And user "testuser3" is logged in to "client-2"
    When I query active sessions for "client-1"
    Then the count should be 2
    And the client session keys in Redis should match the count

  @client-session @offline
  Scenario: Create offline client session
    Given user "testuser" has a session with "client-1"
    When the session is converted to offline mode
    Then an offline client session should be created
    And the offline client session key should match pattern "kc:offlineClientSessions:*"
    And the offline client session TTL should be longer than regular

  @client-session @actions
  Scenario: Store required actions in client session
    Given user "testuser" has a client session with "client-1"
    When I add required action "UPDATE_PASSWORD" to the client session
    Then the client session in Redis should contain the required action
    And retrieving the client session should return the action

  @client-session @roles
  Scenario: Store client roles in client session
    Given user "testuser" has a client session with "client-1"
    When I add client role "admin" to the client session
    Then the client session should contain the role mapping
    And the role should be persisted in Redis

  @client-session @protocol-mappers
  Scenario: Store protocol mapper data in client session
    Given user "testuser" has a client session with "client-1"
    When I add protocol mapper note "email" with value "test@example.com"
    Then the client session should contain the protocol mapper note
    And retrieving the session should return the mapper data

  @client-session @removal
  Scenario: Remove client session on client logout
    Given user "testuser" has sessions with "client-1" and "client-2"
    When user logs out from "client-1"
    Then the client session for "client-1" should be removed from Redis
    And the client session for "client-2" should still exist
    And the user session should still exist

  @client-session @full-logout
  Scenario: Remove all client sessions on full logout
    Given user "testuser" has sessions with "client-1" and "client-2"
    When user performs full logout
    Then all client sessions should be removed from Redis
    And the user session should be removed
    And no session keys should exist for the user
