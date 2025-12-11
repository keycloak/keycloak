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
package org.keycloak.testsuite.migration;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.OAuth2Constants;
import org.keycloak.exportimport.util.ImportUtils;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.testsuite.utils.io.IOUtil;
import org.keycloak.userprofile.config.UPConfigUtils;
import org.keycloak.util.JsonSerialization;

import org.junit.Test;

import static org.keycloak.userprofile.DeclarativeUserProfileProvider.UP_COMPONENT_CONFIG_KEY;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;

/**
 * Tests that we can import json file from previous version. MigrationTest only tests DB.
 */
public class JsonFileImport1903MigrationTest extends AbstractJsonFileImportMigrationTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        Map<String, RealmRepresentation> reps = null;
        try {
            reps = ImportUtils.getRealmsFromStream(JsonSerialization.mapper, IOUtil.class.getResourceAsStream("/migration-test/migration-realm-19.0.3.json"));
            masterRep = reps.remove("master");

            RealmRepresentation upRealm = JsonSerialization.readValue(IOUtil.class.getResourceAsStream("/migration-test/migration-realm-19.0.3-user-profile.json"), RealmRepresentation.class);
            reps.put(upRealm.getRealm(), upRealm);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (RealmRepresentation rep : reps.values()) {
            testRealms.add(rep);
        }
    }

    @Test
    public void migration19_0_3Test() throws Exception {
        checkRealmsImported();
        testMigrationTo20_x();
        testMigrationTo21_x();
        testMigrationTo22_x();
        testMigrationTo23_x(true);
        testMigrationTo24_x(true, true);
        testMigrationTo25_0_0();
        testMigrationTo26_0_0(true);
        testMigrationTo26_3_0();
    }

    @Test
    public void testUserProfileMigration() throws Exception {
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
