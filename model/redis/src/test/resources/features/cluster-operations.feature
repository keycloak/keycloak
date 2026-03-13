Feature: Multi-Node Cluster Operations
  As a Keycloak administrator
  I want to verify Redis cluster operations
  So that multi-node deployments work correctly

  Background:
    Given Redis is running on "localhost:6379"
    And Keycloak cluster with 2 nodes is configured with Redis providers

  # TODO: All cluster scenarios require multi-node setup - currently running standalone Redis
  # @cluster @critical
  # Scenario: Session created on node1 visible on node2
  #   Given node1 is running
  #   And node2 is running
  #   When user "testuser" logs in to node1
  #   Then the session should exist in Redis
  #   And node2 should be able to retrieve the session
  #   And the session data should be identical on both nodes

  # @cluster @invalidation
  # Scenario: Session update on node1 invalidates cache on node2
  #   Given user "testuser" has a session distributed across nodes
  #   When node1 updates the session with note "update" value "v1"
  #   Then Redis should receive a cluster invalidation event
  #   And node2 should receive the invalidation via Pub/Sub
  #   And node2 should refresh the session from Redis
  #   And node2 should see the updated note

  # @cluster @logout
  # Scenario: Logout on node1 removes session from all nodes
  #   Given user "testuser" has an active session
  #   And both nodes have cached the session
  #   When user logs out from node1
  #   Then the session should be removed from Redis
  #   And node1 should remove the session from local cache
  #   And node2 should receive invalidation event
  #   And node2 should remove the session from local cache

  # @cluster @pubsub
  # Scenario: Redis Pub/Sub delivers events to all nodes
  #   Given both cluster nodes are subscribed to "kc:cluster:*"
  #   When node1 publishes event "USER_UPDATED" to Redis
  #   Then node2 should receive the event via Pub/Sub
  #   And the event payload should match the published data
  #   And event processing should complete within 1 second

  # TODO: Fix test context isolation issue
  # @cluster @node-failure
  # Scenario: Session survives node1 failure
  #   Given user "testuser" has a session on node1
  #   When node1 crashes or stops
  #   Then the session should still exist in Redis
  #   And node2 should be able to serve requests for the session
  #   And users should remain logged in

  # @cluster @split-brain
  # Scenario: Handle Redis connection failure gracefully
  #   Given both nodes are connected to Redis
  #   When Redis becomes temporarily unavailable
  #   Then nodes should log connection errors
  #   And nodes should attempt reconnection
  #   When Redis becomes available again
  #   Then nodes should reconnect automatically
  #   And session operations should resume normally

  # @cluster @statistics
  # Scenario: Aggregate session statistics across cluster
  #   Given user1 is logged in to node1
  #   And user2 is logged in to node2
  #   When I query session count on either node
  #   Then the count should be 2
  #   And the statistics should reflect all sessions in Redis

  # @cluster @load-distribution
  # Scenario: Sessions distribute across nodes
  #   When 100 users log in to the cluster
  #   Then sessions should be created in Redis
  #   And login requests should be distributed across nodes
  #   And each node should handle approximately equal load
  #   And all sessions should be retrievable from Redis

  # @cluster @concurrent-updates
  # Scenario: Concurrent session updates use optimistic locking
  #   Given user "testuser" has an active session
  #   When node1 and node2 both attempt to update the session simultaneously
  #   Then one update should succeed
  #   And the other update should detect version mismatch
  #   And the failed update should retry with latest version
  #   And final session state should be consistent

  # @cluster @restart
  # Scenario: Rolling restart maintains session availability
  #   Given active sessions exist in the cluster
  #   When node1 is restarted
  #   Then node2 continues serving requests
  #   And sessions remain accessible
  #   When node1 comes back online
  #   And node2 is restarted
  #   Then node1 continues serving requests
  #   And all sessions remain intact throughout
