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
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.testsuite.util.LDAPRule;

import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * @author sventorben
 */
public class LDAPHardcodedRoleMapperTest extends AbstractLDAPTest {

    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }

    @Override
    protected void afterImportTestRealm() {
        testingClient.testing().ldap(TEST_REALM_NAME).prepareHardcodedRolesLDAPTest();
    }

    /**
     * KEYCLOAK-18308
     */
    @Test
    public void testCompositeRoles() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // check users
            UserModel john = session.users().getUserByUsername(appRealm, "johnkeycloak");
            assertThat(john, notNullValue());

            // check roles
            RoleModel hardcodedRole = appRealm.getRole("hardcoded_role");
            assertThat(hardcodedRole, notNullValue());
            RoleModel compositeClientRole = appRealm.getClientByClientId("admin-cli").getRole("client_role");
            assertThat(compositeClientRole, notNullValue());
            assertThat(hardcodedRole.isComposite(), is(true));
            assertThat(hardcodedRole.getCompositesStream().map(RoleModel::getName).collect(Collectors.toSet()),
                    containsInAnyOrder("client_role"));

            // check role membership
            assertThat(john.hasRole(hardcodedRole), is(true));
            assertThat(john.hasRole(compositeClientRole), is(true));
        });
    }

}
