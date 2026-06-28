package org.keycloak.tests.oid4vc;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.exportimport.dir.DirExportProvider;
import org.keycloak.exportimport.singlefile.SingleFileExportProviderFactory;
import org.keycloak.models.IssuedVerifiableCredentialModel;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.oid4vc.IssuedVerifiableCredentialRepresentation;
import org.keycloak.representations.idm.oid4vc.UserVerifiableCredentialRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.keycloak.exportimport.ExportImportConfig.ACTION;
import static org.keycloak.exportimport.ExportImportConfig.FILE;
import static org.keycloak.exportimport.ExportImportConfig.PROVIDER;
import static org.keycloak.exportimport.ExportImportConfig.REALM_NAME;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCExportImportTest extends OID4VCIssuerTestBase {

    private static String tempDir;

    @InjectRealm(attachTo = "master", ref = "master")
    ManagedRealm masterRealm;

    @InjectRunOnServer(realmRef = "master")
    protected RunOnServerClient runOnServerMaster;

    @BeforeAll
    public static void createTempDir() throws IOException {
        if (tempDir == null) {
            tempDir = Files.createTempDirectory("kc-tests").toAbsolutePath().toString();
        }
    }

    @AfterAll
    public static void deleteTempDir() throws IOException {
        DirExportProvider.recursiveDeleteDir(new File(tempDir));
    }

    @Test
    public void testExportImportUserVerifiableCredentials() {
        // Test verifiable credentials on users
        assertRealmExists(true);

        UserRepresentation john = testRealm.admin().users().search(TEST_USER).stream()
                .findFirst().orElseThrow();

        List<UserVerifiableCredentialRepresentation> verifiableCreds = testRealm.admin().users().get(john.getId()).verifiableCredentials().getCredentials();
        Map<String, List<String>> userAttributes = john.getRawAttributes();
        assertNotNull(userAttributes);
        assertUserCredentials(verifiableCreds, userAttributes, jwtTypeCredentialScopeName, sdJwtTypeCredentialScopeName, minimalJwtTypeCredentialScopeName, jwtTypeNaturalPersonScopeName, sdJwtTypeNaturalPersonScopeName);

        // Export realm
        exportRealm("oid4vc-test-realm.json");

        // Delete realm and check it is not present
        testRealm.admin().remove();
        assertRealmExists(false);

        // Import the realm. Verify same verifiable credentials
        importRealm("oid4vc-test-realm.json");
        assertRealmExists(true);

        UserRepresentation userAfterImport = testRealm.admin().users().search(TEST_USER).stream().findFirst().orElseThrow();
        List<UserVerifiableCredentialRepresentation> importedVerifiableCreds = testRealm.admin().users().get(userAfterImport.getId()).verifiableCredentials().getCredentials();

        // Verify same verifiable credentials
        assertEquals(verifiableCreds, importedVerifiableCreds);

        // Verify each credential's userAttributes are preserved
        assertUserCredentials(importedVerifiableCreds, userAttributes, jwtTypeCredentialScopeName, sdJwtTypeCredentialScopeName, minimalJwtTypeCredentialScopeName, jwtTypeNaturalPersonScopeName, sdJwtTypeNaturalPersonScopeName);

    }

    private void assertUserCredentials(List<UserVerifiableCredentialRepresentation> userCreds, Map<String, List<String>> expectedUserAttributes, String... expectedCredentialNames) {
        assertEquals(userCreds.size(), expectedCredentialNames.length);
        for (String expectedName : expectedCredentialNames) {
            UserVerifiableCredentialRepresentation rep = userCreds.stream()
                    .filter(cred -> expectedName.equals(cred.getCredentialScopeName()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Not found credential scope " + expectedName + " on user"));
            assertNotNull(rep.getCreatedDate());
            assertNotNull(rep.getRevision());
            assertEquals(expectedUserAttributes, rep.getUserAttributes());
        }
    }

    private void exportRealm(String fileName) {
        String absoluteFile = tempDir + File.separator + fileName;
        runOnServer.run(session -> {
            System.setProperty(ACTION, ExportImportConfig.ACTION_EXPORT);
            System.setProperty(PROVIDER, SingleFileExportProviderFactory.PROVIDER_ID);
            System.setProperty(FILE, absoluteFile);
            System.setProperty(REALM_NAME, VCTestRealmConfig.TEST_REALM_NAME);
            try {
                new ExportImportManager(session).runExport();
            } finally {
                System.clearProperty(REALM_NAME);
                System.clearProperty(PROVIDER);
                System.clearProperty(ACTION);
                System.clearProperty(FILE);
            }
        });
    }

    private void importRealm(String fileName) {
        String absoluteFile = tempDir + File.separator + fileName;
        runOnServer.run(session -> {
            System.setProperty(ACTION, ExportImportConfig.ACTION_IMPORT);
            System.setProperty(PROVIDER, SingleFileExportProviderFactory.PROVIDER_ID);
            System.setProperty(FILE, absoluteFile);
            System.setProperty(REALM_NAME, VCTestRealmConfig.TEST_REALM_NAME);
            try {
                new ExportImportManager(session).runImport();
            } finally {
                System.clearProperty(REALM_NAME);
                System.clearProperty(PROVIDER);
                System.clearProperty(ACTION);
                System.clearProperty(FILE);
            }
        });
    }

    @Test
    public void testExportImportIssuedVerifiableCredentials() {
        assertRealmExists(true);

        UserRepresentation john = testRealm.admin().users().search(TEST_USER).stream()
                .findFirst().orElseThrow();
        String userId = john.getId();

        List<UserVerifiableCredentialRepresentation> verifiableCreds =
            testRealm.admin().users().get(userId).verifiableCredentials().getCredentials();
        assertNotNull(verifiableCreds);

        // Get the wallet client ID
        String walletClientId = testRealm.admin().clients().findByClientId(OID4VCI_CLIENT_ID).stream()
                .findFirst()
                .orElseThrow()
                .getId();

        String scope1Id = testRealm.admin().clientScopes().findAll().stream()
                .filter(s -> jwtTypeCredentialScopeName.equals(s.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Client scope not found: " + jwtTypeCredentialScopeName))
                .getId();

        String scope2Id = testRealm.admin().clientScopes().findAll().stream()
                .filter(s -> sdJwtTypeCredentialScopeName.equals(s.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Client scope not found: " + sdJwtTypeCredentialScopeName))
                .getId();

        // Create issued credentials based on existing verifiable credentials
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(VCTestRealmConfig.TEST_REALM_NAME);
            session.getContext().setRealm(realm);

            String vc1Id = session.users().getVerifiableCredentialByClientScope(userId, scope1Id).getId();
            String vc2Id = session.users().getVerifiableCredentialByClientScope(userId, scope2Id).getId();

            IssuedVerifiableCredentialModel issuedCred1 = new IssuedVerifiableCredentialModel(userId, vc1Id, walletClientId);
            issuedCred1.setRevision("rev-001");
            issuedCred1.setIssuedAt(System.currentTimeMillis());
            session.users().addIssuedVerifiableCredential(issuedCred1);

            IssuedVerifiableCredentialModel issuedCred2 = new IssuedVerifiableCredentialModel(userId, vc2Id, walletClientId);
            issuedCred2.setRevision("rev-002");
            issuedCred2.setIssuedAt(System.currentTimeMillis());
            session.users().addIssuedVerifiableCredential(issuedCred2);
        });

        List<IssuedVerifiableCredentialRepresentation> issuedCreds = testRealm.admin().users().get(userId)
                .verifiableCredentials().getIssuedCredentials();
        assertEquals(2, issuedCreds.size());

        // Export realm
        exportRealm("oid4vc-issued-test-realm.json");

        // Delete realm
        testRealm.admin().remove();
        assertRealmExists(false);

        // Import the realm
        importRealm("oid4vc-issued-test-realm.json");
        assertRealmExists(true);

        UserRepresentation userAfterImport = testRealm.admin().users().search(TEST_USER).stream().findFirst().orElseThrow();
        List<IssuedVerifiableCredentialRepresentation> importedIssuedCreds = testRealm.admin().users().get(userAfterImport.getId())
                .verifiableCredentials().getIssuedCredentials();

        assertEquals(issuedCreds.size(), importedIssuedCreds.size());
        for (int i = 0; i < issuedCreds.size(); i++) {
            IssuedVerifiableCredentialRepresentation original = issuedCreds.get(i);
            IssuedVerifiableCredentialRepresentation imported = importedIssuedCreds.get(i);

            assertEquals(original.getCredentialType(), imported.getCredentialType());
            assertEquals(original.getRevision(), imported.getRevision());
            assertEquals(original.getIssuedAt(), imported.getIssuedAt());
            assertNotNull(imported.getId());
        }
    }

    private void assertRealmExists(boolean expectExists) {
        boolean exists = keycloak.realms().findAll()
                .stream()
                .anyMatch(realm -> VCTestRealmConfig.TEST_REALM_NAME.equals(realm.getId()));
        assertEquals(expectExists, exists);
    }
}
