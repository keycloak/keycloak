/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.model.group;

import org.junit.Test;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.testsuite.model.KeycloakModelTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

public class GroupModelTest extends KeycloakModelTest {

    private String realmId;
    private static final String OLD_VALUE = "oldValue";
    private static final String NEW_VALUE = "newValue";

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = createRealm(s, "realm");
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        this.realmId = realm.getId();
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        s.realms().removeRealm(realmId);
    }

    @Test
    public void testGroupAttributesSetter() {
        String groupId = withRealm(realmId, (session, realm) -> {
            GroupModel groupModel = session.groups().createGroup(realm, "my-group");
            groupModel.setSingleAttribute("key", OLD_VALUE);

            return groupModel.getId();
        });
        withRealm(realmId, (session, realm) -> {
            GroupModel groupModel = session.groups().getGroupById(realm, groupId);
            assertThat(groupModel.getAttributes().get("key"), contains(OLD_VALUE));

            // Change value to NEW_VALUE
            groupModel.setSingleAttribute("key", NEW_VALUE);

            // Check all getters return the new value
            assertThat(groupModel.getAttributes().get("key"), contains(NEW_VALUE));
            assertThat(groupModel.getFirstAttribute("key"), equalTo(NEW_VALUE));
            assertThat(groupModel.getAttributeStream("key").findFirst().get(), equalTo(NEW_VALUE));

            return null;
        });
    }

}
