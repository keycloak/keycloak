package org.keycloak.protocol.oidc.rar;

import java.util.Arrays;
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

import static org.keycloak.protocol.oidc.rar.TestAuthorizationDetailsProcessor.PROCESSED_BY;

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
    public void unsupportedTypeIsRejected() {
        AuthorizationDetailsProcessorManager manager = manager(factory("multi", 0, "type_a"));

        InvalidAuthorizationDetailsException ex = assertThrows(InvalidAuthorizationDetailsException.class,
                () -> process(manager, "type_a", "unknown"));
        assertTrue(ex.getMessage(), ex.getMessage().contains("unknown"));
    }

    @Test
    public void sanitizeUsesProcessorNarrowing() {
        AuthorizationDetailsProcessorManager manager = manager(factory("multi", 0, "type_a", "type_b"));

        AccessTokenResponse tokenResponse = new AccessTokenResponse();
        tokenResponse.setAuthorizationDetails(List.of(detail("type_b"), detail("type_a")));

        manager.sanitizeBeforeSendingTokenResponse(tokenResponse);

        assertEquals(List.of("type_b", "type_a"), tokenResponse.getAuthorizationDetails().stream()
                .map(AuthorizationDetailsJSONRepresentation::getType).toList());
    }

    // Helpers ---------------------------------------------------------------------------------------------------------

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
        return new AuthorizationDetailsProcessorFactory() {
            @Override
            public AuthorizationDetailsProcessor<?> create(KeycloakSession session) {
                return new TestAuthorizationDetailsProcessor(id, Set.of(supportedTypes));
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

    /**
     * Creates a manager backed by a mocked {@link KeycloakSession}, which only knows the given factories.
     */
    private static AuthorizationDetailsProcessorManager manager(AuthorizationDetailsProcessorFactory... factories) {
        Map<String, AuthorizationDetailsProcessorFactory> factoriesById = Arrays.stream(factories)
                .collect(Collectors.toMap(ProviderFactory::getId, Function.identity()));

        KeycloakSessionFactory sessionFactory = mock(KeycloakSessionFactory.class);
        when(sessionFactory.getProviderFactoriesStream(AuthorizationDetailsProcessor.class))
                .thenAnswer(invocation -> factoriesById.values().stream().map(ProviderFactory.class::cast));

        KeycloakSession session = mock(KeycloakSession.class);
        when(session.getKeycloakSessionFactory()).thenReturn(sessionFactory);
        when(session.getProvider(eq(AuthorizationDetailsProcessor.class), anyString()))
                .thenAnswer(invocation -> {
                    AuthorizationDetailsProcessorFactory factory = factoriesById.get(invocation.getArgument(1, String.class));
                    return factory == null ? null : factory.create(session);
                });

        return new AuthorizationDetailsProcessorManager(session);
    }
}
