/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.model;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.events.Details;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.mappers.GroupMembershipMapper;
import org.keycloak.protocol.oidc.mappers.UserAttributeMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class GroupTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {
        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            ClientModel app = KeycloakModelUtils.createClient(appRealm, "resource-owner");
            app.setDirectAccessGrantsEnabled(true);
            app.setSecret("secret");

            app = appRealm.getClientByClientId("test-app");
            app.setDirectAccessGrantsEnabled(true);

            UserModel user = session.users().addUser(appRealm, "direct-login");
            user.setEmail("direct-login@localhost");
            user.setEnabled(true);


            session.users().updateCredential(appRealm, user, UserCredentialModel.password("password"));
            keycloak = Keycloak.getInstance("http://localhost:8081/auth", "master", "admin", "admin", Constants.ADMIN_CLI_CLIENT_ID);
        }
    });

    protected static Keycloak keycloak;

    @Rule
    public AssertEvents events = new AssertEvents(keycloakRule);

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected OAuthClient oauth;

    @Test
    public void createAndTestGroups() throws Exception {
        RealmResource realm = keycloak.realms().realm("test");
        {
            RoleRepresentation groupRole = new RoleRepresentation();
            groupRole.setName("topRole");
            realm.roles().create(groupRole);
        }
        RoleRepresentation topRole = realm.roles().get("topRole").toRepresentation();
        {
            RoleRepresentation groupRole = new RoleRepresentation();
            groupRole.setName("level2Role");
            realm.roles().create(groupRole);
        }
        RoleRepresentation level2Role = realm.roles().get("level2Role").toRepresentation();
        {
            RoleRepresentation groupRole = new RoleRepresentation();
            groupRole.setName("level3Role");
            realm.roles().create(groupRole);
        }
        RoleRepresentation level3Role = realm.roles().get("level3Role").toRepresentation();


        GroupRepresentation topGroup = new GroupRepresentation();
        topGroup.setName("top");
        Response response = realm.groups().add(topGroup);
        response.close();
        topGroup = realm.getGroupByPath("/top");
        Assert.assertNotNull(topGroup);
        List<RoleRepresentation> roles = new LinkedList<>();
        roles.add(topRole);
        realm.groups().group(topGroup.getId()).roles().realmLevel().add(roles);

        GroupRepresentation level2Group = new GroupRepresentation();
        level2Group.setName("level2");
        response = realm.groups().group(topGroup.getId()).subGroup(level2Group);
        response.close();
        level2Group = realm.getGroupByPath("/top/level2");
        Assert.assertNotNull(level2Group);
        roles.clear();
        roles.add(level2Role);
        realm.groups().group(level2Group.getId()).roles().realmLevel().add(roles);

        GroupRepresentation level3Group = new GroupRepresentation();
        level3Group.setName("level3");
        response = realm.groups().group(level2Group.getId()).subGroup(level3Group);
        response.close();
        level3Group = realm.getGroupByPath("/top/level2/level3");
        Assert.assertNotNull(level3Group);
        roles.clear();
        roles.add(level3Role);
        realm.groups().group(level3Group.getId()).roles().realmLevel().add(roles);

        topGroup = realm.getGroupByPath("/top");
        Assert.assertEquals(1, topGroup.getRealmRoles().size());
        Assert.assertTrue(topGroup.getRealmRoles().contains("topRole"));
        Assert.assertEquals(1, topGroup.getSubGroups().size());

        level2Group = topGroup.getSubGroups().get(0);
        Assert.assertEquals("level2", level2Group.getName());
        Assert.assertEquals(1, level2Group.getRealmRoles().size());
        Assert.assertTrue(level2Group.getRealmRoles().contains("level2Role"));
        Assert.assertEquals(1, level2Group.getSubGroups().size());

        level3Group = level2Group.getSubGroups().get(0);
        Assert.assertEquals("level3", level3Group.getName());
        Assert.assertEquals(1, level3Group.getRealmRoles().size());
        Assert.assertTrue(level3Group.getRealmRoles().contains("level3Role"));

        try {
            GroupRepresentation notFound = realm.getGroupByPath("/notFound");
            Assert.fail();
        } catch (NotFoundException e) {

        }
        try {
            GroupRepresentation notFound = realm.getGroupByPath("/top/notFound");
            Assert.fail();
        } catch (NotFoundException e) {

        }

        UserRepresentation user = realm.users().search("direct-login", -1, -1).get(0);
        realm.users().get(user.getId()).joinGroup(level3Group.getId());
        List<GroupRepresentation> membership = realm.users().get(user.getId()).groups();
        Assert.assertEquals(1, membership.size());
        Assert.assertEquals("level3", membership.get(0).getName());

        AccessToken token = login("direct-login", "resource-owner", "secret", user.getId());
        Assert.assertTrue(token.getRealmAccess().getRoles().contains("topRole"));
        Assert.assertTrue(token.getRealmAccess().getRoles().contains("level2Role"));
        Assert.assertTrue(token.getRealmAccess().getRoles().contains("level3Role"));

        realm.addDefaultGroup(level3Group.getId());

        List<GroupRepresentation> defaultGroups = realm.getDefaultGroups();
        Assert.assertEquals(1, defaultGroups.size());
        Assert.assertEquals(defaultGroups.get(0).getId(), level3Group.getId());

        UserRepresentation newUser = new UserRepresentation();
        newUser.setUsername("groupUser");
        newUser.setEmail("group@group.com");
        response = realm.users().create(newUser);
        response.close();
        newUser =  realm.users().search("groupUser", -1, -1).get(0);
        membership = realm.users().get(newUser.getId()).groups();
        Assert.assertEquals(1, membership.size());
        Assert.assertEquals("level3", membership.get(0).getName());

        realm.removeDefaultGroup(level3Group.getId());
        defaultGroups = realm.getDefaultGroups();
        Assert.assertEquals(0, defaultGroups.size());

    }

    @Test
    public void testGroupMappers() throws Exception {
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                ClientModel app = appRealm.getClientByClientId("test-app");
                app.addProtocolMapper(GroupMembershipMapper.create("groups", "groups", false, null, true, true));
                app.addProtocolMapper(UserAttributeMapper.createClaimMapper("topAttribute", "topAttribute", "topAttribute", ProviderConfigProperty.STRING_TYPE, false, null, true, true, false));
                app.addProtocolMapper(UserAttributeMapper.createClaimMapper("level2Attribute", "level2Attribute", "level2Attribute", ProviderConfigProperty.STRING_TYPE, false, null, true, true, false));
            }
        }, "test");
        RealmResource realm = keycloak.realms().realm("test");
        {
            UserRepresentation user = realm.users().search("topGroupUser", -1, -1).get(0);

            AccessToken token = login(user.getUsername(), "test-app", "password", user.getId());
            Assert.assertTrue(token.getRealmAccess().getRoles().contains("user"));
            List<String> groups = (List<String>) token.getOtherClaims().get("groups");
            Assert.assertNotNull(groups);
            Assert.assertTrue(groups.size() == 1);
            Assert.assertEquals("topGroup", groups.get(0));
            Assert.assertEquals("true", token.getOtherClaims().get("topAttribute"));
        }
        {
            UserRepresentation user = realm.users().search("level2GroupUser", -1, -1).get(0);

            AccessToken token = login(user.getUsername(), "test-app", "password", user.getId());
            Assert.assertTrue(token.getRealmAccess().getRoles().contains("user"));
            Assert.assertTrue(token.getRealmAccess().getRoles().contains("admin"));
            Assert.assertTrue(token.getResourceAccess("test-app").getRoles().contains("customer-user"));
            List<String> groups = (List<String>) token.getOtherClaims().get("groups");
            Assert.assertNotNull(groups);
            Assert.assertTrue(groups.size() == 1);
            Assert.assertEquals("level2group", groups.get(0));
            Assert.assertEquals("true", token.getOtherClaims().get("topAttribute"));
            Assert.assertEquals("true", token.getOtherClaims().get("level2Attribute"));
        }

    }

    protected AccessToken login(String login, String clientId, String clientSecret, String userId) throws Exception {
        oauth.clientId(clientId);

        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest(clientSecret, login, "password");

        assertEquals(200, response.getStatusCode());

        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshToken = oauth.verifyRefreshToken(response.getRefreshToken());

        events.expectLogin()
                .client(clientId)
                .user(userId)
                .session(accessToken.getSessionState())
                .detail(Details.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .detail(Details.TOKEN_ID, accessToken.getId())
                .detail(Details.REFRESH_TOKEN_ID, refreshToken.getId())
                .detail(Details.USERNAME, login)
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.REDIRECT_URI)
                .removeDetail(Details.CONSENT)
                .assertEvent();
        return accessToken;
    }


}
