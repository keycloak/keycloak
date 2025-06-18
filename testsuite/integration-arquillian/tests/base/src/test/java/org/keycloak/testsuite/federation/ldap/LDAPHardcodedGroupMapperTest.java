/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.federation.ldap;

import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.testsuite.util.LDAPRule;

import java.io.Serializable;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author sventorben
 */
public class LDAPHardcodedGroupMapperTest extends AbstractLDAPTest implements Serializable {

    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }

    @Override
    protected void afterImportTestRealm() {
        testingClient.testing().ldap(TEST_REALM_NAME).prepareHardcodedGroupsLDAPTest();
    }

    /**
     * KEYCLOAK-18308
     */
    @Test
    public void testCompositeGroups() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // check users
            UserModel john = session.users().getUserByUsername(appRealm, "johnkeycloak");
            assertThat(john, notNullValue());

            // check roles
            RoleModel clientRoleGrantedViaHardcodedGroupMembership = appRealm.getClientByClientId("admin-cli").getRole(
                    "client_role");
            assertThat(clientRoleGrantedViaHardcodedGroupMembership, notNullValue());

            // check groups
            GroupModel hardcodedGroup = appRealm.getGroupsStream()
                    .filter(it -> it.getName().equals("hardcoded_group")).findFirst().orElse(null);
            assertThat(hardcodedGroup, notNullValue());
            GroupModel parentGroup = appRealm.getGroupsStream()
                    .filter(it -> it.getName().equals("parent_group")).findFirst().orElse(null);
            assertThat(parentGroup, notNullValue());
            assertThat(hardcodedGroup.getParent(), equalTo(parentGroup));

            // check group membership
            assertThat(john.isMemberOf(hardcodedGroup), is(true));
            assertThat(john.isMemberOf(parentGroup), is(true));

            // check role membership
            assertThat(john.hasRole(clientRoleGrantedViaHardcodedGroupMembership), is(true));
        });
    }

}
