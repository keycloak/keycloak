package org.keycloak.tests.admin;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest
public class ClientSecretVisibilityTest {

    private static final String VIEW_ONLY_USER = "view-only-user";
    private static final String MANAGE_USER = "manage-user";
    private static final String PASSWORD = "password";
    private static final String CONFIDENTIAL_CLIENT_ID = "test-confidential";
    private static final String JWT_CLIENT_ID = "test-confidential-jwt";
    private static final String CONFIDENTIAL_CLIENT_SECRET = "test-secret-value";
    private static final String SAML_CLIENT_ID = "test-saml";
    private static final List<String> OIDC_INSTALLATION_PROVIDERS = List.of(
            "keycloak-oidc-keycloak-json",
            "keycloak-oidc-jboss-subsystem",
            "keycloak-oidc-jboss-subsystem-cli");

    @InjectRealm(config = SecretVisibilityRealmConfig.class)
    ManagedRealm realm;

    @InjectAdminClientFactory
    AdminClientFactory adminClientFactory;

    private String confidentialClientUuid;

    @BeforeEach
    public void setUp() {
        confidentialClientUuid = realm.admin()
                .clients().findByClientId(CONFIDENTIAL_CLIENT_ID)
                .get(0).getId();
    }

    @Test
    public void viewOnly_getClient_secretIsMasked() {
        try (Keycloak viewClient = createViewOnlyClient()) {
            ClientRepresentation rep = viewClient.realm(realm.getName())
                    .clients().get(confidentialClientUuid)
                    .toRepresentation();
            assertThat(rep.getSecret(), is(not(CONFIDENTIAL_CLIENT_SECRET)));
            assertThat(rep.getSecret(), anyOf(nullValue(), is(ComponentRepresentation.SECRET_VALUE)));
        }
    }

    @Test
    public void viewOnly_getClients_secretsAreMasked() {
        try (Keycloak viewClient = createViewOnlyClient()) {
            List<ClientRepresentation> clients = viewClient.realm(realm.getName())
                    .clients().findAll();

            ClientRepresentation target = clients.stream()
                    .filter(c -> CONFIDENTIAL_CLIENT_ID.equals(c.getClientId()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "Confidential client not found in list"));

            assertThat(target.getSecret(), is(not(CONFIDENTIAL_CLIENT_SECRET)));
            assertThat(target.getSecret(),
                    anyOf(nullValue(), is(ComponentRepresentation.SECRET_VALUE)));
        }
    }

    @Test
    public void viewOnly_getClientRotatedSecret_returns403() {
        try (Keycloak viewClient = createViewOnlyClient()) {
            try {
                viewClient.realm(realm.getName())
                        .clients().get(confidentialClientUuid)
                        .getClientRotatedSecret();
                fail("Expected ForbiddenException for view-only user"
                        + " on getClientRotatedSecret()");
            } catch (ForbiddenException expected) {
                // Expected -- 403 before the not-found check
            } catch (jakarta.ws.rs.NotFoundException e) {
                fail("Got 404 instead of 403 — auth check is too permissive");
            }
        }
    }

    @Test
    public void viewOnly_getClientSecret_returns403() {
        try (Keycloak viewClient = createViewOnlyClient()) {
            try {
                viewClient.realm(realm.getName())
                        .clients().get(confidentialClientUuid)
                        .getSecret();
                fail("Expected ForbiddenException for view-only user on getClientSecret()");
            } catch (ForbiddenException expected) {
                // Expected -- view-only users must not access the dedicated secret endpoint
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {CONFIDENTIAL_CLIENT_ID, JWT_CLIENT_ID})
    public void viewOnly_getInstallationProvider_secretsAreMasked(String clientId) {
        String clientUuid = realm.admin().clients().findByClientId(clientId).get(0).getId();
        try (Keycloak viewClient = createViewOnlyClient()) {
            for (String providerId : OIDC_INSTALLATION_PROVIDERS) {
                String installation = viewClient.realm(realm.getName()).clients().get(clientUuid)
                        .getInstallationProvider(providerId);

                assertThat(installation, not(containsString(CONFIDENTIAL_CLIENT_SECRET)));
                assertThat(installation, containsString(ComponentRepresentation.SECRET_VALUE));
                assertThat(installation, containsString(clientId));
                if (JWT_CLIENT_ID.equals(clientId)) {
                    assertThat(installation, containsString("HS256"));
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {CONFIDENTIAL_CLIENT_ID, JWT_CLIENT_ID})
    public void manageRole_getInstallationProvider_secretIsVisible(String clientId) {
        String clientUuid = realm.admin().clients().findByClientId(clientId).get(0).getId();
        try (Keycloak manageClient = createManageClient()) {
            for (String providerId : OIDC_INSTALLATION_PROVIDERS) {
                String installation = manageClient.realm(realm.getName()).clients().get(clientUuid)
                        .getInstallationProvider(providerId);

                assertThat(installation, containsString(CONFIDENTIAL_CLIENT_SECRET));
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void samlInstallation_privateKeysRespectManagementPermission(boolean manage) {
        ClientRepresentation saml = realm.admin().clients().findByClientId(SAML_CLIENT_ID).get(0);
        Map<String, String> attributes = realm.admin().clients().get(saml.getId()).toRepresentation().getAttributes();
        String signingKey = attributes.get(SamlConfigAttributes.SAML_SIGNING_PRIVATE_KEY);
        String encryptionKey = attributes.get(SamlConfigAttributes.SAML_ENCRYPTION_PRIVATE_KEY_ATTRIBUTE);
        String certificate = attributes.get(SamlConfigAttributes.SAML_SIGNING_CERTIFICATE_ATTRIBUTE);
        try (Keycloak caller = manage ? createManageClient() : createViewOnlyClient()) {
            for (String providerId : List.of("keycloak-saml", "keycloak-saml-subsystem", "keycloak-saml-subsystem-cli")) {
                String installation = caller.realm(realm.getName()).clients().get(saml.getId()).getInstallationProvider(providerId);
                assertThat(providerId, installation, containsString(certificate));
                if (manage) {
                    assertThat(providerId, installation, containsString(signingKey));
                    assertThat(providerId, installation, containsString(encryptionKey));
                } else {
                    assertThat(providerId, installation, not(containsString(signingKey)));
                    assertThat(providerId, installation, not(containsString(encryptionKey)));
                    assertEquals(2, installation.split("\\Q" + ComponentRepresentation.SECRET_VALUE + "\\E", -1).length - 1, providerId);
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void samlInstallation_zipPrivateKeyRespectsManagementPermission(boolean manage) throws IOException {
        ClientRepresentation saml = realm.admin().clients().findByClientId(SAML_CLIENT_ID).get(0);
        Map<String, String> attributes = realm.admin().clients().get(saml.getId()).toRepresentation().getAttributes();
        String signingKey = attributes.get(SamlConfigAttributes.SAML_SIGNING_PRIVATE_KEY);
        String certificate = attributes.get(SamlConfigAttributes.SAML_SIGNING_CERTIFICATE_ATTRIBUTE);
        String privateKeyPem = null;
        String certificatePem = null;
        try (Keycloak caller = manage ? createManageClient() : createViewOnlyClient();
                Response response = caller.realm(realm.getName()).clients().get(saml.getId()).getInstallationProviderAsResponse("mod-auth-mellon");
                ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(response.readEntity(byte[].class)))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String content = new String(zip.readAllBytes(), StandardCharsets.US_ASCII);
                if (entry.getName().endsWith("client-private-key.pem")) {
                    privateKeyPem = content;
                } else if (entry.getName().endsWith("client-cert.pem")) {
                    certificatePem = content;
                }
                if (!manage) {
                    assertThat(entry.getName(), content.replaceAll("\\s", ""), not(containsString(signingKey)));
                    assertThat(entry.getName(), content.replaceAll("\\s", ""),
                            not(containsString(attributes.get(SamlConfigAttributes.SAML_ENCRYPTION_PRIVATE_KEY_ATTRIBUTE))));
                }
            }
        }
        assertNotNull(privateKeyPem);
        assertEquals("-----BEGIN PRIVATE KEY-----" + (manage ? signingKey : ComponentRepresentation.SECRET_VALUE)
                + "-----END PRIVATE KEY-----", privateKeyPem.replaceAll("[\\r\\n]", ""));
        assertNotNull(certificatePem);
        assertEquals("-----BEGIN CERTIFICATE-----" + certificate + "-----END CERTIFICATE-----", certificatePem.replaceAll("[\\r\\n]", ""));
    }

    @Test
    public void manageRole_getClient_secretIsVisible() {
        try (Keycloak manageClient = createManageClient()) {
            ClientRepresentation rep = manageClient.realm(realm.getName())
                    .clients().get(confidentialClientUuid).toRepresentation();

            assertThat(rep.getSecret(), is(notNullValue()));
            assertThat(rep.getSecret(), is(not(ComponentRepresentation.SECRET_VALUE)));
        }
    }

    @Test
    public void manageRole_getClients_secretsAreVisible() {
        try (Keycloak manageClient = createManageClient()) {
            List<ClientRepresentation> clients = manageClient.realm(realm.getName())
                    .clients().findAll();

            ClientRepresentation target = clients.stream()
                    .filter(c -> CONFIDENTIAL_CLIENT_ID.equals(c.getClientId()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "Confidential client not found in list"));

            assertThat(target.getSecret(), is(notNullValue()));
            assertThat(target.getSecret(), is(not(ComponentRepresentation.SECRET_VALUE)));
        }
    }

    @Test
    public void manageRole_getClientSecret_returnsSecret() {
        try (Keycloak manageClient = createManageClient()) {
            CredentialRepresentation secret = manageClient.realm(realm.getName())
                    .clients().get(confidentialClientUuid)
                    .getSecret();

            assertThat(secret, is(notNullValue()));
            assertThat(secret.getValue(), is(notNullValue()));
            assertThat(secret.getValue(), is(not(ComponentRepresentation.SECRET_VALUE)));
        }
    }

    @Test
    public void publicClient_getClient_secretIsNull() {
        List<ClientRepresentation> clients = realm.admin().clients().findAll();

        ClientRepresentation publicClient = clients.stream()
                .filter(c -> "test-public".equals(c.getClientId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Public client not found"));

        assertThat(publicClient.getSecret(), is(nullValue()));
    }

    // --- Realm Configuration ---

    public static class SecretVisibilityRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.clients(ClientBuilder.create(CONFIDENTIAL_CLIENT_ID)
                    .secret(CONFIDENTIAL_CLIENT_SECRET)
                    .directAccessGrantsEnabled(true));

            ClientRepresentation jwtClient = ClientBuilder.create(JWT_CLIENT_ID)
                    .secret(CONFIDENTIAL_CLIENT_SECRET)
                    .attribute(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG, "HS256")
                    .build();
            jwtClient.setClientAuthenticatorType("client-secret-jwt");
            realm.clients(jwtClient);

            KeyPair signingKey = KeyUtils.generateRsaKeyPair(2048);
            KeyPair encryptionKey = KeyUtils.generateRsaKeyPair(2048);
            realm.clients(ClientBuilder.create(SAML_CLIENT_ID)
                    .protocol("saml")
                    .attribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "true")
                    .attribute(SamlConfigAttributes.SAML_ENCRYPT, "true")
                    .attribute(SamlConfigAttributes.SAML_SIGNING_PRIVATE_KEY, PemUtils.encodeKey(signingKey.getPrivate()))
                    .attribute(SamlConfigAttributes.SAML_SIGNING_CERTIFICATE_ATTRIBUTE,
                            PemUtils.encodeCertificate(CertificateUtils.generateV1SelfSignedCertificate(signingKey, SAML_CLIENT_ID)))
                    .attribute(SamlConfigAttributes.SAML_ENCRYPTION_PRIVATE_KEY_ATTRIBUTE, PemUtils.encodeKey(encryptionKey.getPrivate()))
                    .attribute(SamlConfigAttributes.SAML_ENCRYPTION_CERTIFICATE_ATTRIBUTE,
                            PemUtils.encodeCertificate(CertificateUtils.generateV1SelfSignedCertificate(encryptionKey, SAML_CLIENT_ID))));

            // Public client for regression test
            realm.clients(ClientBuilder.create("test-public")
                          .publicClient());

            // User with only view-clients role
            realm.users(UserBuilder.create(VIEW_ONLY_USER)
                    .password(PASSWORD)
                    .email("viewonly@localhost")
                    .firstName("View")
                    .lastName("Only")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID,
                            AdminRoles.VIEW_CLIENTS));

            // User with manage-clients role
            realm.users(UserBuilder.create(MANAGE_USER)
                    .password(PASSWORD)
                    .email("manage@localhost")
                    .firstName("Manage")
                    .lastName("Clients")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID,
                            AdminRoles.MANAGE_CLIENTS));

            return realm;
        }
    }

    private Keycloak createViewOnlyClient() {
        return adminClientFactory.create()
                .realm(realm.getName())
                .username(VIEW_ONLY_USER)
                .password(PASSWORD)
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .build();
    }

    private Keycloak createManageClient() {
        return adminClientFactory.create()
                .realm(realm.getName())
                .username(MANAGE_USER)
                .password(PASSWORD)
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .build();
    }
}
