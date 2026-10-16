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

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.keycloak.component.ComponentModel;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.storage.CacheableStorageProviderModel;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.HardcodedLDAPRoleStorageMapper;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testsuite.broker.util.SimpleHttpDefault;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;
import org.keycloak.testsuite.util.ServerURLs;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.ClassRule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
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
        runOnServer.run(prepareHardcodedGroupsLDAPTest());
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

    // https://github.com/keycloak/keycloak/issues/54054
    @Test
    public void testHardcodedGroupForUserWithQueryAdminRole() throws IOException {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // John gets a query-* admin role, but neither view-users nor manage-users
            LDAPTestUtils.addOrUpdateHardcodedRoleMapper(appRealm, ctx.getLdapModel(), HardcodedLDAPRoleStorageMapper.ROLE,
                    Constants.REALM_MANAGEMENT_CLIENT_ID + "." + AdminRoles.QUERY_CLIENTS);
            appRealm.setAdminPermissionsEnabled(true);
            appRealm.getClientByClientId("test-app").setDirectAccessGrantsEnabled(true);
        });

        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("johnkeycloak", "Password1");
        assertThat(tokenResponse.getError() + ": " + tokenResponse.getErrorDescription(), tokenResponse.isSuccess(), is(true));

        // Start with no cached group queries, as after a restart or after the group was updated
        adminClient.realm(TEST_REALM_NAME).clearRealmCache();

        // John reads his groups through the account REST API, so he is the user of the request when the hardcoded
        // group is looked up. The group must be found, together with the role granted through its parent group.
        String accountGroupsUrl = ServerURLs.getAuthServerContextRoot() + "/auth/realms/" + TEST_REALM_NAME + "/account/groups";
        try (SimpleHttpResponse response = SimpleHttpDefault.doGet(accountGroupsUrl, oauth.httpClient().get())
                .auth(tokenResponse.getAccessToken()).acceptJson().asResponse()) {
            assertThat(response.asString(), response.getStatus(), is(200));
            List<GroupRepresentation> groups = response.asJson(new TypeReference<>() {});
            assertThat(groups.stream().map(GroupRepresentation::getName).toList(), hasItem("hardcoded_group"));
        }

        testingClient.server(TEST_REALM_NAME).run(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel john = session.users().getUserByUsername(realm, "johnkeycloak");
            RoleModel clientRole = realm.getClientByClientId("admin-cli").getRole("client_role");
            GroupModel hardcodedGroup = KeycloakModelUtils.findGroupByPath(session, realm, "/parent_group/hardcoded_group");

            // John is the user of the request, as when his session cookie or bearer token is verified
            session.getContext().setUserSession(session.sessions().createUserSession(null, realm, john, john.getUsername(),
                    "127.0.0.1", "form", false, null, null, UserSessionModel.SessionPersistenceState.TRANSIENT));

            assertThat(john.isMemberOf(hardcodedGroup), is(true));
            assertThat(john.hasRole(clientRole), is(true));
        });
    }

    /**
     * Prepare hardcoded groups LDAP tests. Creates some LDAP mappers as well as some built-in Groups and users in LDAP
     */
    public static RunOnServer prepareHardcodedGroupsLDAPTest() {
        return session -> {
            RealmModel realm = session.getContext().getRealm();
            ComponentModel ldapCompModel = LDAPTestUtils.getLdapProviderModel(realm);
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapCompModel);
            UserStorageProviderModel ldapModel = ldapFedProvider.getModel();
            ldapModel.setCachePolicy(CacheableStorageProviderModel.CachePolicy.NO_CACHE);
            ldapModel.setImportEnabled(false);
            ldapModel.getConfig().putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.READ_ONLY.name());
            realm.updateComponent(ldapModel);

            // Add a hardcoded group hierarchy with role
            RoleModel clientRole = realm.getClientByClientId("admin-cli").addRole("client_role");
            GroupModel parentGroup = realm.createGroup("parent_group");
            parentGroup.grantRole(clientRole);
            GroupModel hardcodedGroup = realm.createGroup("hardcoded_group");
            parentGroup.addChild(hardcodedGroup);

            // Add group mapper
            LDAPTestUtils.addOrUpdateHardcodedGroupMapper(realm, ldapModel);

            // Remove all LDAP users
            LDAPTestUtils.removeAllLDAPUsers(ldapFedProvider, realm);

            // Add some LDAP users for testing
            LDAPObject john = LDAPTestUtils.addLDAPUser(ldapFedProvider, realm, "johnkeycloak", "John", "Doe", "john@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ldapFedProvider, john, "Password1");
        };
    }

}
