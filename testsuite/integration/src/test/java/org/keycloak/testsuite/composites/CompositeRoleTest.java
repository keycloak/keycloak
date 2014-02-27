/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.testsuite.composites;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.ApplicationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.ApplicationServlet;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.OAuthClient.AccessTokenResponse;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import java.security.PublicKey;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class CompositeRoleTest {

    public static PublicKey realmPublicKey;
    @ClassRule
    public static AbstractKeycloakRule keycloakRule = new AbstractKeycloakRule(){
        @Override
        protected void configure(RealmManager manager, RealmModel adminRealm) {
            RealmModel realm = manager.createRealm("Test");
            manager.generateRealmKeys(realm);
            realmPublicKey = realm.getPublicKey();
            realm.setCentralLoginLifespan(3000);
            realm.setAccessTokenLifespan(10000);
            realm.setRefreshTokenLifespan(10000);
            realm.setAccessCodeLifespanUserAction(1000);
            realm.setAccessCodeLifespan(1000);
            realm.setSslNotRequired(true);
            realm.setEnabled(true);
            realm.addRequiredCredential(UserCredentialModel.PASSWORD);
            final RoleModel realmRole1 = realm.addRole("REALM_ROLE_1");
            final RoleModel realmRole2 = realm.addRole("REALM_ROLE_2");
            final RoleModel realmRole3 = realm.addRole("REALM_ROLE_3");
            final RoleModel realmComposite1 = realm.addRole("REALM_COMPOSITE_1");
            realmComposite1.addCompositeRole(realmRole1);

            final UserModel realmComposite1User = realm.addUser("REALM_COMPOSITE_1_USER");
            realmComposite1User.setEnabled(true);
            realm.updateCredential(realmComposite1User, UserCredentialModel.password("password"));
            realm.grantRole(realmComposite1User, realmComposite1);

            final UserModel realmRole1User = realm.addUser("REALM_ROLE_1_USER");
            realmRole1User.setEnabled(true);
            realm.updateCredential(realmRole1User, UserCredentialModel.password("password"));
            realm.grantRole(realmRole1User, realmRole1);

            final ApplicationModel realmComposite1Application = new ApplicationManager(manager).createApplication(realm, "REALM_COMPOSITE_1_APPLICATION");
            realmComposite1Application.setEnabled(true);
            realmComposite1Application.addScope(realmComposite1);
            realmComposite1Application.setBaseUrl("http://localhost:8081/app");
            realmComposite1Application.setManagementUrl("http://localhost:8081/app/logout");
            realmComposite1Application.setSecret("password");

            final ApplicationModel realmRole1Application = new ApplicationManager(manager).createApplication(realm, "REALM_ROLE_1_APPLICATION");
            realmRole1Application.setEnabled(true);
            realmRole1Application.addScope(realmRole1);
            realmRole1Application.setBaseUrl("http://localhost:8081/app");
            realmRole1Application.setManagementUrl("http://localhost:8081/app/logout");
            realmRole1Application.setSecret("password");


            final ApplicationModel appRoleApplication = new ApplicationManager(manager).createApplication(realm, "APP_ROLE_APPLICATION");
            appRoleApplication.setEnabled(true);
            appRoleApplication.setBaseUrl("http://localhost:8081/app");
            appRoleApplication.setManagementUrl("http://localhost:8081/app/logout");
            appRoleApplication.setSecret("password");
            final RoleModel appRole1 = appRoleApplication.addRole("APP_ROLE_1");
            final RoleModel appRole2 = appRoleApplication.addRole("APP_ROLE_2");

            final RoleModel realmAppCompositeRole = realm.addRole("REALM_APP_COMPOSITE_ROLE");
            realmAppCompositeRole.addCompositeRole(appRole1);

            final UserModel realmAppCompositeUser = realm.addUser("REALM_APP_COMPOSITE_USER");
            realmAppCompositeUser.setEnabled(true);
            realm.updateCredential(realmAppCompositeUser, UserCredentialModel.password("password"));
            realm.grantRole(realmAppCompositeUser, realmAppCompositeRole);

            final UserModel realmAppRoleUser = realm.addUser("REALM_APP_ROLE_USER");
            realmAppRoleUser.setEnabled(true);
            realm.updateCredential(realmAppRoleUser, UserCredentialModel.password("password"));
            realm.grantRole(realmAppRoleUser, appRole2);

            final ApplicationModel appCompositeApplication = new ApplicationManager(manager).createApplication(realm, "APP_COMPOSITE_APPLICATION");
            appCompositeApplication.setEnabled(true);
            appCompositeApplication.setBaseUrl("http://localhost:8081/app");
            appCompositeApplication.setManagementUrl("http://localhost:8081/app/logout");
            appCompositeApplication.setSecret("password");
            final RoleModel appCompositeRole = appCompositeApplication.addRole("APP_COMPOSITE_ROLE");
            appCompositeApplication.addScope(appRole2);
            appCompositeRole.addCompositeRole(realmRole1);
            appCompositeRole.addCompositeRole(realmRole2);
            appCompositeRole.addCompositeRole(realmRole3);
            appCompositeRole.addCompositeRole(appRole1);

            final UserModel appCompositeUser = realm.addUser("APP_COMPOSITE_USER");
            appCompositeUser.setEnabled(true);
            realm.updateCredential(appCompositeUser, UserCredentialModel.password("password"));
            realm.grantRole(appCompositeUser, realmAppCompositeRole);
            realm.grantRole(appCompositeUser, realmComposite1);

            deployServlet("app", "/app", ApplicationServlet.class);

        }
    };

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected LoginPage loginPage;

    @Test
    public void testAppCompositeUser() throws Exception {
        oauth.realm("Test");
        oauth.realmPublicKey(realmPublicKey);
        oauth.clientId("APP_COMPOSITE_APPLICATION");
        oauth.doLogin("APP_COMPOSITE_USER", "password");

        String code = oauth.getCurrentQuery().get("code");
        AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        Assert.assertEquals(200, response.getStatusCode());

        Assert.assertEquals("bearer", response.getTokenType());

        AccessToken token = oauth.verifyToken(response.getAccessToken());

        Assert.assertEquals(keycloakRule.getUser("Test", "APP_COMPOSITE_USER").getId(), token.getSubject());

        Assert.assertEquals(1, token.getResourceAccess("APP_ROLE_APPLICATION").getRoles().size());
        Assert.assertEquals(1, token.getRealmAccess().getRoles().size());
        Assert.assertTrue(token.getResourceAccess("APP_ROLE_APPLICATION").isUserInRole("APP_ROLE_1"));
        Assert.assertTrue(token.getRealmAccess().isUserInRole("REALM_ROLE_1"));
    }


    @Test
    public void testRealmAppCompositeUser() throws Exception {
        oauth.realm("Test");
        oauth.realmPublicKey(realmPublicKey);
        oauth.clientId("APP_ROLE_APPLICATION");
        oauth.doLogin("REALM_APP_COMPOSITE_USER", "password");

        String code = oauth.getCurrentQuery().get("code");
        AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        Assert.assertEquals(200, response.getStatusCode());

        Assert.assertEquals("bearer", response.getTokenType());

        AccessToken token = oauth.verifyToken(response.getAccessToken());

        Assert.assertEquals(keycloakRule.getUser("Test", "REALM_APP_COMPOSITE_USER").getId(), token.getSubject());

        Assert.assertEquals(1, token.getResourceAccess("APP_ROLE_APPLICATION").getRoles().size());
        Assert.assertTrue(token.getResourceAccess("APP_ROLE_APPLICATION").isUserInRole("APP_ROLE_1"));
    }



    @Test
    public void testRealmOnlyWithUserCompositeAppComposite() throws Exception {
        oauth.realm("Test");
        oauth.realmPublicKey(realmPublicKey);
        oauth.clientId("REALM_COMPOSITE_1_APPLICATION");
        oauth.doLogin("REALM_COMPOSITE_1_USER", "password");

        String code = oauth.getCurrentQuery().get("code");
        AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        Assert.assertEquals(200, response.getStatusCode());

        Assert.assertEquals("bearer", response.getTokenType());

        AccessToken token = oauth.verifyToken(response.getAccessToken());

        Assert.assertEquals(keycloakRule.getUser("Test", "REALM_COMPOSITE_1_USER").getId(), token.getSubject());

        Assert.assertEquals(2, token.getRealmAccess().getRoles().size());
        Assert.assertTrue(token.getRealmAccess().isUserInRole("REALM_COMPOSITE_1"));
        Assert.assertTrue(token.getRealmAccess().isUserInRole("REALM_ROLE_1"));
    }

    @Test
    public void testRealmOnlyWithUserCompositeAppRole() throws Exception {
        oauth.realm("Test");
        oauth.realmPublicKey(realmPublicKey);
        oauth.clientId("REALM_ROLE_1_APPLICATION");
        oauth.doLogin("REALM_COMPOSITE_1_USER", "password");

        String code = oauth.getCurrentQuery().get("code");
        AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        Assert.assertEquals(200, response.getStatusCode());

        Assert.assertEquals("bearer", response.getTokenType());

        AccessToken token = oauth.verifyToken(response.getAccessToken());

        Assert.assertEquals(keycloakRule.getUser("Test", "REALM_COMPOSITE_1_USER").getId(), token.getSubject());

        Assert.assertEquals(1, token.getRealmAccess().getRoles().size());
        Assert.assertTrue(token.getRealmAccess().isUserInRole("REALM_ROLE_1"));
    }

    @Test
    public void testRealmOnlyWithUserRoleAppComposite() throws Exception {
        oauth.realm("Test");
        oauth.realmPublicKey(realmPublicKey);
        oauth.clientId("REALM_COMPOSITE_1_APPLICATION");
        oauth.doLogin("REALM_ROLE_1_USER", "password");

        String code = oauth.getCurrentQuery().get("code");
        AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        Assert.assertEquals(200, response.getStatusCode());

        Assert.assertEquals("bearer", response.getTokenType());

        AccessToken token = oauth.verifyToken(response.getAccessToken());

        Assert.assertEquals(keycloakRule.getUser("Test", "REALM_ROLE_1_USER").getId(), token.getSubject());

        Assert.assertEquals(1, token.getRealmAccess().getRoles().size());
        Assert.assertTrue(token.getRealmAccess().isUserInRole("REALM_ROLE_1"));
    }

}
