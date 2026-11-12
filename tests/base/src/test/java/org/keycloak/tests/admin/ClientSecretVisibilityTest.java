package org.keycloak.tests.admin;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    public static class SecretVisibilityRealmConfig implements RealmConfig {
        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.addClient(CONFIDENTIAL_CLIENT_ID)
                    .secret(CONFIDENTIAL_CLIENT_SECRET)
                    .directAccessGrantsEnabled(true);

            realm.addClient(JWT_CLIENT_ID)
                    .secret(CONFIDENTIAL_CLIENT_SECRET)
                    .attribute(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG, "HS256")
                    .authenticatorType("client-secret-jwt");

            KeyPair signingKey = KeyUtils.generateRsaKeyPair(2048);
            KeyPair encryptionKey = KeyUtils.generateRsaKeyPair(2048);
            realm.addClient(SAML_CLIENT_ID)
                    .protocol("saml")
                    .attribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "true")
                    .attribute(SamlConfigAttributes.SAML_ENCRYPT, "true")
                    .attribute(SamlConfigAttributes.SAML_SIGNING_PRIVATE_KEY, PemUtils.encodeKey(signingKey.getPrivate()))
                    .attribute(SamlConfigAttributes.SAML_SIGNING_CERTIFICATE_ATTRIBUTE,
                            PemUtils.encodeCertificate(CertificateUtils.generateV1SelfSignedCertificate(signingKey, SAML_CLIENT_ID)))
                    .attribute(SamlConfigAttributes.SAML_ENCRYPTION_PRIVATE_KEY_ATTRIBUTE, PemUtils.encodeKey(encryptionKey.getPrivate()))
                    .attribute(SamlConfigAttributes.SAML_ENCRYPTION_CERTIFICATE_ATTRIBUTE,
                            PemUtils.encodeCertificate(CertificateUtils.generateV1SelfSignedCertificate(encryptionKey, SAML_CLIENT_ID)));

            realm.addUser(VIEW_ONLY_USER)
                    .password(PASSWORD)
                    .email("viewonly@localhost")
                    .firstName("View")
                    .lastName("Only")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID,
                            AdminRoles.VIEW_CLIENTS);

            realm.addUser(MANAGE_USER)
                    .password(PASSWORD)
                    .email("manage@localhost")
                    .firstName("Manage")
                    .lastName("Clients")
                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID,
                            AdminRoles.MANAGE_CLIENTS);

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
