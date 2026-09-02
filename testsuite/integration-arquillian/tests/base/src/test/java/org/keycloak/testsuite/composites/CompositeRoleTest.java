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
package org.keycloak.testsuite.composites;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RoleBuilder;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.AdminApiUtil;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class CompositeRoleTest extends AbstractCompositeKeycloakTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmBuilder realmBuilder = RealmBuilder.create()
                .name("test")
                .ssoSessionIdleTimeout(3000)
                .accessTokenLifespan(10000)
                .ssoSessionMaxLifespan(10000)
                .accessCodeLifespanUserAction(1000)
                .accessCodeLifespan(1000)
                .sslRequired(SslRequired.EXTERNAL.toString());


        realmBuilder.realmRoles(
                RoleBuilder.create().name("REALM_ROLE_1"),
                RoleBuilder.create().name("REALM_COMPOSITE_1").composite(true).realmComposite("REALM_ROLE_1"),
                RoleBuilder.create("REALM_ROLE_2"),
                RoleBuilder.create("REALM_ROLE_3")
        );

        UserBuilder realmCompositeUser = UserBuilder.create()
                .username("REALM_COMPOSITE_1_USER")
                .enabled(true)
                .password("password")
                .realmRoles("REALM_COMPOSITE_1");
        realmBuilder.users(realmCompositeUser);

        UserBuilder realmRole1User = UserBuilder.create()
                .username("REALM_ROLE_1_USER")
                .enabled(true)
                .password("password")
                .realmRoles("REALM_ROLE_1");
        realmBuilder.users(realmRole1User);

        ClientBuilder realmComposite1Application = ClientBuilder.create()
                .clientId("REALM_COMPOSITE_1_APPLICATION")
                .name("REALM_COMPOSITE_1_APPLICATION")
                .fullScopeEnabled(Boolean.FALSE)
                // addScopeMapping(realmComposite1)
                .redirectUris("http://localhost:8180/auth/realms/master/app/*", "https://localhost:8543/auth/realms/master/app/*")
                .baseUrl("http://localhost:8180/auth/realms/master/app/auth")
                .adminUrl("http://localhost:8180/auth/realms/master/app/logout")
                .secret("password");
        realmBuilder.clients(realmComposite1Application);

        ClientBuilder realmRole1Application = ClientBuilder.create()
                .clientId("REALM_ROLE_1_APPLICATION")
                .name("REALM_ROLE_1_APPLICATION")
                .fullScopeEnabled(Boolean.FALSE)
                // addScopeMapping(realmRole1)
                .redirectUris("http://localhost:8180/auth/realms/master/app/*", "https://localhost:8543/auth/realms/master/app/*")
                .baseUrl("http://localhost:8180/auth/realms/master/app/auth")
                .adminUrl("http://localhost:8180/auth/realms/master/app/logout")
                .secret("password");
        realmBuilder.clients(realmRole1Application);

        ClientBuilder appRoleApplication = ClientBuilder.create()
                .clientId("APP_ROLE_APPLICATION")
                .name("APP_ROLE_APPLICATION")
                .fullScopeEnabled(Boolean.FALSE)
                .redirectUris("http://localhost:8180/auth/realms/master/app/*", "https://localhost:8543/auth/realms/master/app/*")
                .baseUrl("http://localhost:8180/auth/realms/master/app/auth")
                .adminUrl("http://localhost:8180/auth/realms/master/app/logout")
                .defaultRoles("APP_ROLE_1", "APP_ROLE_2")
                .secret("password");
        realmBuilder.clients(appRoleApplication);

        UserBuilder realmAppCompositeUser = UserBuilder.create()
                .username("REALM_APP_COMPOSITE_USER")
                .password("password");
        realmBuilder.users(realmAppCompositeUser);

        UserBuilder realmAppRoleUser = UserBuilder.create()
                .username("REALM_APP_ROLE_USER")
                .password("password")
                .realmRoles("APP_ROLE_2");
        realmBuilder.users(realmAppRoleUser);

        ClientBuilder appCompositeApplication = ClientBuilder.create()
                .clientId("APP_COMPOSITE_APPLICATION")
                .name("APP_COMPOSITE_APPLICATION")
                .fullScopeEnabled(Boolean.FALSE)
                //.scopeMapping(appRole2)
                .defaultRoles("APP_COMPOSITE_ROLE")
                .redirectUris("http://localhost:8180/auth/realms/master/app/*", "https://localhost:8543/auth/realms/master/app/*")
                .baseUrl("http://localhost:8180/auth/realms/master/app/auth")
                .adminUrl("http://localhost:8180/auth/realms/master/app/logout")
                .secret("password");
        realmBuilder.clients(appCompositeApplication);

        UserBuilder appCompositeUser = UserBuilder.create()
                .username("APP_COMPOSITE_USER")
                .password("password")
                .realmRoles("REALM_COMPOSITE_1");
        realmBuilder.users(appCompositeUser);

        testRealms.add(realmBuilder.build());
    }

    @Before
    public void before() {
        if (testContext.isInitialized()) {
            return;
        }

        // addScopeMappings
        addRealmLevelScopeMapping("REALM_COMPOSITE_1_APPLICATION", "REALM_COMPOSITE_1");
        addRealmLevelScopeMapping("REALM_ROLE_1_APPLICATION", "REALM_ROLE_1");
        addClientLevelScopeMapping("APP_COMPOSITE_APPLICATION", "APP_ROLE_APPLICATION", "APP_ROLE_2");

        // createRealmAppCompositeRole
        ClientResource appRoleApplication = AdminApiUtil.findClientByClientId(managedRealm.admin(), "APP_ROLE_APPLICATION");
        RoleResource appRole1 = appRoleApplication.roles().get("APP_ROLE_1");

        RoleBuilder realmAppCompositeRole = RoleBuilder.create()
                .name("REALM_APP_COMPOSITE_ROLE");

        managedRealm.admin().roles().create(realmAppCompositeRole.build());
        String id = managedRealm.admin().roles().get("REALM_APP_COMPOSITE_ROLE").toRepresentation().getId();
        managedRealm.admin().rolesById().addComposites(id, Collections.singletonList(appRole1.toRepresentation()));

        // addRealmAppCompositeToUsers
        UserResource userRsc = AdminApiUtil.findUserByUsernameId(managedRealm.admin(), "REALM_APP_COMPOSITE_USER");
        RoleRepresentation realmAppCompositeRolee = managedRealm.admin().roles().get("REALM_APP_COMPOSITE_ROLE").toRepresentation();
        userRsc.roles().realmLevel().add(Collections.singletonList(realmAppCompositeRolee));

        // addRealmAppCompositeToUsers2
        userRsc = AdminApiUtil.findUserByUsernameId(managedRealm.admin(), "APP_COMPOSITE_USER");
        userRsc.roles().realmLevel().add(Collections.singletonList(realmAppCompositeRolee));

        ClientResource appCompositeApplication = AdminApiUtil.findClientByClientId(managedRealm.admin(), "APP_COMPOSITE_APPLICATION");
        RoleResource appCompositeRole = appCompositeApplication.roles().get("APP_COMPOSITE_ROLE");

        // addCompositeRolesToAppCompositeRoleInAppCompositeApplication
        List<RoleRepresentation> toAdd = new LinkedList<>();
        toAdd.add(managedRealm.admin().roles().get("REALM_ROLE_1").toRepresentation());
        toAdd.add(managedRealm.admin().roles().get("REALM_ROLE_2").toRepresentation());
        toAdd.add(managedRealm.admin().roles().get("REALM_ROLE_3").toRepresentation());

        ClientResource appRolesApplication = AdminApiUtil.findClientByClientId(managedRealm.admin(), "APP_ROLE_APPLICATION");
        RoleRepresentation appRole1Rep = appRolesApplication.roles().get("APP_ROLE_1").toRepresentation();
        toAdd.add(appRole1Rep);

        appCompositeRole.addComposites(toAdd);

        // Track that we initialized model already
        testContext.setInitialized(true);
    }

    private void addRealmLevelScopeMapping(String clientId, String roleName) {
        ClientResource client = AdminApiUtil.findClientByClientId(managedRealm.admin(), clientId);
        RoleRepresentation role = managedRealm.admin().roles().get(roleName).toRepresentation();
        client.getScopeMappings().realmLevel().add(Collections.singletonList(role));
    }

    private void addClientLevelScopeMapping(String targetClientId, String sourceClientId, String roleName) {
        ClientResource targetClient = AdminApiUtil.findClientByClientId(managedRealm.admin(), targetClientId);
        ClientResource sourceClient = AdminApiUtil.findClientByClientId(managedRealm.admin(), sourceClientId);
        RoleRepresentation role = sourceClient.roles().get(roleName).toRepresentation();
        targetClient.getScopeMappings().clientLevel(sourceClient.toRepresentation().getId()).add(Collections.singletonList(role));
    }

    @Page
    protected LoginPage loginPage;

    @Test
    public void testAppCompositeUser() throws Exception {
        oauth.realm("test");
        oauth.client("APP_COMPOSITE_APPLICATION", "password");
        oauth.doLogin("APP_COMPOSITE_USER", "password");

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);

        Assertions.assertEquals(200, response.getStatusCode());

        Assertions.assertEquals("Bearer", response.getTokenType());

        AccessToken token = oauth.verifyToken(response.getAccessToken());

        Assertions.assertEquals(getUserId("APP_COMPOSITE_USER"), token.getSubject());

        Assertions.assertEquals(1, token.getResourceAccess("APP_ROLE_APPLICATION").getRoles().size());
        Assertions.assertEquals(1, token.getRealmAccess().getRoles().size());
        Assertions.assertTrue(token.getResourceAccess("APP_ROLE_APPLICATION").isUserInRole("APP_ROLE_1"));
        Assertions.assertTrue(token.getRealmAccess().isUserInRole("REALM_ROLE_1"));

        AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(response.getRefreshToken());
        Assertions.assertEquals(200, refreshResponse.getStatusCode());
    }


    @Test
    public void testRealmAppCompositeUser() throws Exception {
        oauth.realm("test");
        oauth.client("APP_ROLE_APPLICATION", "password");
        oauth.doLogin("REALM_APP_COMPOSITE_USER", "password");

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);

        Assertions.assertEquals(200, response.getStatusCode());

        Assertions.assertEquals("Bearer", response.getTokenType());

        AccessToken token = oauth.verifyToken(response.getAccessToken());

        Assertions.assertEquals(getUserId("REALM_APP_COMPOSITE_USER"), token.getSubject());

        Assertions.assertEquals(1, token.getResourceAccess("APP_ROLE_APPLICATION").getRoles().size());
        Assertions.assertTrue(token.getResourceAccess("APP_ROLE_APPLICATION").isUserInRole("APP_ROLE_1"));

        AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(response.getRefreshToken());
        Assertions.assertEquals(200, refreshResponse.getStatusCode());
    }

    @Test
    public void testRealmOnlyWithUserCompositeAppComposite() throws Exception {
        oauth.realm("test");
        oauth.client("REALM_COMPOSITE_1_APPLICATION", "password");
        oauth.doLogin("REALM_COMPOSITE_1_USER", "password");

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);

        Assertions.assertEquals(200, response.getStatusCode());

        Assertions.assertEquals("Bearer", response.getTokenType());

        AccessToken token = oauth.verifyToken(response.getAccessToken());

        Assertions.assertEquals(getUserId("REALM_COMPOSITE_1_USER"), token.getSubject());

        Assertions.assertEquals(2, token.getRealmAccess().getRoles().size());
        Assertions.assertTrue(token.getRealmAccess().isUserInRole("REALM_COMPOSITE_1"));
        Assertions.assertTrue(token.getRealmAccess().isUserInRole("REALM_ROLE_1"));

        AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(response.getRefreshToken());
        Assertions.assertEquals(200, refreshResponse.getStatusCode());
    }

    @Test
    public void testRealmOnlyWithUserCompositeAppRole() throws Exception {
        oauth.realm("test");
        oauth.client("REALM_ROLE_1_APPLICATION", "password");
        oauth.doLogin("REALM_COMPOSITE_1_USER", "password");

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);

        Assertions.assertEquals(200, response.getStatusCode());

        Assertions.assertEquals("Bearer", response.getTokenType());

        AccessToken token = oauth.verifyToken(response.getAccessToken());

        Assertions.assertEquals(getUserId("REALM_COMPOSITE_1_USER"), token.getSubject());

        Assertions.assertEquals(1, token.getRealmAccess().getRoles().size());
        Assertions.assertTrue(token.getRealmAccess().isUserInRole("REALM_ROLE_1"));

        AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(response.getRefreshToken());
        Assertions.assertEquals(200, refreshResponse.getStatusCode());
    }

    @Test
    public void testRealmOnlyWithUserRoleAppComposite() throws Exception {
        oauth.realm("test");
        oauth.client("REALM_COMPOSITE_1_APPLICATION", "password");
        oauth.doLogin("REALM_ROLE_1_USER", "password");

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);

        Assertions.assertEquals(200, response.getStatusCode());

        Assertions.assertEquals("Bearer", response.getTokenType());

        AccessToken token = oauth.verifyToken(response.getAccessToken());

        Assertions.assertEquals(getUserId("REALM_ROLE_1_USER"), token.getSubject());

        Assertions.assertEquals(1, token.getRealmAccess().getRoles().size());
        Assertions.assertTrue(token.getRealmAccess().isUserInRole("REALM_ROLE_1"));

        AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(response.getRefreshToken());
        Assertions.assertEquals(200, refreshResponse.getStatusCode());
    }

    
    // KEYCLOAK-4274
    @Test
    public void testRecursiveComposites() throws Exception {
        // This will create recursive composite mappings between "REALM_COMPOSITE_1" and "REALM_ROLE_1"
        RoleRepresentation realmComposite1 = managedRealm.admin().roles().get("REALM_COMPOSITE_1").toRepresentation();
        managedRealm.admin().roles().get("REALM_ROLE_1").addComposites(Collections.singletonList(realmComposite1));

        UserResource userResource = AdminApiUtil.findUserByUsernameId(managedRealm.admin(), "REALM_COMPOSITE_1_USER");
        List<RoleRepresentation> realmRoles = userResource.roles().realmLevel().listEffective();
        Assert.assertNames(realmRoles, "REALM_COMPOSITE_1", "REALM_ROLE_1");

        userResource = AdminApiUtil.findUserByUsernameId(managedRealm.admin(), "REALM_ROLE_1_USER");
        realmRoles = userResource.roles().realmLevel().listEffective();
        Assert.assertNames(realmRoles, "REALM_COMPOSITE_1", "REALM_ROLE_1");

        // Revert
        managedRealm.admin().roles().get("REALM_ROLE_1").deleteComposites(Collections.singletonList(realmComposite1));
    }

}
