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
package org.keycloak.testsuite.admin;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.Config;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ApplicationRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;
import org.keycloak.testutils.KeycloakServer;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tests Undertow Adapter
 *
 * @author <a href="mailto:bburke@redhat.com">Bill Burke</a>
 */
public class AdminAPITest {

    @ClassRule
    public static AbstractKeycloakRule keycloakRule = new AbstractKeycloakRule() {
        @Override
        protected void configure(KeycloakSession session, RealmManager manager, RealmModel adminRealm) {
        }
    };

    private static String createToken() {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmManager manager = new RealmManager(session);

            RealmModel adminRealm = manager.getRealm(Config.getAdminRealm());
            ApplicationModel adminConsole = adminRealm.getApplicationByName(Constants.ADMIN_CONSOLE_APPLICATION);
            TokenManager tm = new TokenManager();
            UserModel admin = session.users().getUserByUsername("admin", adminRealm);
            UserSessionModel userSession = session.sessions().createUserSession(adminRealm, admin, "admin", null, "form", false);
            AccessToken token = tm.createClientAccessToken(tm.getAccess(null, adminConsole, admin), adminRealm, adminConsole, admin, userSession);
            return tm.encodeToken(adminRealm, token);
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }

    protected void testCreateRealm(RealmRepresentation rep) {
        String token = createToken();
        final String authHeader = "Bearer " + token;
        ClientRequestFilter authFilter = new ClientRequestFilter() {
            @Override
            public void filter(ClientRequestContext requestContext) throws IOException {
                requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, authHeader);
            }
        };
        Client client = ClientBuilder.newBuilder().register(authFilter).build();
        UriBuilder authBase = UriBuilder.fromUri("http://localhost:8081/auth");
        WebTarget adminRealms = client.target(AdminRoot.realmsUrl(authBase));
        String realmName = rep.getRealm();
        WebTarget realmTarget = adminRealms.path(realmName);


        // create with just name, enabled, and id, just like admin console
        RealmRepresentation newRep = new RealmRepresentation();
        newRep.setRealm(rep.getRealm());
        newRep.setEnabled(rep.isEnabled());
        {
            Response response = adminRealms.request().post(Entity.json(newRep));
            Assert.assertEquals(201, response.getStatus());
            response.close();
        }
        // todo test with full import with initial create
        RealmRepresentation storedRealm = realmTarget.request().get(RealmRepresentation.class);
        checkRealmRep(newRep, storedRealm);

        Response updateResponse = realmTarget.request().put(Entity.json(rep));
        Assert.assertEquals(204, updateResponse.getStatus());
        updateResponse.close();
        storedRealm = realmTarget.request().get(RealmRepresentation.class);
        checkRealmRep(rep, storedRealm);

        if (rep.getApplications() != null) {
            WebTarget applicationsTarget = realmTarget.path("applications");
            for (ApplicationRepresentation appRep : rep.getApplications()) {
                ApplicationRepresentation newApp = new ApplicationRepresentation();
                if (appRep.getId() != null) newApp.setId(appRep.getId());
                newApp.setName(appRep.getName());
                if (appRep.getSecret() != null) {
                    newApp.setSecret(appRep.getSecret());
                }
                Response appCreateResponse = applicationsTarget.request().post(Entity.json(newApp));
                Assert.assertEquals(201, appCreateResponse.getStatus());
                appCreateResponse.close();
                WebTarget appTarget = applicationsTarget.path(appRep.getName());
                CredentialRepresentation cred = appTarget.path("client-secret").request().get(CredentialRepresentation.class);
                if (appRep.getSecret() != null) Assert.assertEquals(appRep.getSecret(), cred.getValue());
                CredentialRepresentation newCred = appTarget.path("client-secret").request().post(null, CredentialRepresentation.class);
                Assert.assertNotEquals(newCred.getValue(), cred.getValue());

                Response appUpdateResponse = appTarget.request().put(Entity.json(appRep));
                Assert.assertEquals(204, appUpdateResponse.getStatus());
                appUpdateResponse.close();


                ApplicationRepresentation storedApp = appTarget.request().get(ApplicationRepresentation.class);

                checkAppUpdate(appRep, storedApp);

            }
        }

        // delete realm
        {
            Response response = adminRealms.path(realmName).request().delete();
            Assert.assertEquals(204, response.getStatus());
            response.close();

        }
        client.close();
    }

    protected void checkAppUpdate(ApplicationRepresentation appRep, ApplicationRepresentation storedApp) {
        if (appRep.getName() != null) Assert.assertEquals(appRep.getName(), storedApp.getName());
        if (appRep.isEnabled() != null) Assert.assertEquals(appRep.isEnabled(), storedApp.isEnabled());
        if (appRep.isBearerOnly() != null) Assert.assertEquals(appRep.isBearerOnly(), storedApp.isBearerOnly());
        if (appRep.isPublicClient() != null) Assert.assertEquals(appRep.isPublicClient(), storedApp.isPublicClient());
        if (appRep.isFullScopeAllowed() != null) Assert.assertEquals(appRep.isFullScopeAllowed(), storedApp.isFullScopeAllowed());
        if (appRep.getAdminUrl() != null) Assert.assertEquals(appRep.getAdminUrl(), storedApp.getAdminUrl());
        if (appRep.getBaseUrl() != null) Assert.assertEquals(appRep.getBaseUrl(), storedApp.getBaseUrl());
        if (appRep.isSurrogateAuthRequired() != null) Assert.assertEquals(appRep.isSurrogateAuthRequired(), storedApp.isSurrogateAuthRequired());

        if (appRep.getNotBefore() != null) {
            Assert.assertEquals(appRep.getNotBefore(), storedApp.getNotBefore());
        }
        if (appRep.getDefaultRoles() != null) {
            Set<String> set = new HashSet<String>();
            for (String val : appRep.getDefaultRoles()) {
                set.add(val);
            }
            Set<String> storedSet = new HashSet<String>();
            for (String val : storedApp.getDefaultRoles()) {
                storedSet.add(val);
            }

            Assert.assertEquals(set, storedSet);
        }

        List<String> redirectUris = appRep.getRedirectUris();
        if (redirectUris != null) {
            Set<String> set = new HashSet<String>();
            for (String val : appRep.getRedirectUris()) {
                set.add(val);
            }
            Set<String> storedSet = new HashSet<String>();
            for (String val : storedApp.getRedirectUris()) {
                storedSet.add(val);
            }

            Assert.assertEquals(set, storedSet);
        }

        List<String> webOrigins = appRep.getWebOrigins();
        if (webOrigins != null) {
            Set<String> set = new HashSet<String>();
            for (String val : appRep.getWebOrigins()) {
                set.add(val);
            }
            Set<String> storedSet = new HashSet<String>();
            for (String val : storedApp.getWebOrigins()) {
                storedSet.add(val);
            }

            Assert.assertEquals(set, storedSet);
        }

        if (appRep.getClaims() != null) {
            Assert.assertEquals(appRep.getClaims(), storedApp.getClaims());
        }
    }

    protected void checkRealmRep(RealmRepresentation rep, RealmRepresentation storedRealm) {
        if (rep.getId() != null) {
            Assert.assertEquals(rep.getId(), storedRealm.getId());
        }
        if (rep.getRealm() != null) {
            Assert.assertEquals(rep.getRealm(), storedRealm.getRealm());
        }
        if (rep.isEnabled() != null) Assert.assertEquals(rep.isEnabled(), storedRealm.isEnabled());
        if (rep.isSocial() != null) Assert.assertEquals(rep.isSocial(), storedRealm.isSocial());
        if (rep.isBruteForceProtected() != null) Assert.assertEquals(rep.isBruteForceProtected(), storedRealm.isBruteForceProtected());
        if (rep.getMaxFailureWaitSeconds() != null) Assert.assertEquals(rep.getMaxFailureWaitSeconds(), storedRealm.getMaxFailureWaitSeconds());
        if (rep.getMinimumQuickLoginWaitSeconds() != null) Assert.assertEquals(rep.getMinimumQuickLoginWaitSeconds(), storedRealm.getMinimumQuickLoginWaitSeconds());
        if (rep.getWaitIncrementSeconds() != null) Assert.assertEquals(rep.getWaitIncrementSeconds(), storedRealm.getWaitIncrementSeconds());
        if (rep.getQuickLoginCheckMilliSeconds() != null) Assert.assertEquals(rep.getQuickLoginCheckMilliSeconds(), storedRealm.getQuickLoginCheckMilliSeconds());
        if (rep.getMaxDeltaTimeSeconds() != null) Assert.assertEquals(rep.getMaxDeltaTimeSeconds(), storedRealm.getMaxDeltaTimeSeconds());
        if (rep.getFailureFactor() != null) Assert.assertEquals(rep.getFailureFactor(), storedRealm.getFailureFactor());
        if (rep.isPasswordCredentialGrantAllowed() != null) Assert.assertEquals(rep.isPasswordCredentialGrantAllowed(), storedRealm.isPasswordCredentialGrantAllowed());
        if (rep.isRegistrationAllowed() != null) Assert.assertEquals(rep.isRegistrationAllowed(), storedRealm.isRegistrationAllowed());
        if (rep.isRememberMe() != null) Assert.assertEquals(rep.isRememberMe(), storedRealm.isRememberMe());
        if (rep.isVerifyEmail() != null) Assert.assertEquals(rep.isVerifyEmail(), storedRealm.isVerifyEmail());
        if (rep.isResetPasswordAllowed() != null) Assert.assertEquals(rep.isResetPasswordAllowed(), storedRealm.isResetPasswordAllowed());
        if (rep.isUpdateProfileOnInitialSocialLogin() != null)
            Assert.assertEquals(rep.isUpdateProfileOnInitialSocialLogin(), storedRealm.isUpdateProfileOnInitialSocialLogin());
        if (rep.getSslRequired() != null) Assert.assertEquals(rep.getSslRequired(), storedRealm.getSslRequired());
        if (rep.getAccessCodeLifespan() != null) Assert.assertEquals(rep.getAccessCodeLifespan(), storedRealm.getAccessCodeLifespan());
        if (rep.getAccessCodeLifespanUserAction() != null)
            Assert.assertEquals(rep.getAccessCodeLifespanUserAction(), storedRealm.getAccessCodeLifespanUserAction());
        if (rep.getNotBefore() != null) Assert.assertEquals(rep.getNotBefore(), storedRealm.getNotBefore());
        if (rep.getAccessTokenLifespan() != null) Assert.assertEquals(rep.getAccessTokenLifespan(), storedRealm.getAccessTokenLifespan());
        if (rep.getSsoSessionIdleTimeout() != null) Assert.assertEquals(rep.getSsoSessionIdleTimeout(), storedRealm.getSsoSessionIdleTimeout());
        if (rep.getSsoSessionMaxLifespan() != null) Assert.assertEquals(rep.getSsoSessionMaxLifespan(), storedRealm.getSsoSessionMaxLifespan());
        if (rep.getRequiredCredentials() != null) {
            Assert.assertNotNull(storedRealm.getRequiredCredentials());
            for (String cred : rep.getRequiredCredentials()) {
                Assert.assertTrue(storedRealm.getRequiredCredentials().contains(cred));
            }
        }
        if (rep.getLoginTheme() != null) Assert.assertEquals(rep.getLoginTheme(), storedRealm.getLoginTheme());
        if (rep.getAccountTheme() != null) Assert.assertEquals(rep.getAccountTheme(), storedRealm.getAccountTheme());
        if (rep.getAdminTheme() != null) Assert.assertEquals(rep.getAdminTheme(), storedRealm.getAdminTheme());
        if (rep.getEmailTheme() != null) Assert.assertEquals(rep.getEmailTheme(), storedRealm.getEmailTheme());

        if (rep.getPasswordPolicy() != null) Assert.assertEquals(rep.getPasswordPolicy(), storedRealm.getPasswordPolicy());

        if (rep.getDefaultRoles() != null) {
            Assert.assertNotNull(storedRealm.getDefaultRoles());
            for (String role : rep.getDefaultRoles()) {
                Assert.assertTrue(storedRealm.getDefaultRoles().contains(role));
            }
        }

        if (rep.getSmtpServer() != null) {
            Assert.assertEquals(rep.getSmtpServer(), storedRealm.getSmtpServer());
        }

        if (rep.getSocialProviders() != null) {
            Assert.assertEquals(rep.getSocialProviders(), storedRealm.getSocialProviders());
        }
        if (rep.getBrowserSecurityHeaders() != null) {
            Assert.assertEquals(rep.getBrowserSecurityHeaders(), storedRealm.getBrowserSecurityHeaders());
        }

    }

    protected void testCreateRealm(String path) {
        RealmRepresentation rep = KeycloakServer.loadJson(getClass().getResourceAsStream(path), RealmRepresentation.class);
        Assert.assertNotNull(rep);
        testCreateRealm(rep);
    }

    @Test
    public void testAdminApi() {
        RealmRepresentation empty = new RealmRepresentation();
        empty.setEnabled(true);
        empty.setRealm("empty");
        testCreateRealm(empty);
        testCreateRealm("/admin-test/testrealm.json");
    }

}
