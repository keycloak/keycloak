package org.keycloak.services.managers;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.authentication.ClientAuthenticator;
import org.keycloak.authentication.ClientAuthenticatorFactory;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientSecretAuthenticator;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.representations.idm.ComponentRepresentation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClientInstallationUtilsTest {

    private static final String MASK = ComponentRepresentation.SECRET_VALUE;

    @Test
    public void masksOnlyDeclaredLocations() {
        Map<String, Object> nested = Map.of("password", "nested-secret", "token", "public-token", "algorithm", "HS256");
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("token", "top-secret");
        config.put("custom", nested);
        config.put("password", "public-password");
        config.put("secret", "public-secret");

        Map<String, Object> masked = mask(config, Set.of("/token", "/custom/password"));

        assertEquals(MASK, masked.get("token"));
        assertEquals(Map.of("password", MASK, "token", "public-token", "algorithm", "HS256"), masked.get("custom"));
        assertEquals("public-password", masked.get("password"));
        assertEquals("public-secret", masked.get("secret"));
        assertEquals(List.copyOf(config.keySet()), List.copyOf(masked.keySet()));
        assertEquals("top-secret", config.get("token"));
        assertSame(nested, config.get("custom"));
        assertEquals("nested-secret", nested.get("password"));
    }

    @Test
    public void preservesSharedMapsAndNestedOrder() {
        Map<String, Object> shared = new LinkedHashMap<>();
        shared.put("password", "secret");
        shared.put("algorithm", "HS256");
        Map<String, Object> config = Map.of("sensitive", shared, "public", shared);

        Map<String, Object> masked = mask(config, Set.of("/sensitive/password"));

        assertEquals(Map.of("password", MASK, "algorithm", "HS256"), masked.get("sensitive"));
        assertEquals(List.copyOf(shared.keySet()), List.copyOf(assertInstanceOf(Map.class, masked.get("sensitive")).keySet()));
        assertSame(shared, masked.get("public"));
        assertEquals("secret", shared.get("password"));
    }

    @Test
    public void permitsAbsentOptionalPaths() {
        Map<String, Object> config = Map.of("custom", Map.of("algorithm", "HS256"));
        assertEquals(config, mask(config, Set.of("/token", "/custom/password", "/absent/nested")));
        assertEquals(Map.of(), mask(Map.of(), Set.of("/secret")));
    }

    @Test
    public void masksNullAndStructuredLeafValues() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("null", null);
        config.put("list", List.of("secret"));
        config.put("array", new String[]{"secret"});
        config.put("map", Map.of("password", "secret"));
        assertEquals(Map.of("null", MASK, "list", MASK, "array", MASK, "map", MASK),
                mask(config, Set.of("/null", "/list", "/array", "/map")));
    }

    @Test
    public void sensitiveParentTakesPrecedenceOverDescendants() {
        Map<String, Object> config = Map.of("custom", Map.of("password", "secret"));
        assertEquals(Map.of("custom", MASK), mask(config, Set.of("/custom", "/custom/password")));
        assertEquals(Map.of("custom", MASK), mask(Map.of("custom", List.of("secret")), Set.of("/custom", "/custom/0")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "secret", "#/secret", "/", "//secret", "/custom/", "/custom//password"})
    public void rejectsMalformedPaths(String path) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> mask(Map.of(), Set.of(path)));
        assertTrue(error.getMessage().contains("client-secret"));
    }

    @Test
    public void rejectsNullDeclarations() {
        assertThrows(NullPointerException.class, () -> mask(Map.of(), null));
        assertThrows(IllegalArgumentException.class, () -> mask(Map.of(), java.util.Collections.singleton(null)));
    }

    @Test
    public void rejectsTraversalThroughNonMaps() {
        for (Object value : Arrays.asList(List.of("secret"), new String[]{"secret"}, "secret", null)) {
            Map<String, Object> config = new LinkedHashMap<>();
            config.put("custom", value);
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> mask(config, Set.of("/custom/0")));
            assertTrue(error.getMessage().contains("client-secret"));
            assertTrue(error.getMessage().contains("custom"));
        }
    }

    @Test
    public void emptyDeclarationsPreserveConfiguration() {
        Map<String, Object> config = Map.of("secret", "public", "custom", Map.of("password", "public"));
        assertEquals(config, mask(config, Set.of()));
    }

    @Test
    public void includingSecretsReturnsOriginalConfigurationWithoutReadingDeclarations() {
        Map<String, Object> config = Map.of("token", "secret");
        assertSame(config, getConfig(provider(config, null), true));
    }

    @Test
    public void builtInDeclarationsMaskClientSecrets() {
        assertEquals(Map.of("secret", MASK), mask(Map.of("secret", "secret"),
                new ClientIdAndSecretAuthenticator().getAdapterConfigurationSecretKeys()));
        assertEquals(Map.of("secret-jwt", Map.of("secret", MASK, "algorithm", "HS256")),
                mask(Map.of("secret-jwt", Map.of("secret", "secret", "algorithm", "HS256")),
                        new JWTClientSecretAuthenticator().getAdapterConfigurationSecretKeys()));
    }

    @Test
    public void jwtKeystoreTemplateRemainsVisible() {
        Map<String, Object> config = getConfig(new JWTClientAuthenticator(), false);
        Map<?, ?> jwt = assertInstanceOf(Map.class, config.get("jwt"));
        assertEquals("REPLACE WITH THE KEYSTORE PASSWORD", jwt.get("client-keystore-password"));
        assertEquals("REPLACE WITH THE KEY PASSWORD IN KEYSTORE", jwt.get("client-key-password"));
    }

    private Map<String, Object> mask(Map<String, Object> config, Set<String> paths) {
        return getConfig(provider(config, paths), false);
    }

    private ClientAuthenticatorFactory provider(Map<String, Object> config, Set<String> paths) {
        return new ClientIdAndSecretAuthenticator() {
            @Override
            public Map<String, Object> getAdapterConfiguration(KeycloakSession session, ClientModel client) {
                return config;
            }

            @Override
            public Set<String> getAdapterConfigurationSecretKeys() {
                return paths;
            }
        };
    }

    private Map<String, Object> getConfig(ClientAuthenticatorFactory provider, boolean includeSecrets) {
        KeycloakSessionFactory factory = proxy(KeycloakSessionFactory.class, (proxy, method, args) -> {
            if ("getProviderFactory".equals(method.getName())) {
                assertEquals(ClientAuthenticator.class, args[0]);
                assertEquals(provider.getId(), args[1]);
                return provider;
            }
            throw new UnsupportedOperationException(method.getName());
        });
        KeycloakSession session = proxy(KeycloakSession.class, (proxy, method, args) -> {
            if ("getKeycloakSessionFactory".equals(method.getName())) {
                return factory;
            }
            throw new UnsupportedOperationException(method.getName());
        });
        ClientModel client = proxy(ClientModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "getClientAuthenticatorType" -> provider.getId();
            case "getClientId" -> "test-client";
            case "getAttribute" -> null;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        return ClientInstallationUtils.getClientCredentialsAdapterConfig(session, client, includeSecrets);
    }

    private <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
