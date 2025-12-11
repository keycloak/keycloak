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
 *
 */

package org.keycloak.testsuite.migration;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.keycloak.exportimport.util.ImportUtils;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.utils.io.IOUtil;
import org.keycloak.util.JsonSerialization;

import org.junit.Test;

/**
 * This is test only for migration of client policies from Keycloak 13. As the format JSON format of client policies changed between Keycloak 13 and 14
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JsonFileImport1301MigrationClientPoliciesTest extends AbstractJsonFileImportMigrationTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        Map<String, RealmRepresentation> reps = null;
        try {
            reps = ImportUtils.getRealmsFromStream(JsonSerialization.mapper, IOUtil.class.getResourceAsStream("/migration-test/migration-realm-13.0.1-client-policies.json"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (RealmRepresentation rep : reps.values()) {
            testRealms.add(rep);
        }
    }

    @Test
    public void migration13_0_1_Test() throws Exception {
        RealmRepresentation testRealm = adminClient.realms().realm("test").toRepresentation();

        // Stick to null for now. No support for proper migration from Keycloak 13 as client policies was preview and JSON format was changed significantly
        Assert.assertTrue(testRealm.getParsedClientProfiles().getProfiles().isEmpty());
        Assert.assertTrue(testRealm.getParsedClientPolicies().getPolicies().isEmpty());

        ClientProfilesRepresentation clientProfiles = adminClient.realms().realm("test").clientPoliciesProfilesResource().getProfiles(false);
        Assert.assertTrue(clientProfiles.getProfiles().isEmpty());
        ClientPoliciesRepresentation clientPolicies = adminClient.realms().realm("test").clientPoliciesPoliciesResource().getPolicies();
        Assert.assertTrue(clientPolicies.getPolicies().isEmpty());
        testViewGroups(masterRealm);
    }
}
