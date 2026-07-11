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
package org.keycloak.tests.migration;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * This is a test only for migration of client policies from Keycloak 13. As the
 * JSON format of client policies changed between Keycloak 13 and 14.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@KeycloakIntegrationTest
public class JsonFileImport1301MigrationClientPoliciesTest {

    /**
     * The "test" realm loaded from the Keycloak 13.0.1 export JSON.
     * The framework creates it via adminClient.realms().create() and removes it after the test class.
     */
    @InjectRealm(fromJson = "migration-realm-13.0.1-client-policies.json", lifecycle = LifeCycle.CLASS)
    ManagedRealm testRealm;

    /**
     * Attach to the pre-existing master realm (not managed — not deleted on cleanup).
     */
    @InjectRealm(ref = "master", attachTo = "master")
    ManagedRealm masterRealm;

    @InjectAdminClient
    Keycloak adminClient;

    /**
     * Verifies that client profiles and client policies from a Keycloak 13.0.1
     * export are migrated to empty collections after import into the current
     * server, because the JSON format changed significantly between KC 13 and 14
     * and no proper migration path exists for preview-era client policies.
     *
     * Also checks that the view-groups role was added to the account client in
     * master (migration to 20.x).
     */
    @Test
    public void migration13_0_1_Test() {
        RealmRepresentation testRealmRep = testRealm.admin().toRepresentation();

        // Stick to null/empty for now. No support for proper migration from Keycloak 13
        // as client policies was preview and JSON format was changed significantly.
        Assertions.assertTrue(testRealmRep.getParsedClientProfiles().getProfiles().isEmpty(),
                "Expected client profiles to be empty after migration from KC 13");
        Assertions.assertTrue(testRealmRep.getParsedClientPolicies().getPolicies().isEmpty(),
                "Expected client policies to be empty after migration from KC 13");

        ClientProfilesRepresentation clientProfiles = adminClient.realms().realm(testRealm.getName())
                .clientPoliciesProfilesResource().getProfiles(false);
        Assertions.assertTrue(clientProfiles.getProfiles().isEmpty(),
                "Expected client profiles resource to be empty after migration from KC 13");

        ClientPoliciesRepresentation clientPolicies = adminClient.realms().realm(testRealm.getName())
                .clientPoliciesPoliciesResource().getPolicies();
        Assertions.assertTrue(clientPolicies.getPolicies().isEmpty(),
                "Expected client policies resource to be empty after migration from KC 13");

        // testViewGroups on master: verifies the view-groups role was added to account client (migration to 20.x)
        ClientRepresentation accountClient = masterRealm.admin().clients()
                .findByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).get(0);
        ClientResource accountResource = masterRealm.admin().clients().get(accountClient.getId());
        Assertions.assertNotNull(accountResource.roles().get(AccountRoles.VIEW_GROUPS).toRepresentation(),
                "Expected view-groups role to be present in account client of master realm");
    }
}
