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
import org.keycloak.representations.SkeletonKeyToken;
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
            realm.setTokenLifespan(10000);
            realm.setSslNotRequired(true);
            realm.setEnabled(true);
            realm.addRequiredResourceCredential(UserCredentialModel.PASSWORD);
            realm.addRequiredOAuthClientCredential(UserCredentialModel.PASSWORD);
            realm.addRequiredCredential(UserCredentialModel.PASSWORD);
            final RoleModel realmRole1 = realm.addRole("REALM_ROLE_1");
            final RoleModel realmRole2 = realm.addRole("REALM_ROLE_2");
            final RoleModel realmRole3 = realm.addRole("REALM_ROLE_3");
            final RoleModel realmComposite1 = realm.addRole("REALM_COMPOSITE_1");
            realmComposite1.setComposite(true);
            realmComposite1.addCompositeRole(realmRole1);

            final UserModel realmComposite1User = realm.addUser("REALM_COMPOSITE_1_USER");
            realmComposite1User.setEnabled(true);
            realm.updateCredential(realmComposite1User, UserCredentialModel.password("password"));
            realm.grantRole(realmComposite1User, realmComposite1);

            final ApplicationModel realmComposite1Application = new ApplicationManager(manager).createApplication(realm, "REALM_COMPOSITE_1_APPLICATION");
            realmComposite1Application.setEnabled(true);
            realmComposite1Application.addScope(realmComposite1);
            realmComposite1Application.setBaseUrl("http://localhost:8081/app");
            realmComposite1Application.setManagementUrl("http://localhost:8081/app/logout");
            realm.updateCredential(realmComposite1Application.getApplicationUser(), UserCredentialModel.password("password"));

            final ApplicationModel realmRole1Application = new ApplicationManager(manager).createApplication(realm, "REALM_ROLE_1_APPLICATION");
            realmRole1Application.setEnabled(true);
            realmRole1Application.addScope(realmRole1);
            realmRole1Application.setBaseUrl("http://localhost:8081/app");
            realmRole1Application.setManagementUrl("http://localhost:8081/app/logout");
            realm.updateCredential(realmRole1Application.getApplicationUser(), UserCredentialModel.password("password"));



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
    public void testRealmOnlyCompositeWithUserCompositeAppComposite() throws Exception {
        oauth.realm("Test");
        oauth.realmPublicKey(realmPublicKey);
        oauth.clientId("REALM_COMPOSITE_1_APPLICATION");
        oauth.doLogin("REALM_COMPOSITE_1_USER", "password");

        String code = oauth.getCurrentQuery().get("code");
        AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        Assert.assertEquals(200, response.getStatusCode());

        Assert.assertEquals("bearer", response.getTokenType());

        SkeletonKeyToken token = oauth.verifyToken(response.getAccessToken());

        Assert.assertEquals("REALM_COMPOSITE_1_USER", token.getSubject());

        Assert.assertEquals(2, token.getRealmAccess().getRoles().size());
        Assert.assertTrue(token.getRealmAccess().isUserInRole("REALM_COMPOSITE_1"));
        Assert.assertTrue(token.getRealmAccess().isUserInRole("REALM_ROLE_1"));
    }

    @Test
    public void testRealmOnlyCompositeWithUserCompositeAppRole() throws Exception {
        oauth.realm("Test");
        oauth.realmPublicKey(realmPublicKey);
        oauth.clientId("REALM_ROLE_1_APPLICATION");
        oauth.doLogin("REALM_COMPOSITE_1_USER", "password");

        String code = oauth.getCurrentQuery().get("code");
        AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        Assert.assertEquals(200, response.getStatusCode());

        Assert.assertEquals("bearer", response.getTokenType());

        SkeletonKeyToken token = oauth.verifyToken(response.getAccessToken());

        Assert.assertEquals("REALM_COMPOSITE_1_USER", token.getSubject());

        Assert.assertEquals(1, token.getRealmAccess().getRoles().size());
        Assert.assertTrue(token.getRealmAccess().isUserInRole("REALM_ROLE_1"));
    }


}
