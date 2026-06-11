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

import java.util.stream.Collectors;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.CacheableStorageProviderModel;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;

import org.junit.ClassRule;
import org.junit.Test;

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
        runOnServer.run(prepareHardcodedRolesLDAPTest());
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

    /**
     * Prepare hardcoded roles LDAP tests. Creates some LDAP mappers as well as some hardcoded roles and users in LDAP
     */
    public static RunOnServer prepareHardcodedRolesLDAPTest() {
        return session -> {
            RealmModel realm = session.getContext().getRealm();
            ComponentModel ldapCompModel = LDAPTestUtils.getLdapProviderModel(realm);
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapCompModel);
            UserStorageProviderModel ldapModel = ldapFedProvider.getModel();
            ldapModel.setCachePolicy(CacheableStorageProviderModel.CachePolicy.NO_CACHE);
            ldapModel.setImportEnabled(false);
            ldapModel.getConfig().putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.READ_ONLY.name());
            realm.updateComponent(ldapModel);

            // Add a hardcoded and composite role
            RoleModel clientRole = realm.getClientByClientId("admin-cli").addRole("client_role");
            RoleModel hardcodedRole = realm.addRole("hardcoded_role");
            hardcodedRole.addCompositeRole(clientRole);

            // Add role mapper
            LDAPTestUtils.addOrUpdateHardcodedRoleMapper(realm, ldapModel);

            // Remove all LDAP users
            LDAPTestUtils.removeAllLDAPUsers(ldapFedProvider, realm);

            // Add some LDAP users for testing
            LDAPObject john = LDAPTestUtils.addLDAPUser(ldapFedProvider, realm, "johnkeycloak", "John", "Doe", "john@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ldapFedProvider, john, "Password1");
        };
    }
}
