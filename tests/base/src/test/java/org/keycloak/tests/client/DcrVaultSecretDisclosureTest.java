package org.keycloak.tests.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.models.ClientSecretConstants;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KeycloakIntegrationTest(config = DcrVaultSecretDisclosureTest.ServerConfig.class)
public class DcrVaultSecretDisclosureTest extends AbstractClientRegistrationTest {

    private static final String VAULT_EXPRESSION = "${vault.client_secret}";
    private static final String VAULT_SECRET_VALUE = "mysecret";

    @InjectClient(config = DcrVaultClientConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedClient client;

    // The registration access token is only exposed by the Admin API via regenerateRegistrationAccessToken().
    // We refresh it once per test so all DCR calls in the test body use a valid token.
    private ClientRepresentation clientWithRat;

    @BeforeEach
    public void refreshRegistrationAccessToken() {
        clientWithRat = client.admin().regenerateRegistrationAccessToken();
    }

    @Test
    public void testDcrUpdateRejectsVaultExpression() {
        reg.auth(Auth.token(clientWithRat));

        ClientRepresentation updateRep = new ClientRepresentation();
        updateRep.setClientId(client.getClientId());
        updateRep.setSecret(VAULT_EXPRESSION);

        ClientRegistrationException ex = assertThrows(ClientRegistrationException.class, () -> reg.update(updateRep));
        assertInstanceOf(HttpErrorException.class, ex.getCause());
        assertEquals(400, ((HttpErrorException) ex.getCause()).getStatusLine().getStatusCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"client-secret", "client-secret-jwt"})
    public void testAdapterInstallationNeverResolvesVaultSecret(String authenticator) throws ClientRegistrationException {
        // Configure client with vault expression directly via Admin API (simulating operator setup)
        ClientRepresentation rep = client.admin().toRepresentation();
        rep.setSecret(VAULT_EXPRESSION);
        rep.setClientAuthenticatorType(authenticator);
        if ("client-secret-jwt".equals(authenticator)) {
            ClientBuilder.update(rep).attribute(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG, "HS256");
        }
        client.admin().update(rep);

        reg.auth(Auth.token(clientWithRat));

        // Layer 2 assertion: Adapter config must return raw expression, never resolved plaintext vault secret
        AdapterConfig config = reg.getAdapterConfig(client.getClientId());
        assertNotNull(config);

        if ("client-secret-jwt".equals(authenticator)) {
            Map<?, ?> jwtCredentials = assertInstanceOf(Map.class, config.getCredentials().get("secret-jwt"));
            assertEquals(VAULT_EXPRESSION, jwtCredentials.get("secret"));
        } else {
            assertEquals(VAULT_EXPRESSION, config.getCredentials().get("secret"));
        }
    }

    @Test
    public void testDcrUpdateRejectsVaultExpressionInRotatedSecretAttribute() {
        reg.auth(Auth.token(clientWithRat));

        ClientRepresentation updateRep = new ClientRepresentation();
        updateRep.setClientId(client.getClientId());
        updateRep.setAttributes(Map.of(ClientSecretConstants.CLIENT_ROTATED_SECRET, VAULT_EXPRESSION));

        ClientRegistrationException ex = assertThrows(ClientRegistrationException.class, () -> reg.update(updateRep));
        assertInstanceOf(HttpErrorException.class, ex.getCause());
        assertEquals(400, ((HttpErrorException) ex.getCause()).getStatusLine().getStatusCode());
    }

    public static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            try {
                Path vaultDir = Files.createTempDirectory("kc-dcr-vault-test");
                vaultDir.toFile().deleteOnExit();
                // Keycloak REALM_UNDERSCORE_KEY format: <realmName>_<secretNameEscaped>
                Files.writeString(vaultDir.resolve(REALM_NAME + "_client__secret"), VAULT_SECRET_VALUE);
                return config.option("vault", "file").option("vault-dir", vaultDir.toAbsolutePath().toString());
            } catch (Exception e) {
                throw new RuntimeException("Failed to prepare ephemeral vault fixture directory", e);
            }
        }
    }

    public static class DcrVaultClientConfig implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client
                    .enabled(true)
                    .clientId("dcr-vault-test-client")
                    .secret("initial-literal-secret")
                    .publicClient(false)
                    .rootUrl("http://localhost");
        }
    }
}
