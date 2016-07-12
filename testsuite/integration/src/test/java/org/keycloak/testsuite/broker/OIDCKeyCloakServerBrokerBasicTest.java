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

package org.keycloak.testsuite.broker;

import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.Constants;
import org.keycloak.testsuite.pages.AccountApplicationsPage;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.KeycloakServer;
import org.keycloak.util.JsonSerialization;
import org.openqa.selenium.NoSuchElementException;

import java.io.IOException;

import javax.ws.rs.core.UriBuilder;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author pedroigor
 */
public class OIDCKeyCloakServerBrokerBasicTest extends AbstractKeycloakIdentityProviderTest {

    private static final int PORT = 8082;

    @ClassRule
    public static AbstractKeycloakRule samlServerRule = new AbstractKeycloakRule() {

        @Override
        protected void configureServer(KeycloakServer server) {
            server.getConfig().setPort(PORT);
        }

        @Override
        protected void configure(KeycloakSession session, RealmManager manager, RealmModel adminRealm) {
            server.importRealm(getClass().getResourceAsStream("/broker-test/test-broker-realm-with-kc-oidc.json"));
        }

        @Override
        protected String[] getTestRealms() {
            return new String[] { "realm-with-oidc-identity-provider" };
        }
    };

    @WebResource
    protected AccountApplicationsPage accountApplicationsPage;

    @Override
    protected void revokeGrant() {
        String currentUrl = driver.getCurrentUrl();

        String accountAccessPath = Urls.accountApplicationsPage(UriBuilder.fromUri(Constants.AUTH_SERVER_ROOT).port(PORT).build(), "realm-with-oidc-identity-provider").toString();
        accountApplicationsPage.setPath(accountAccessPath);
        accountApplicationsPage.open();
        try {
            accountApplicationsPage.revokeGrant("broker-app");
        } catch (NoSuchElementException e) {
            System.err.println("Couldn't revoke broker-app application, maybe because it wasn't granted or user not logged");
        }

        driver.navigate().to(currentUrl);
    }

    @Override
    protected void doAfterProviderAuthentication() {
        // grant access to broker-app
        //grantPage.assertCurrent();
        //grantPage.accept();
    }

    @Override
    protected void doAssertTokenRetrieval(String pageSource) {
        try {
            AccessTokenResponse accessTokenResponse = JsonSerialization.readValue(pageSource, AccessTokenResponse.class);

            assertNotNull(accessTokenResponse.getToken());
            assertNotNull(accessTokenResponse.getIdToken());
        } catch (IOException e) {
            fail("Could not parse token.");
        }
    }

    @Override
    protected String getProviderId() {
        return "kc-oidc-idp";
    }

    @Test
    public void testSuccessfulAuthentication() {
        super.testSuccessfulAuthentication();
    }

    @Test
    public void testDisabledUser() {
        super.testDisabledUser();
    }

    @Test
    public void testTemporarilyDisabledUser() {
        super.testTemporarilyDisabledUser();
    }

    @Test
    public void testLogoutWorksWithTokenTimeout() {
        Keycloak keycloak = Keycloak.getInstance("http://localhost:8081/auth", "master", "admin", "admin", org.keycloak.models.Constants.ADMIN_CLI_CLIENT_ID);
        RealmRepresentation realm = keycloak.realm("realm-with-oidc-identity-provider").toRepresentation();
        assertNotNull(realm);
        int oldLifespan = realm.getAccessTokenLifespan();
        realm.setAccessTokenLifespan(1);
        keycloak.realm("realm-with-oidc-identity-provider").update(realm);
        IdentityProviderRepresentation idp =  keycloak.realm("realm-with-broker").identityProviders().get("kc-oidc-idp").toRepresentation();
        idp.getConfig().put("backchannelSupported", "false");
        keycloak.realm("realm-with-broker").identityProviders().get("kc-oidc-idp").update(idp);
        logoutTimeOffset = 2;
        super.testSuccessfulAuthentication();
        logoutTimeOffset = 0;
        realm.setAccessTokenLifespan(oldLifespan);
        keycloak.realm("realm-with-oidc-identity-provider").update(realm);
        idp.getConfig().put("backchannelSupported", "true");
        keycloak.realm("realm-with-broker").identityProviders().get("kc-oidc-idp").update(idp);
    }

    @Test
    public void testSuccessfulAuthenticationWithoutUpdateProfile() {
        super.testSuccessfulAuthenticationWithoutUpdateProfile();
    }

    @Test
    public void testSuccessfulAuthenticationWithoutUpdateProfile_emailNotProvided_emailVerifyEnabled() {
        super.testSuccessfulAuthenticationWithoutUpdateProfile_emailNotProvided_emailVerifyEnabled();
    }

    @Test
    public void testSuccessfulAuthenticationWithoutUpdateProfile_newUser_emailAsUsername() {
        super.testSuccessfulAuthenticationWithoutUpdateProfile_newUser_emailAsUsername();
    }

    @Test
    public void testTokenStorageAndRetrievalByApplication() {
        super.testTokenStorageAndRetrievalByApplication();
    }

    @Test
    public void testAccountManagementLinkIdentity() {
        super.testAccountManagementLinkIdentity();
    }

    @Test
    public void testWithLinkedFederationProvider() throws Exception {
        super.testWithLinkedFederationProvider();
    }

    @Test
    public void testAccountManagementLinkedIdentityAlreadyExists() {
        super.testAccountManagementLinkedIdentityAlreadyExists();
    }
}
