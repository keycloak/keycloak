/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.keycloak.tests.scim.tck;

import org.keycloak.scim.client.ScimClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.scim.client.annotations.InjectScimClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The realm attribute is set, but the experimental server feature is off: SCIM must behave as it does without it.
 */
@KeycloakIntegrationTest
public class GroupOwnershipFeatureOffTest {

    @InjectRealm(config = GroupOwnershipRealmConfig.class)
    ManagedRealm realm;

    @InjectScimClient(clientId = "okta-directory")
    ScimClient okta;

    @InjectScimClient(clientId = "entra-directory")
    ScimClient entra;

    GroupOwnershipOffChecks checks;

    @BeforeEach
    public void setUp() {
        checks = new GroupOwnershipOffChecks(realm, okta, entra);
    }

    @Test
    public void testCreateWithMembersIsRefused() {
        checks.createWithMembersIsRefused();
    }

    @Test
    public void testPutWithMembersIsRefused() {
        checks.putWithMembersIsRefused();
    }

    @Test
    public void testAnyClientCanWriteAndDelete() {
        checks.anyClientCanWriteAndDelete();
    }

    @Test
    public void testUsersGroupsWritesWork() {
        checks.usersGroupsWritesWork();
    }

    @Test
    public void testReadShowsMembersOnlyOnRequest() {
        checks.readShowsMembersOnlyOnRequest();
    }
}
