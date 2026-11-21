package org.keycloak.protocol.oidc.resourceindicators;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.token.TokenInterceptorException;
import org.keycloak.protocol.oidc.token.TokenPostProcessorContext;
import org.keycloak.representations.AccessToken;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ResourceIndicatorsPostProcessorTest {

    private static final String CUSTOM_AUDIENCE = "https://custom-audience";

    // client ID -> resource URL of the client
    private static final Map<String, String> CLIENTS = Map.of("service-a", "https://service-a", "service-b", "https://service-b");

    private final Map<String, Integer> lookups = new HashMap<>();

    @Test
    public void testClientsOfAudienceLookedUpOncePerRequest() {
        AccessToken accessToken = createAccessToken();

        process(accessToken, "https://service-b", "https://service-a");

        assertArrayEquals(new String[] { "https://service-b", "https://service-a" }, accessToken.getAudience());
        assertEquals(Map.of(CUSTOM_AUDIENCE, 1, "service-a", 1, "service-b", 1), lookups);
    }

    @Test
    public void testUnresolvedResourceRejectedWithoutRepeatedLookups() {
        AccessToken accessToken = createAccessToken();

        TokenInterceptorException exception = assertThrows(TokenInterceptorException.class,
                () -> process(accessToken, "https://service-a", "https://service-c"));

        assertEquals(OAuthErrorException.INVALID_TARGET, exception.getError());
        assertEquals(Map.of(CUSTOM_AUDIENCE, 1, "service-a", 1, "service-b", 1), lookups);
    }

    private AccessToken createAccessToken() {
        AccessToken accessToken = new AccessToken();
        // the first audience is not a client
        accessToken.audience(CUSTOM_AUDIENCE, "service-a", "service-b");
        return accessToken;
    }

    private void process(AccessToken accessToken, String... resources) {
        ClientSessionContext clientSessionCtx = proxy(ClientSessionContext.class, (proxy, method, args) ->
                OAuth2Constants.RESOURCE.equals(args[0]) ? ResourceIndicators.join(List.of(resources)) : null);
        new ResourceIndicatorsPostProcessor(createSession()).process(new TokenPostProcessorContext(null, null, null, accessToken, clientSessionCtx));
    }

    private KeycloakSession createSession() {
        ClientProvider clients = proxy(ClientProvider.class, (proxy, method, args) -> {
            String clientId = (String) args[1];
            lookups.merge(clientId, 1, Integer::sum);
            String resourceUrl = CLIENTS.get(clientId);
            return resourceUrl == null ? null : proxy(ClientModel.class, (clientProxy, clientMethod, clientArgs) -> resourceUrl);
        });
        KeycloakContext context = proxy(KeycloakContext.class, (proxy, method, args) -> null);
        return proxy(KeycloakSession.class, (proxy, method, args) -> "clients".equals(method.getName()) ? clients : context);
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, handler));
    }

}
