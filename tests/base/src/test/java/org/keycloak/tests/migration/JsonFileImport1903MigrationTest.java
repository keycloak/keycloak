/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

import java.util.List;
import java.util.Set;

import org.keycloak.OAuth2Constants;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.userprofile.config.UPConfigUtils;

import org.junit.jupiter.api.Test;

import static org.keycloak.userprofile.DeclarativeUserProfileProvider.UP_COMPONENT_CONFIG_KEY;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that we can import json file from previous version. MigrationTest only tests DB.
 */
@KeycloakIntegrationTest
public class JsonFileImport1903MigrationTest extends AbstractJsonFileImportMigrationTest {

    @InjectRealm(ref = "migration", fromJson = "migration-realm-19.0.3-Migration.json")
    ManagedRealm migrationManagedRealm;

    @InjectRealm(ref = "migration2", fromJson = "migration-realm-19.0.3-Migration2.json")
    ManagedRealm migration2ManagedRealm;

    @InjectRealm(ref = "master", attachTo = "master")
    ManagedRealm masterManagedRealm;

    @InjectRealm(ref = "userProfile", fromJson = "migration-realm-19.0.3-user-profile.json")
    ManagedRealm userProfileManagedRealm;

    @Test
    void migration19_0_3Test() {
        checkRealmsImported();
        testMigrationTo20_x();
        testMigrationTo21_x();
        testMigrationTo22_x();
        testMigrationTo23_x(true);
        testMigrationTo24_x_usingUserProfileMigration();
        testMigrationTo25_0_0();
        testMigrationTo26_0_0(true);
        testMigrationTo26_3_0();
    }

    @Test
    void testUserProfileMigration() throws Exception {
        List<ComponentRepresentation> userProfileComponents = adminClient.realm("migration-user-profile")
                .components()
                .query(null, "org.keycloak.userprofile.UserProfileProvider");
        assertThat(userProfileComponents, hasSize(1));
        ComponentRepresentation component = userProfileComponents.get(0);

        // Test "street" attribute being presented with the expected scope selectors
        UPConfig upConfig = UPConfigUtils.parseConfig(component.getConfig().getFirst(UP_COMPONENT_CONFIG_KEY));
        UPAttribute streetAttr = upConfig.getAttribute("street");
        assertThat(streetAttr, notNullValue());

        assertThat(streetAttr.getSelector(), notNullValue());
        assertEquals(Set.of(OAuth2Constants.SCOPE_ADDRESS), streetAttr.getSelector().getScopes());

        assertThat(streetAttr.getSelector(), notNullValue());
        assertEquals(Set.of(OAuth2Constants.SCOPE_PHONE), streetAttr.getRequired().getScopes());
    }

}
