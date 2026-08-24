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

package org.keycloak.tests.migration;

import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * This is test only for migration of client policies from Keycloak 13. As the format JSON format of client policies changed between Keycloak 13 and 14
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@KeycloakIntegrationTest
public class JsonFileImport1301MigrationClientPoliciesTest extends AbstractJsonFileImportMigrationTest {

    @InjectRealm(ref = "test", fromJson = "migration-realm-13.0.1-client-policies-test.json")
    ManagedRealm testManagedRealm;

    @InjectRealm(ref = "master", attachTo = "master")
    ManagedRealm masterManagedRealm;

    @Test
    void migration13_0_1_Test() {
        RealmRepresentation testRealm = adminClient.realms().realm("test").toRepresentation();

        // Stick to null for now. No support for proper migration from Keycloak 13 as client policies was preview and JSON format was changed significantly
        Assertions.assertTrue(testRealm.getParsedClientProfiles().getProfiles().isEmpty());
        Assertions.assertTrue(testRealm.getParsedClientPolicies().getPolicies().isEmpty());

        ClientProfilesRepresentation clientProfiles = adminClient.realms().realm("test").clientPoliciesProfilesResource().getProfiles(false);
        Assertions.assertTrue(clientProfiles.getProfiles().isEmpty());
        ClientPoliciesRepresentation clientPolicies = adminClient.realms().realm("test").clientPoliciesPoliciesResource().getPolicies();
        Assertions.assertTrue(clientPolicies.getPolicies().isEmpty());
        testViewGroups(masterRealm);
    }
}
