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

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.OAuthClient.AccessTokenResponse;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.RolesBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class CompositeRoleTest extends AbstractCompositeKeycloakTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmBuilder realmBuilder = RealmBuilder.create()
                .name("test")
                .publicKey("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB")
                .privateKey("MIICXAIBAAKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQABAoGAfmO8gVhyBxdqlxmIuglbz8bcjQbhXJLR2EoS8ngTXmN1bo2L90M0mUKSdc7qF10LgETBzqL8jYlQIbt+e6TH8fcEpKCjUlyq0Mf/vVbfZSNaVycY13nTzo27iPyWQHK5NLuJzn1xvxxrUeXI6A2WFpGEBLbHjwpx5WQG9A+2scECQQDvdn9NE75HPTVPxBqsEd2z10TKkl9CZxu10Qby3iQQmWLEJ9LNmy3acvKrE3gMiYNWb6xHPKiIqOR1as7L24aTAkEAtyvQOlCvr5kAjVqrEKXalj0Tzewjweuxc0pskvArTI2Oo070h65GpoIKLc9jf+UA69cRtquwP93aZKtW06U8dQJAF2Y44ks/mK5+eyDqik3koCI08qaC8HYq2wVl7G2QkJ6sbAaILtcvD92ToOvyGyeE0flvmDZxMYlvaZnaQ0lcSQJBAKZU6umJi3/xeEbkJqMfeLclD27XGEFoPeNrmdx0q10Azp4NfJAY+Z8KRyQCR2BEG+oNitBOZ+YXF9KCpH3cdmECQHEigJhYg+ykOvr1aiZUMFT72HU0jnmQe2FVekuG+LJUt2Tm7GtMjTFoGpf0JwrVuZN39fOYAlo+nTixgeW7X8Y=")
                .ssoSessionIdleTimeout(3000)
                .accessTokenLifespan(10000)
                .ssoSessionMaxLifespan(10000)
                .accessCodeLifespanUserAction(1000)
                .accessCodeLifespan(1000)
                .sslRequired(SslRequired.EXTERNAL.toString());


        RoleRepresentation realmRole1 = RoleBuilder.create().name("REALM_ROLE_1").build();
        RoleRepresentation realmComposite1 = RoleBuilder.create()
                .name("REALM_COMPOSITE_1")
                .composite()
                .realmComposite(realmRole1)
                .build();

        RolesBuilder roles = RolesBuilder.create()
                .realmRole(realmRole1)
                .realmRole(RoleBuilder.create().name("REALM_ROLE_2").build())
                .realmRole(RoleBuilder.create().name("REALM_ROLE_3").build())
                .realmRole(realmComposite1);
        realmBuilder.roles(roles);

        UserBuilder realmCompositeUser = UserBuilder.create()
                .username("REALM_COMPOSITE_1_USER")
                .enabled(true)
                .password("password")
                .addRoles(realmComposite1.getName());
        realmBuilder.user(realmCompositeUser);

        UserBuilder realmRole1User = UserBuilder.create()
                .username("REALM_ROLE_1_USER")
                .enabled(true)
                .password("password")
                .addRoles(realmRole1.getName());
        realmBuilder.user(realmRole1User);

        ClientBuilder realmComposite1Application = ClientBuilder.create()
                .clientId("REALM_COMPOSITE_1_APPLICATION")
                .name("REALM_COMPOSITE_1_APPLICATION")
                .fullScopeEnabled(Boolean.FALSE)
                // addScopeMapping(realmComposite1)
                .redirectUris("http://localhost:8180/auth/realms/master/app/*", "https://localhost:8543/auth/realms/master/app/*")
                .baseUrl("http://localhost:8180/auth/realms/master/app/auth")
                .adminUrl("http://localhost:8180/auth/realms/master/app/logout")
                .secret("password");
        realmBuilder.client(realmComposite1Application);

        ClientBuilder realmRole1Application = ClientBuilder.create()
                .clientId("REALM_ROLE_1_APPLICATION")
                .name("REALM_ROLE_1_APPLICATION")
                .fullScopeEnabled(Boolean.FALSE)
                // addScopeMapping(realmRole1)
                .redirectUris("http://localhost:8180/auth/realms/master/app/*", "https://localhost:8543/auth/realms/master/app/*")
                .baseUrl("http://localhost:8180/auth/realms/master/app/auth")
                .adminUrl("http://localhost:8180/auth/realms/master/app/logout")
                .secret("password");
        realmBuilder.client(realmRole1Application);

        ClientBuilder appRoleApplication = ClientBuilder.create()
                .clientId("APP_ROLE_APPLICATION")
                .name("APP_ROLE_APPLICATION")
                .fullScopeEnabled(Boolean.FALSE)
                .redirectUris("http://localhost:8180/auth/realms/master/app/*", "https://localhost:8543/auth/realms/master/app/*")
                .baseUrl("http://localhost:8180/auth/realms/master/app/auth")
                .adminUrl("http://localhost:8180/auth/realms/master/app/logout")
                .defaultRoles("APP_ROLE_1", "APP_ROLE_2")
                .secret("password");
        realmBuilder.client(appRoleApplication);

        UserBuilder realmAppCompositeUser = UserBuilder.create()
                .username("REALM_APP_COMPOSITE_USER")
                .password("password");
        realmBuilder.user(realmAppCompositeUser);

        UserBuilder realmAppRoleUser = UserBuilder.create()
                .username("REALM_APP_ROLE_USER")
                .password("password")
                .addRoles("APP_ROLE_2");
        realmBuilder.user(realmAppRoleUser);

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
        realmBuilder.client(appCompositeApplication);

        UserBuilder appCompositeUser = UserBuilder.create()
                .username("APP_COMPOSITE_USER")
                .password("password")
                .addRoles("REALM_COMPOSITE_1");
        realmBuilder.user(appCompositeUser);

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
        ClientResource appRoleApplication = ApiUtil.findClientByClientId(testRealm(), "APP_ROLE_APPLICATION");
        RoleResource appRole1 = appRoleApplication.roles().get("APP_ROLE_1");

        RoleBuilder realmAppCompositeRole = RoleBuilder.create()
                .name("REALM_APP_COMPOSITE_ROLE");

        testRealm().roles().create(realmAppCompositeRole.build());
        String id = testRealm().roles().get("REALM_APP_COMPOSITE_ROLE").toRepresentation().getId();
        testRealm().rolesById().addComposites(id, Collections.singletonList(appRole1.toRepresentation()));

        // addRealmAppCompositeToUsers
        UserResource userRsc = ApiUtil.findUserByUsernameId(testRealm(), "REALM_APP_COMPOSITE_USER");
        RoleRepresentation realmAppCompositeRolee = testRealm().roles().get("REALM_APP_COMPOSITE_ROLE").toRepresentation();
        userRsc.roles().realmLevel().add(Collections.singletonList(realmAppCompositeRolee));

        // addRealmAppCompositeToUsers2
        userRsc = ApiUtil.findUserByUsernameId(testRealm(), "APP_COMPOSITE_USER");
        userRsc.roles().realmLevel().add(Collections.singletonList(realmAppCompositeRolee));

        ClientResource appCompositeApplication = ApiUtil.findClientByClientId(testRealm(), "APP_COMPOSITE_APPLICATION");
        RoleResource appCompositeRole = appCompositeApplication.roles().get("APP_COMPOSITE_ROLE");

        // addCompositeRolesToAppCompositeRoleInAppCompositeApplication
        List<RoleRepresentation> toAdd = new LinkedList<>();
        toAdd.add(testRealm().roles().get("REALM_ROLE_1").toRepresentation());
        toAdd.add(testRealm().roles().get("REALM_ROLE_2").toRepresentation());
        toAdd.add(testRealm().roles().get("REALM_ROLE_3").toRepresentation());

        ClientResource appRolesApplication = ApiUtil.findClientByClientId(testRealm(), "APP_ROLE_APPLICATION");
        RoleRepresentation appRole1Rep = appRolesApplication.roles().get("APP_ROLE_1").toRepresentation();
        toAdd.add(appRole1Rep);

        appCompositeRole.addComposites(toAdd);

        // Track that we initialized model already
        testContext.setInitialized(true);
    }

    private void addRealmLevelScopeMapping(String clientId, String roleName) {
        ClientResource client = ApiUtil.findClientByClientId(testRealm(), clientId);
        RoleRepresentation role = testRealm().roles().get(roleName).toRepresentation();
        client.getScopeMappings().realmLevel().add(Collections.singletonList(role));
    }

    private void addClientLevelScopeMapping(String targetClientId, String sourceClientId, String roleName) {
        ClientResource targetClient = ApiUtil.findClientByClientId(testRealm(), targetClientId);
        ClientResource sourceClient = ApiUtil.findClientByClientId(testRealm(), sourceClientId);
        RoleRepresentation role = sourceClient.roles().get(roleName).toRepresentation();
        targetClient.getScopeMappings().clientLevel(sourceClient.toRepresentation().getId()).add(Collections.singletonList(role));
    }

    @Page
    protected LoginPage loginPage;

    @Test
    public void testAppCompositeUser() throws Exception {
        oauth.realm("test");
        oauth.clientId("APP_COMPOSITE_APPLICATION");
        oauth.doLogin("APP_COMPOSITE_USER", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        Assert.assertEquals(200, response.getStatusCode());

        Assert.assertEquals("bearer", response.getTokenType());

        AccessToken token = oauth.verifyToken(response.getAccessToken());

        Assert.assertEquals(getUserId("APP_COMPOSITE_USER"), token.getSubject());

        Assert.assertEquals(1, token.getResourceAccess("APP_ROLE_APPLICATION").getRoles().size());
        Assert.assertEquals(1, token.getRealmAccess().getRoles().size());
        Assert.assertTrue(token.getResourceAccess("APP_ROLE_APPLICATION").isUserInRole("APP_ROLE_1"));
        Assert.assertTrue(token.getRealmAccess().isUserInRole("REALM_ROLE_1"));

        AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(response.getRefreshToken(), "password");
        Assert.assertEquals(200, refreshResponse.getStatusCode());
    }


    @Test
    public void testRealmAppCompositeUser() throws Exception {
        oauth.realm("test");
        oauth.clientId("APP_ROLE_APPLICATION");
        oauth.doLogin("REALM_APP_COMPOSITE_USER", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        Assert.assertEquals(200, response.getStatusCode());

        Assert.assertEquals("bearer", response.getTokenType());

        AccessToken token = oauth.verifyToken(response.getAccessToken());

        Assert.assertEquals(getUserId("REALM_APP_COMPOSITE_USER"), token.getSubject());

        Assert.assertEquals(1, token.getResourceAccess("APP_ROLE_APPLICATION").getRoles().size());
        Assert.assertTrue(token.getResourceAccess("APP_ROLE_APPLICATION").isUserInRole("APP_ROLE_1"));

        AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(response.getRefreshToken(), "password");
        Assert.assertEquals(200, refreshResponse.getStatusCode());
    }

    @Test
    public void testRealmOnlyWithUserCompositeAppComposite() throws Exception {
        oauth.realm("test");
        oauth.clientId("REALM_COMPOSITE_1_APPLICATION");
        oauth.doLogin("REALM_COMPOSITE_1_USER", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        Assert.assertEquals(200, response.getStatusCode());

        Assert.assertEquals("bearer", response.getTokenType());

        AccessToken token = oauth.verifyToken(response.getAccessToken());

        Assert.assertEquals(getUserId("REALM_COMPOSITE_1_USER"), token.getSubject());

        Assert.assertEquals(2, token.getRealmAccess().getRoles().size());
        Assert.assertTrue(token.getRealmAccess().isUserInRole("REALM_COMPOSITE_1"));
        Assert.assertTrue(token.getRealmAccess().isUserInRole("REALM_ROLE_1"));

        AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(response.getRefreshToken(), "password");
        Assert.assertEquals(200, refreshResponse.getStatusCode());
    }

    @Test
    public void testRealmOnlyWithUserCompositeAppRole() throws Exception {
        oauth.realm("test");
        oauth.clientId("REALM_ROLE_1_APPLICATION");
        oauth.doLogin("REALM_COMPOSITE_1_USER", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        Assert.assertEquals(200, response.getStatusCode());

        Assert.assertEquals("bearer", response.getTokenType());

        AccessToken token = oauth.verifyToken(response.getAccessToken());

        Assert.assertEquals(getUserId("REALM_COMPOSITE_1_USER"), token.getSubject());

        Assert.assertEquals(1, token.getRealmAccess().getRoles().size());
        Assert.assertTrue(token.getRealmAccess().isUserInRole("REALM_ROLE_1"));

        AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(response.getRefreshToken(), "password");
        Assert.assertEquals(200, refreshResponse.getStatusCode());
    }

    @Test
    public void testRealmOnlyWithUserRoleAppComposite() throws Exception {
        oauth.realm("test");
        oauth.clientId("REALM_COMPOSITE_1_APPLICATION");
        oauth.doLogin("REALM_ROLE_1_USER", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        Assert.assertEquals(200, response.getStatusCode());

        Assert.assertEquals("bearer", response.getTokenType());

        AccessToken token = oauth.verifyToken(response.getAccessToken());

        Assert.assertEquals(getUserId("REALM_ROLE_1_USER"), token.getSubject());

        Assert.assertEquals(1, token.getRealmAccess().getRoles().size());
        Assert.assertTrue(token.getRealmAccess().isUserInRole("REALM_ROLE_1"));

        AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(response.getRefreshToken(), "password");
        Assert.assertEquals(200, refreshResponse.getStatusCode());
    }

    
    // KEYCLOAK-4274
    @Test
    public void testRecursiveComposites() throws Exception {
        // This will create recursive composite mappings between "REALM_COMPOSITE_1" and "REALM_ROLE_1"
        RoleRepresentation realmComposite1 = testRealm().roles().get("REALM_COMPOSITE_1").toRepresentation();
        testRealm().roles().get("REALM_ROLE_1").addComposites(Collections.singletonList(realmComposite1));

        UserResource userResource = ApiUtil.findUserByUsernameId(testRealm(), "REALM_COMPOSITE_1_USER");
        List<RoleRepresentation> realmRoles = userResource.roles().realmLevel().listEffective();
        Assert.assertNames(realmRoles, "REALM_COMPOSITE_1", "REALM_ROLE_1");

        userResource = ApiUtil.findUserByUsernameId(testRealm(), "REALM_ROLE_1_USER");
        realmRoles = userResource.roles().realmLevel().listEffective();
        Assert.assertNames(realmRoles, "REALM_COMPOSITE_1", "REALM_ROLE_1");

        // Revert
        testRealm().roles().get("REALM_ROLE_1").deleteComposites(Collections.singletonList(realmComposite1));
    }

}
