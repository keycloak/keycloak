package org.keycloak.protocol.oidc.rar;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.AuthorizationDetailsJSONRepresentation;
import org.keycloak.util.JsonSerialization;

import org.junit.Test;

import static org.keycloak.protocol.oidc.rar.TestAuthorizationDetailsProcessor.NARROWED_BY;
import static org.keycloak.protocol.oidc.rar.TestAuthorizationDetailsProcessor.NarrowedAuthorizationDetail;
import static org.keycloak.protocol.oidc.rar.TestAuthorizationDetailsProcessor.PROCESSED_BY;
import static org.keycloak.protocol.oidc.rar.TestAuthorizationDetailsProcessor.SANITIZED_BY;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests that {@link AuthorizationDetailsProcessorManager} dispatches authorization_details entries to processors
 * based on {@link AuthorizationDetailsProcessor#getSupportedTypes()} rather than on the provider id.
 */
public class AuthorizationDetailsProcessorManagerTest {

    @Test
    public void dispatchesByTypeIndependentOfProviderId() throws Exception {
        AuthorizationDetailsProcessorManager manager = manager(
                factory("legacy-processor", 0, "legacy_type"),
                factory("multi", 0, "type_a", "type_b"));

        Map<String, String> processedBy = process(manager, "legacy_type", "type_a", "type_b");

        assertEquals("legacy-processor", processedBy.get("legacy_type"));
        assertEquals("multi", processedBy.get("type_a"));
        assertEquals("multi", processedBy.get("type_b"));
    }

    @Test
    public void higherOrderProcessorWinsOnTypeCollision() throws Exception {
        AuthorizationDetailsProcessorManager manager = manager(
                factory("low", 5, "shared", "low_only"),
                factory("high", 10, "shared", "high_only"));

        Map<String, String> processedBy = process(manager, "shared", "low_only", "high_only");

        assertEquals("high", processedBy.get("shared"));
        assertEquals("low", processedBy.get("low_only"));
        assertEquals("high", processedBy.get("high_only"));
    }

    @Test
    public void orderComparisonDoesNotOverflow() throws Exception {
        AuthorizationDetailsProcessorManager manager = manager(
                factory("lowest", Integer.MIN_VALUE, "shared"),
                factory("highest", Integer.MAX_VALUE, "shared"));

        assertEquals("highest", process(manager, "shared").get("shared"));
    }

    @Test
    public void handleMissingAuthorizationDetailsOnlyInvokesSelectedProcessors() {
        AuthorizationDetailsProcessorManager manager = manager(
                factory("shadowed", 1, "shared"),
                factory("low", 5, "shared", "low_only"),
                factory("high", 10, "shared", "high_only"));

        List<AuthorizationDetailsJSONRepresentation> responses = manager.handleMissingAuthorizationDetails(null, null);

        // "shadowed" lost its only type and must not contribute; "shared" must only be emitted by the winner "high"
        assertEquals(List.of("high:high_only", "high:shared", "low:low_only"), responses.stream()
                .map(response -> response.getCustomData().get(PROCESSED_BY) + ":" + response.getType()).toList());
    }

    @Test
    public void unsupportedTypeIsRejected() {
        AuthorizationDetailsProcessorManager manager = manager(factory("multi", 0, "type_a"));

        InvalidAuthorizationDetailsException ex = assertThrows(InvalidAuthorizationDetailsException.class,
                () -> process(manager, "type_a", "unknown"));
        assertTrue(ex.getMessage(), ex.getMessage().contains("unknown"));
    }

    @Test
    public void unsupportedProcessorDoesNotShadowSupportedProcessor() throws Exception {
        AuthorizationDetailsProcessorManager manager = manager(
                factory("disabled-high", 10, false, "shared", "disabled_only"),
                factory("enabled-low", 5, true, "shared", "low_only"));

        Map<String, String> processedBy = process(manager, "shared", "low_only");

        // Consistent with discovery, which advertises "shared" from the enabled processor only
        assertEquals("enabled-low", processedBy.get("shared"));
        assertEquals("enabled-low", processedBy.get("low_only"));

        InvalidAuthorizationDetailsException ex = assertThrows(InvalidAuthorizationDetailsException.class,
                () -> process(manager, "disabled_only"));
        assertTrue(ex.getMessage(), ex.getMessage().contains("disabled_only"));
    }

    @Test
    public void unsupportedProcessorIsNotAskedForMissingAuthorizationDetails() {
        AuthorizationDetailsProcessorManager manager = manager(
                factory("disabled", 10, false, "disabled_only"),
                factory("enabled", 5, true, "enabled_only"));

        List<AuthorizationDetailsJSONRepresentation> responses = manager.handleMissingAuthorizationDetails(null, null);

        assertEquals(List.of("enabled:enabled_only"), responses.stream()
                .map(response -> response.getCustomData().get(PROCESSED_BY) + ":" + response.getType()).toList());
    }

    @Test
    public void sanitizeUsesProcessorNarrowing() {
        AuthorizationDetailsProcessorManager manager = manager(factory("multi", 0, "type_a", "type_b"));

        AccessTokenResponse tokenResponse = new AccessTokenResponse();
        tokenResponse.setAuthorizationDetails(List.of(detail("type_b"), detail("type_a")));

        manager.sanitizeBeforeSendingTokenResponse(tokenResponse);

        List<AuthorizationDetailsJSONRepresentation> sanitized = tokenResponse.getAuthorizationDetails();
        assertEquals(List.of("type_b", "type_a"), sanitized.stream().map(AuthorizationDetailsJSONRepresentation::getType).toList());
        for (AuthorizationDetailsJSONRepresentation authzDetail : sanitized) {
            // The sanitizer must have received the representation narrowed by the processor owning the type
            assertThat(authzDetail, instanceOf(NarrowedAuthorizationDetail.class));
            assertEquals("multi", authzDetail.getCustomData().get(NARROWED_BY));
            assertEquals("multi", authzDetail.getCustomData().get(SANITIZED_BY));
        }
    }

    @Test
    public void sanitizeDispatchesToWinningProcessor() {
        AuthorizationDetailsProcessorManager manager = manager(
                factory("low", 5, "shared", "low_only"),
                factory("high", 10, "shared"));

        AccessTokenResponse tokenResponse = new AccessTokenResponse();
        tokenResponse.setAuthorizationDetails(List.of(detail("shared"), detail("low_only")));

        manager.sanitizeBeforeSendingTokenResponse(tokenResponse);

        assertEquals(List.of("high:high", "low:low"), tokenResponse.getAuthorizationDetails().stream()
                .map(authzDetail -> authzDetail.getCustomData().get(NARROWED_BY) + ":" + authzDetail.getCustomData().get(SANITIZED_BY)).toList());
    }

    @Test
    public void afterAuthorizationDetailsProcessedReceivesNarrowedRepresentationFromWinningProcessor() {
        KeycloakSession session = session(
                factory("low", 5, "shared", "low_only"),
                factory("high", 10, "shared"));

        new AuthorizationDetailsProcessorManager(session).afterAuthorizationDetailsProcessed(null, null,
                List.of(detail("shared"), detail("low_only")));

        // The hook signature only accepts the processor specific subtype, so each entry must have been narrowed by the
        // processor that received it. Additionally assert that "shared" went to the winner only.
        assertEquals(List.of("shared:high"), afterProcessed(session, "high"));
        assertEquals(List.of("low_only:low"), afterProcessed(session, "low"));
    }

    // Helpers ---------------------------------------------------------------------------------------------------------

    private static List<String> afterProcessed(KeycloakSession session, String providerId) {
        TestAuthorizationDetailsProcessor processor = (TestAuthorizationDetailsProcessor) session.getProvider(AuthorizationDetailsProcessor.class, providerId);
        return processor.getAfterProcessed().stream()
                .map(authzDetail -> authzDetail.getType() + ":" + authzDetail.getCustomData().get(NARROWED_BY)).toList();
    }

    private static Map<String, String> process(AuthorizationDetailsProcessorManager manager, String... types) throws Exception {
        List<AuthorizationDetailsJSONRepresentation> request = Arrays.stream(types).map(AuthorizationDetailsProcessorManagerTest::detail).toList();
        List<AuthorizationDetailsJSONRepresentation> responses = manager.processAuthorizationDetails(null, null, JsonSerialization.writeValueAsString(request));
        assertEquals(types.length, responses.size());
        return responses.stream().collect(Collectors.toMap(AuthorizationDetailsJSONRepresentation::getType,
                response -> (String) response.getCustomData().get(PROCESSED_BY)));
    }

    private static AuthorizationDetailsJSONRepresentation detail(String type) {
        AuthorizationDetailsJSONRepresentation detail = new AuthorizationDetailsJSONRepresentation();
        detail.setType(type);
        return detail;
    }

    private static AuthorizationDetailsProcessorFactory factory(String id, int order, String... supportedTypes) {
        return factory(id, order, true, supportedTypes);
    }

    private static AuthorizationDetailsProcessorFactory factory(String id, int order, boolean supported, String... supportedTypes) {
        return new AuthorizationDetailsProcessorFactory() {
            @Override
            public AuthorizationDetailsProcessor<?> create(KeycloakSession session) {
                return new TestAuthorizationDetailsProcessor(id, supported, Set.of(supportedTypes));
            }

            @Override
            public void init(Config.Scope config) {
            }

            @Override
            public String getId() {
                return id;
            }

            @Override
            public int order() {
                return order;
            }
        };
    }

    private static AuthorizationDetailsProcessorManager manager(AuthorizationDetailsProcessorFactory... factories) {
        return new AuthorizationDetailsProcessorManager(session(factories));
    }

    /**
     * Creates a mocked {@link KeycloakSession}, which only knows the given factories. Like the real session, it creates
     * one provider instance per factory and returns that instance on subsequent lookups.
     */
    private static KeycloakSession session(AuthorizationDetailsProcessorFactory... factories) {
        Map<String, AuthorizationDetailsProcessorFactory> factoriesById = Arrays.stream(factories)
                .collect(Collectors.toMap(ProviderFactory::getId, Function.identity()));
        Map<String, AuthorizationDetailsProcessor<?>> providersById = new HashMap<>();

        KeycloakSessionFactory sessionFactory = mock(KeycloakSessionFactory.class);
        when(sessionFactory.getProviderFactoriesStream(AuthorizationDetailsProcessor.class))
                .thenAnswer(invocation -> factoriesById.values().stream().map(ProviderFactory.class::cast));

        KeycloakSession session = mock(KeycloakSession.class);
        when(session.getKeycloakSessionFactory()).thenReturn(sessionFactory);
        when(session.getProvider(eq(AuthorizationDetailsProcessor.class), anyString()))
                .thenAnswer(invocation -> {
                    String id = invocation.getArgument(1, String.class);
                    AuthorizationDetailsProcessorFactory factory = factoriesById.get(id);
                    return factory == null ? null : providersById.computeIfAbsent(id, k -> factory.create(session));
                });

        return session;
    }
}
