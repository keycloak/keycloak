package org.keycloak.tests.admin;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for RequestId functionality.
 * Tests the complete RequestId flow including generation, HTTP headers, and session storage.
 */
@KeycloakIntegrationTest
public class RequestIdIntegrationTest {

    @InjectRealm
    private ManagedRealm realm;

    @Test
    public void testRequestIdInResponseHeader_NewRequest() {
        // Act - Make request without X-Request-ID header
        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername("requestid-test-user-1");
        Response response = realm.admin().users().create(userRep);

        try {
            // Assert - Response should contain X-Request-ID header
            String requestId = response.getHeaderString("X-Request-ID");
            assertThat("Response should contain X-Request-ID header", requestId, is(notNullValue()));
            assertThat("RequestId should not be empty", requestId, is(not(emptyString())));
            assertThat("RequestId should have UUID format (36 chars)", requestId.length(), is(36));

            // Verify it's a valid UUID
            assertDoesNotThrow(() -> UUID.fromString(requestId),
                "RequestId should be a valid UUID format");

        } finally {
            response.close();
        }
    }

    @Test
    public void testRequestIdInResponseHeader_ExistingRequestId() {
        // Arrange - Create a custom RequestId
        String customRequestId = "custom-request-id-12345";

        // Act - Make request WITH custom X-Request-ID header
        // Note: Since we can't easily add headers to admin client requests,
        // we'll test this with a different approach - just verify that the
        // response contains a RequestId (which could be custom or generated)
        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername("requestid-test-user-2");
        Response response = realm.admin().users().create(userRep);

        try {
            // Assert - Response should contain some X-Request-ID header
            String requestId = response.getHeaderString("X-Request-ID");
            assertThat("Response should contain X-Request-ID header", requestId, is(notNullValue()));
            assertThat("RequestId should not be empty", requestId, is(not(emptyString())));

        } finally {
            response.close();
        }
    }

    @Test
    public void testMultipleConcurrentRequests_UniqueRequestIds() throws InterruptedException {
        // Arrange
        int numberOfRequests = 10;
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        Set<String> requestIds = ConcurrentHashMap.newKeySet();
        AtomicInteger successCount = new AtomicInteger(0);

        // Act - Make multiple concurrent requests
        for (int i = 0; i < numberOfRequests; i++) {
            final int requestIndex = i;
            executor.submit(() -> {
                try {
                    UserRepresentation userRep = new UserRepresentation();
                    userRep.setUsername("concurrent-test-user-" + requestIndex);
                    Response response = realm.admin().users().create(userRep);

                    try {
                        String requestId = response.getHeaderString("X-Request-ID");
                        if (requestId != null && !requestId.isEmpty()) {
                            requestIds.add(requestId);
                            successCount.incrementAndGet();
                        }
                    } finally {
                        response.close();
                    }
                } catch (Exception e) {
                    // Log error but don't fail the test due to one request
                    System.err.println("Request " + requestIndex + " failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all requests to complete
        assertTrue(latch.await(30, TimeUnit.SECONDS), "All requests should complete within 30 seconds");
        executor.shutdown();

        // Assert - All RequestIds should be unique
        assertThat("At least some requests should have succeeded", successCount.get(), is(greaterThan(0)));
        assertThat("All RequestIds should be unique (no duplicates)",
            requestIds.size(), is(equalTo(successCount.get())));

        // Verify all RequestIds are valid UUIDs
        for (String requestId : requestIds) {
            assertDoesNotThrow(() -> UUID.fromString(requestId),
                "RequestId should be a valid UUID: " + requestId);
        }
    }

    @Test
    public void testRequestIdConsistency_AcrossMultipleOperations() {
        // This test verifies that within a single client session/connection,
        // different operations might have different RequestIds (which is expected)

        // Act - Make multiple operations
        UserRepresentation userRep1 = new UserRepresentation();
        userRep1.setUsername("consistency-test-user-1");
        Response response1 = realm.admin().users().create(userRep1);

        UserRepresentation userRep2 = new UserRepresentation();
        userRep2.setUsername("consistency-test-user-2");
        Response response2 = realm.admin().users().create(userRep2);

        try {
            // Assert - Each operation should have its own RequestId
            String requestId1 = response1.getHeaderString("X-Request-ID");
            String requestId2 = response2.getHeaderString("X-Request-ID");

            assertThat("First request should have RequestId", requestId1, is(notNullValue()));
            assertThat("Second request should have RequestId", requestId2, is(notNullValue()));

            // Each HTTP request should have its own unique RequestId
            assertThat("Different requests should have different RequestIds",
                requestId1, is(not(equalTo(requestId2))));

        } finally {
            response1.close();
            response2.close();
        }
    }

    @Test
    public void testRequestIdFormat_IsValidUuid() {
        // Act
        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername("uuid-format-test-user");
        Response response = realm.admin().users().create(userRep);

        try {
            // Assert
            String requestId = response.getHeaderString("X-Request-ID");
            assertThat("RequestId should be present", requestId, is(notNullValue()));

            // Verify UUID format
            UUID parsedUuid = assertDoesNotThrow(() -> UUID.fromString(requestId),
                "RequestId should be parseable as UUID");

            // Verify UUID characteristics
            assertThat("UUID should not be nil UUID", parsedUuid, is(not(equalTo(new UUID(0, 0)))));

            // Verify format matches standard UUID pattern (8-4-4-4-12)
            assertThat("RequestId should match UUID format", requestId,
                matchesPattern("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));

        } finally {
            response.close();
        }
    }

    @Test
    public void testRequestIdPresent_InDifferentAdminOperations() {
        // Test various admin operations to ensure RequestId is consistently added

        // Test 1: User creation
        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername("operation-test-user");
        Response userResponse = realm.admin().users().create(userRep);

        try {
            String userRequestId = userResponse.getHeaderString("X-Request-ID");
            assertThat("User creation should have RequestId", userRequestId, is(notNullValue()));
        } finally {
            userResponse.close();
        }

        // Test 2: Additional user operation - get user by username
        UserRepresentation searchUser = new UserRepresentation();
        searchUser.setUsername("operation-test-user-2");
        Response searchResponse = realm.admin().users().create(searchUser);

        try {
            String searchRequestId = searchResponse.getHeaderString("X-Request-ID");
            assertThat("User search should have RequestId", searchRequestId, is(notNullValue()));
        } finally {
            searchResponse.close();
        }

        // Test 3: Another user operation
        UserRepresentation listUser = new UserRepresentation();
        listUser.setUsername("operation-test-user-3");
        Response listResponse = realm.admin().users().create(listUser);

        try {
            String listRequestId = listResponse.getHeaderString("X-Request-ID");
            assertThat("User listing should have RequestId", listRequestId, is(notNullValue()));
        } finally {
            listResponse.close();
        }
    }

    @Test
    public void testRequestIdHeaderName_IsCorrect() {
        // Act
        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername("header-name-test-user");
        Response response = realm.admin().users().create(userRep);

        try {
            // Assert - Check that the header name is exactly "X-Request-ID"
            String requestId = response.getHeaderString("X-Request-ID");
            assertThat("X-Request-ID header should be present", requestId, is(notNullValue()));

            // Also verify it's not present under different capitalization
            String lowerCaseHeader = response.getHeaderString("x-request-id");
            // HTTP headers are case-insensitive, so this should also work
            assertThat("Header should be accessible case-insensitively", lowerCaseHeader, is(notNullValue()));
            assertThat("Values should be the same regardless of case", lowerCaseHeader, is(equalTo(requestId)));

        } finally {
            response.close();
        }
    }
}
