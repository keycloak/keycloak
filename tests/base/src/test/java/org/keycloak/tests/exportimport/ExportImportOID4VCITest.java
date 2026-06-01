/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.tests.exportimport;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.Profile;
import org.keycloak.constants.OID4VCIConstants;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.singlefile.SingleFileExportProviderFactory;
import org.keycloak.exportimport.util.ImportUtils;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testsuite.util.runonserver.ExportImportHelper;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for OID4VCI export/import with automatic role creation
 */
@KeycloakIntegrationTest(config = ExportImportOID4VCITest.OID4VCIServerConfig.class)
public class ExportImportOID4VCITest {

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectAdminClient(mode = InjectAdminClient.Mode.BOOTSTRAP)
    Keycloak adminClient;

    @Test
    public void testRealmImportWithOID4VCICredentialOfferCreateRole() throws Throwable {
        String testRealmName = "oid4vci-import-test";

        // Create a realm with OID4VCI REST credential offer enabled - credential-offer-create role will be created manually
        RealmRepresentation realmRep = new RealmRepresentation();
        realmRep.setRealm(testRealmName);
        realmRep.setEnabled(true);
        adminClient.realms().create(realmRep);

        // Verify the role exists after creation
        RealmResource realmResource = adminClient.realm(testRealmName);
        RoleRepresentation credentialOfferCreateRole = realmResource.roles().get(OID4VCIConstants.CREDENTIAL_OFFER_CREATE.getName()).toRepresentation();
        assertNotNull(credentialOfferCreateRole, "credential-offer-create role should exist after realm creation");
        assertEquals(OID4VCIConstants.CREDENTIAL_OFFER_CREATE.getName(), credentialOfferCreateRole.getName(), "Role name should match");

        // Export the realm
        runOnServer.run(ExportImportHelper.setProvider(SingleFileExportProviderFactory.PROVIDER_ID));
        runOnServer.run(ExportImportHelper.setAction(ExportImportConfig.ACTION_EXPORT));
        runOnServer.run(ExportImportHelper.setRealmName(testRealmName));
        String targetFilePath = runOnServer.fetchString(ExportImportHelper.getExportImportTestDirectory()).replace("\"","") + File.separator + "oid4vci-realm-export.json";
        runOnServer.run(ExportImportHelper.setFile(targetFilePath));
        runOnServer.run(ExportImportHelper.runExport());

        // Verify the exported file contains the role
        Map<String, RealmRepresentation> exportedRealms;
        try (FileInputStream fis = new FileInputStream(targetFilePath)) {
            exportedRealms = ImportUtils.getRealmsFromStream(JsonSerialization.mapper, fis);
        }
        RealmRepresentation exportedRealm = exportedRealms.get(testRealmName);
        assertNotNull(exportedRealm, "Exported realm should exist");
        assertTrue(exportedRealm.getRoles() != null &&
                        exportedRealm.getRoles().getRealm() != null &&
                        exportedRealm.getRoles().getRealm().stream()
                                .anyMatch(role -> OID4VCIConstants.CREDENTIAL_OFFER_CREATE.getName().equals(role.getName())),
                "Exported realm should contain credential-offer-create role");

        // Remove the realm
        adminClient.realm(testRealmName).remove();

        // Import the realm back - this should succeed without ModelDuplicateException
        runOnServer.run(ExportImportHelper.setAction(ExportImportConfig.ACTION_IMPORT));
        runOnServer.run(ExportImportHelper.runImport());

        // Verify the realm was imported successfully
        RealmResource importedRealmResource = adminClient.realm(testRealmName);
        assertNotNull(importedRealmResource, "Imported realm should exist");

        // Verify the role still exists after import
        RoleRepresentation importedRole = importedRealmResource.roles().get(OID4VCIConstants.CREDENTIAL_OFFER_CREATE.getName()).toRepresentation();
        assertNotNull(importedRole, "credential-offer-create role should exist after import");
        assertEquals(OID4VCIConstants.CREDENTIAL_OFFER_CREATE.getName(), importedRole.getName(), "Role name should match");

        // Cleanup
        adminClient.realm(testRealmName).remove();
    }

    public static class OID4VCIServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.OID4VC_VCI, Profile.Feature.OID4VC_VCI_REST_CREDENTIAL_OFFER);
        }
    }
}
