/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.organization.admin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.Profile;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.singlefile.SingleFileExportProviderFactory;
import org.keycloak.models.OrganizationModel;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.util.RealmBuilder;

@EnableFeature(Profile.Feature.ORGANIZATION)
public class OrganizationExportImportTest extends AbstractOrganizationTest {

    private static final String EXPORT_IMPORT_REALM = "organization-export-import";

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(RealmBuilder.create()
                .name(EXPORT_IMPORT_REALM)
                .organizationEnabled(true)
                .build());
        super.addTestRealms(testRealms);
    }

    @Test
    public void testExportDoesNotContainOrgStuff() throws Exception {
        RealmResource testRealm = adminClient.realm(EXPORT_IMPORT_REALM);
       
        OrganizationRepresentation org = createOrganization(testRealm, "test-org", "test-org.com");

        addMember(testRealm, org.getId(), "member@neworg.org", null, null);

        // export
        testingClient.testing().exportImport().setProvider(SingleFileExportProviderFactory.PROVIDER_ID);
        String targetFilePath = testingClient.testing().exportImport().getExportImportTestDirectory() + File.separator + "singleFile-full.json";
        testingClient.testing().exportImport().setFile(targetFilePath);

        testingClient.testing().exportImport().setAction(ExportImportConfig.ACTION_EXPORT);
        testingClient.testing().exportImport().setRealmName(null);

        testingClient.testing().exportImport().runExport();

        try (InputStream is = Files.newInputStream(Paths.get(targetFilePath))) {
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(content, Matchers.notNullValue());
            assertTrue(content.contains(EXPORT_IMPORT_REALM));

            assertFalse(content.contains(OrganizationModel.BROKER_PUBLIC));
            assertFalse(content.contains(OrganizationModel.ORGANIZATION_ATTRIBUTE));
            assertFalse(content.contains(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE));
        }
    }
}
