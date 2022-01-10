/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.client;

import org.junit.Test;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.singlefile.SingleFileExportProviderFactory;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientPolicyRepresentation;
import org.keycloak.representations.idm.ClientProfileRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.REMOTE;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
@AuthServerContainerExclude({REMOTE})
public class ClientPoliciesImportExportTest extends AbstractClientPoliciesTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realm);
	}

    @Override
    protected boolean isImportAfterEachMethod() {
        return true;
    }

    @Override
    public void beforeAbstractKeycloakTestRealmImport() {
        removeAllRealmsDespiteMaster();
    }

    @Test
    public void testSingleFileRealmExportImport() throws Throwable {
        testingClient.testing().exportImport().setProvider(SingleFileExportProviderFactory.PROVIDER_ID);
        String targetFilePath = testingClient.testing().exportImport().getExportImportTestDirectory() + File.separator + "client-policies-exported-realm.json";
        testingClient.testing().exportImport().setFile(targetFilePath);

        setupValidProfilesAndPolicies();

        testRealmExportImport();
    }

    private void testRealmExportImport() throws Exception {
        testingClient.testing().exportImport().setAction(ExportImportConfig.ACTION_EXPORT);
        testingClient.testing().exportImport().setRealmName("test");

        testingClient.testing().exportImport().runExport();

        // Delete some realm (and some data in admin realm)
        adminClient.realm("test").remove();

        Assert.assertNames(adminClient.realms().findAll(), "master");

        // Configure import
        testingClient.testing().exportImport().setAction(ExportImportConfig.ACTION_IMPORT);

        testingClient.testing().exportImport().runImport();

        // Ensure data are imported back, but just for "test" realm
        Assert.assertNames(adminClient.realms().findAll(), "master", "test");

        assertExpectedLoadedProfiles((ClientProfilesRepresentation reps)->{
            ClientProfileRepresentation rep =  getProfileRepresentation(reps, "ordinal-test-profile", false);
            assertExpectedProfile(rep, "ordinal-test-profile", "The profile that can be loaded.");
        });

        assertExpectedLoadedPolicies((ClientPoliciesRepresentation reps)->{
            ClientPolicyRepresentation rep =  getPolicyRepresentation(reps, "new-policy");
            assertExpectedPolicy("new-policy", "duplicated profiles are ignored.", true, Arrays.asList("ordinal-test-profile", "lack-of-builtin-field-test-profile"),
                    rep);
        });
    }

}
