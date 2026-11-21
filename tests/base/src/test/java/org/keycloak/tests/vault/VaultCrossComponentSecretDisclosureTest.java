package org.keycloak.tests.vault;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.broker.oauth.OAuth2IdentityProviderFactory;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectHttpServer;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.util.HttpServerUtil;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;


@KeycloakIntegrationTest(config = VaultCrossComponentSecretDisclosureTest.ServerConfig.class)
public class VaultCrossComponentSecretDisclosureTest {

    private static final String REALM_NAME = "vaultpoc";
    private static final String VICTIM_CLIENT_ID = "victim-client";
    private static final String VICTIM_VAULT_KEY = "victim_secret";
    private static final String VICTIM_SECRET_VALUE = "victim-super-secret-value";

    private static final String ATTACKER = "idp-manager";
    private static final String REALM_ADMIN = "realm-manager";
    private static final String PASSWORD = "password";
    private static final String IDP_ALIAS = "attacker-idp";
    private static final String AUTHZ_PATH = "/attacker/authorize";
    private static final String TOKEN_PATH = "/attacker/token";

    @InjectRealm(config = VaultPocRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectAdminClientFactory
    AdminClientFactory adminClientFactory;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectHttpServer
    HttpServer httpServer;

    @InjectPage
    LoginPage loginPage;

    // Form body that Keycloak POSTs to the "attacker controlled" token endpoint
    private final AtomicReference<String> capturedTokenRequestBody = new AtomicReference<>();

    @BeforeEach
    public void startAttackerEndpoints() {
        httpServer.createContext(AUTHZ_PATH, this::authorize);
        httpServer.createContext(TOKEN_PATH, this::token);
    }

    @AfterEach
    public void stopAttackerEndpoints() {
        httpServer.removeContext(AUTHZ_PATH);
        httpServer.removeContext(TOKEN_PATH);
    }

    @Test
    public void idpManagerCannotSetVaultExpressionWhenUpdatingIdp() {
        IdentityProviderRepresentation idp = attackerIdp("plain-secret");

        try (Keycloak attacker = adminClientFactory.create()
                .realm(REALM_NAME)
                .username(ATTACKER)
                .password(PASSWORD)
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .build()) {
            try (Response created = attacker.realm(REALM_NAME).identityProviders().create(idp)) {
                assertThat(created.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            }

            idp.getConfig().put("clientSecret", "${vault." + VICTIM_VAULT_KEY + "}");
            assertThrows(ForbiddenException.class,
                    () -> attacker.realm(REALM_NAME).identityProviders().get(IDP_ALIAS).update(idp));
        }
    }

    @Test
    public void realmManagerCanSetVaultExpression() {
        IdentityProviderRepresentation idp = attackerIdp("${vault." + VICTIM_VAULT_KEY + "}");

        try (Keycloak admin = adminClientFactory.create()
                .realm(REALM_NAME)
                .username(REALM_ADMIN)
                .password(PASSWORD)
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .build();
             Response created = admin.realm(REALM_NAME).identityProviders().create(idp)) {
            assertThat(created.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }
    }

    private IdentityProviderRepresentation attackerIdp(String clientSecret) {
        String base = "http://localhost:" + httpServer.getAddress().getPort();

        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setAlias(IDP_ALIAS);
        idp.setProviderId(OAuth2IdentityProviderFactory.PROVIDER_ID);
        idp.setEnabled(true);
        Map<String, String> config = new HashMap<>();
        config.put("clientId", "attacker-client");
        config.put("clientSecret", clientSecret);
        config.put("authorizationUrl", base + AUTHZ_PATH);
        config.put(OAuth2IdentityProviderConfig.TOKEN_ENDPOINT_URL, base + TOKEN_PATH);
        config.put("userInfoUrl", base + "/attacker/userinfo");
        config.put("defaultScope", "openid");
        idp.setConfig(config);
        return idp;
    }

    @Test
    public void idpManagerCannotExfiltrateClientVaultSecretViaIdpClientSecret() {
        // Same Vault key as the victim client, which the attacker has no permission to read or manage
        IdentityProviderRepresentation idp = attackerIdp("${vault." + VICTIM_VAULT_KEY + "}");

        try (Keycloak attacker = adminClientFactory.create()
                .realm(REALM_NAME)
                .username(ATTACKER)
                .password(PASSWORD)
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .build();
             Response response = attacker.realm(REALM_NAME).identityProviders().create(idp)) {
            int status = response.getStatus();
            String createBody = response.readEntity(String.class);
            if (status != Response.Status.CREATED.getStatusCode()) {
                // Only a rejection caused by the Vault expression counts as secure behaviour; any other
                // failure would make this test pass vacuously
                assertThat("IdP creation failed for a reason unrelated to Vault: " + createBody,
                        createBody.toLowerCase(), containsString("vault"));
                return;
            }
        }

        // Any user logging in through the IdP makes Keycloak call the token endpoint with the resolved client secret
        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);

        String body = capturedTokenRequestBody.get();
        // Sanity check: the attacker endpoint was reached and Keycloak did send a client_secret, so a pass is not vacuous
        assertThat("Token endpoint was never called, the reproduction is inconclusive", body, notNullValue());
        assertThat(body, containsString("client_secret="));

        assertThat("Resolved Vault secret of client '" + VICTIM_CLIENT_ID + "' was sent to an IdP token URL controlled by a "
                        + "user who only has " + AdminRoles.MANAGE_IDENTITY_PROVIDERS,
                body, not(containsString(VICTIM_SECRET_VALUE)));
    }

    // Behaves like an external OAuth2 authorization endpoint: immediately redirects back with a code
    private void authorize(HttpExchange exchange) {
        Map<String, String> params = URLEncodedUtils.parse(exchange.getRequestURI(), StandardCharsets.UTF_8).stream()
                .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
        String location = params.get("redirect_uri") + "?code=attacker-code&state=" + params.get("state");
        HttpServerUtil.sendResponse(exchange, 302, Map.of("Location", List.of(location)));
    }

    // Records what Keycloak sends as client authentication, then fails the exchange
    private void token(HttpExchange exchange) {
        try {
            String body = URLDecoder.decode(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8);
            String authorization = exchange.getRequestHeaders().getFirst("Authorization");
            capturedTokenRequestBody.set(body + (authorization != null ? "&authorization=" + authorization : ""));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        HttpServerUtil.sendResponse(exchange, 400, Map.of("Content-Type", List.of("application/json")), "{\"error\":\"invalid_request\"}");
    }

    public static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            try {
                Path vaultDir = Files.createTempDirectory("kc-vault-cross-component");
                vaultDir.toFile().deleteOnExit();
                // REALM_UNDERSCORE_KEY: <realm>_<key>, underscores in the key are escaped as '__'
                Files.writeString(vaultDir.resolve(REALM_NAME + "_" + VICTIM_VAULT_KEY.replace("_", "__")), VICTIM_SECRET_VALUE);
                return config.option("vault", "file").option("vault-dir", vaultDir.toAbsolutePath().toString());
            } catch (Exception e) {
                throw new RuntimeException("Failed to prepare vault fixture directory", e);
            }
        }
    }

    public static class VaultPocRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name(REALM_NAME);

            // The client the attacker is not allowed to manage; its secret is only a Vault reference
            realm.clients(ClientBuilder.create(VICTIM_CLIENT_ID)
                    .publicClient(false)
                    .secret("${vault." + VICTIM_VAULT_KEY + "}"));

            // Delegated admin: may CRUD identity providers, but has no client permissions at all
            realm.users(UserBuilder.create(ATTACKER)
                    .email("idp-manager@localhost")
                    .firstName("Idp")
                    .lastName("Manager")
                    .password(PASSWORD)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.MANAGE_IDENTITY_PROVIDERS));

            // Trusted with all realm secrets: may still configure vault expressions
            realm.users(UserBuilder.create(REALM_ADMIN)
                    .email("realm-manager@localhost")
                    .firstName("Realm")
                    .lastName("Manager")
                    .password(PASSWORD)
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.MANAGE_REALM,
                            AdminRoles.MANAGE_IDENTITY_PROVIDERS));
            return realm;
        }
    }
}
