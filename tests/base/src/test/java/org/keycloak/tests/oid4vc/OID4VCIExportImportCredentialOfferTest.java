package org.keycloak.tests.oid4vc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.exportimport.dir.DirExportProvider;
import org.keycloak.exportimport.singlefile.SingleFileExportProviderFactory;
import org.keycloak.exportimport.util.ImportUtils;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.keycloak.constants.OID4VCIConstants.CREDENTIAL_OFFER_CREATE;
import static org.keycloak.exportimport.ExportImportConfig.ACTION;
import static org.keycloak.exportimport.ExportImportConfig.FILE;
import static org.keycloak.exportimport.ExportImportConfig.PROVIDER;
import static org.keycloak.exportimport.ExportImportConfig.REALM_NAME;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test that verifies that the {@code credential-offer-create} realm role is properly created when
 * the {@code OID4VC_VCI_REST_CREDENTIAL_OFFER} feature is enabled, and that the role survives
 * a full export/import round-trip without causing a {@code ModelDuplicateException}.
 */
@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerWithRestCredentialOfferEnabled.class)
public class OID4VCIExportImportCredentialOfferTest extends OID4VCIssuerTestBase {

    @InjectRealm(attachTo = "master", ref = "master")
    ManagedRealm masterRealm;

    @InjectRunOnServer(realmRef = "master")
    protected RunOnServerClient runOnServerMaster;

    private static final String EXPORT_FILE_NAME = "oid4vci-realm-export.json";

    private static String tempDir;

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
    public void testRealmImportWithOID4VCICredentialOfferCreateRole() throws IOException {
        // When the OID4VC_VCI_REST_CREDENTIAL_OFFER feature is enabled, the credential-offer-create
        // role should be created automatically on realm creation
        RoleRepresentation credentialOfferCreateRole = testRealm.admin().roles()
                .get(CREDENTIAL_OFFER_CREATE.getName()).toRepresentation();
        assertNotNull(credentialOfferCreateRole, "credential-offer-create role should exist after realm creation");
        assertEquals(CREDENTIAL_OFFER_CREATE.getName(), credentialOfferCreateRole.getName(),
                "Role name should match");

        // Export the realm to a single file
        String targetFilePath = tempDir + File.separator + EXPORT_FILE_NAME;
        exportRealm(targetFilePath);

        // Verify the exported file contains the credential-offer-create role
        Map<String, RealmRepresentation> exportedRealms;
        try (FileInputStream fis = new FileInputStream(targetFilePath)) {
            exportedRealms = ImportUtils.getRealmsFromStream(JsonSerialization.mapper, fis);
        }
        RealmRepresentation exportedRealm = exportedRealms.get(VCTestRealmConfig.TEST_REALM_NAME);
        assertNotNull(exportedRealm, "Exported realm should exist");
        assertTrue(exportedRealm.getRoles() != null
                        && exportedRealm.getRoles().getRealm() != null
                        && exportedRealm.getRoles().getRealm().stream()
                        .anyMatch(role -> CREDENTIAL_OFFER_CREATE.getName().equals(role.getName())),
                "Exported realm should contain credential-offer-create role");

        // Remove the realm
        testRealm.admin().remove();

        // Import the realm back — this should succeed without ModelDuplicateException
        importRealm(targetFilePath);

        // Verify the realm was imported successfully
        assertRealmExists(true);

        // Verify the role still exists after import
        RoleRepresentation importedRole = testRealm.admin().roles()
                .get(CREDENTIAL_OFFER_CREATE.getName()).toRepresentation();
        assertNotNull(importedRole, "credential-offer-create role should exist after import");
        assertEquals(CREDENTIAL_OFFER_CREATE.getName(), importedRole.getName(),
                "Role name should match");
    }

    private void exportRealm(String absoluteFile) {
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

    private void importRealm(String absoluteFile) {
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
