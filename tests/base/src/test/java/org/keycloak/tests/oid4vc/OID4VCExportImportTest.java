package org.keycloak.tests.oid4vc;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.exportimport.dir.DirExportProvider;
import org.keycloak.exportimport.singlefile.SingleFileExportProviderFactory;
import org.keycloak.representations.idm.UserRepresentation;
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
        assertUserCredentials(verifiableCreds, jwtTypeCredentialScopeName, sdJwtTypeCredentialScopeName, minimalJwtTypeCredentialScopeName, jwtTypeNaturalPersonScopeName, sdJwtTypeNaturalPersonScopeName);

        // Export realm
        exportRealm("oid4vc-test-realm.json");

        // Delete realm and check it is not present
        testRealm.admin().remove();
        assertRealmExists(false);

        // Import the realm. Verify same verifiable credentials
        importRealm("oid4vc-test-realm.json");
        assertRealmExists(true);
        List<UserVerifiableCredentialRepresentation> importedVerifiableCreds = testRealm.admin().users().get(john.getId()).verifiableCredentials().getCredentials();
        assertEquals(verifiableCreds, importedVerifiableCreds);
    }

    private void assertUserCredentials(List<UserVerifiableCredentialRepresentation> userCreds, String... expectedCredentialNames) {
        assertEquals(userCreds.size(), expectedCredentialNames.length);
        for (String expectedName : expectedCredentialNames) {
            UserVerifiableCredentialRepresentation rep = userCreds.stream()
                    .filter(cred -> expectedName.equals(cred.getCredentialScopeName()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Not found credential scope " + expectedName + " on user"));
            assertNotNull(rep.getCreatedDate());
            assertNotNull(rep.getRevision());
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

    private void assertRealmExists(boolean expectExists) {
        boolean exists = keycloak.realms().findAll()
                .stream()
                .anyMatch(realm -> VCTestRealmConfig.TEST_REALM_NAME.equals(realm.getId()));
        assertEquals(expectExists, exists);
    }
}
