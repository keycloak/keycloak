package org.keycloak.tests.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
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

    private ClientRepresentation client;

    @BeforeEach
    @Override
    public void before() throws Exception {
        super.before();

        client = ClientBuilder.create()
                .enabled(true)
                .clientId("dcr-vault-test-client")
                .secret("initial-literal-secret")
                .publicClient(false)
                .registrationAccessToken("test-rat-token")
                .rootUrl("http://localhost")
                .build();
        client = createClient(client);
        String clientId = client.getId();
        managedRealm.cleanup().add(r -> r.clients().get(clientId).remove());
    }

    @Test
    public void testDcrUpdateRejectsVaultExpression() {
        reg.auth(Auth.token(client.getRegistrationAccessToken()));

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
        client.setSecret(VAULT_EXPRESSION);
        client.setClientAuthenticatorType(authenticator);
        if ("client-secret-jwt".equals(authenticator)) {
            ClientBuilder.update(client).attribute(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG, "HS256");
        }
        managedRealm.admin().clients().get(client.getId()).update(client);

        reg.auth(Auth.token(client.getRegistrationAccessToken()));

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
}
