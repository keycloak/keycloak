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
package org.keycloak.testsuite.oauth;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.constants.KerberosConstants;
import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.mappers.UserSessionNoteMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.AccountApplicationsPage;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:vrockai@redhat.com">Viliam Rockai</a>
 */
public class OAuthGrantTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule();

    @Rule
    public AssertEvents events = new AssertEvents(keycloakRule);

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected LoginPage loginPage;

    @WebResource
    protected OAuthGrantPage grantPage;

    @WebResource
    protected AccountApplicationsPage accountAppsPage;

    @WebResource
    protected AppPage appPage;

    private static String ROLE_USER = "Have User privileges";
    private static String ROLE_CUSTOMER = "Have Customer User privileges";

    @Test
    public void oauthGrantAcceptTest() {
        oauth.clientId("third-party");
        oauth.doLoginGrant("test-user@localhost", "password");

        grantPage.assertCurrent();
        Assert.assertTrue(driver.getPageSource().contains(ROLE_USER));
        Assert.assertTrue(driver.getPageSource().contains(ROLE_CUSTOMER));

        grantPage.accept();

        Assert.assertTrue(oauth.getCurrentQuery().containsKey(OAuth2Constants.CODE));

        Event loginEvent = events.expectLogin()
                .client("third-party")
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        String sessionId = loginEvent.getSessionId();

        OAuthClient.AccessTokenResponse accessToken = oauth.doAccessTokenRequest(oauth.getCurrentQuery().get(OAuth2Constants.CODE), "password");

        String tokenString = accessToken.getAccessToken();
        Assert.assertNotNull(tokenString);
        AccessToken token = oauth.verifyToken(tokenString);
        assertEquals(sessionId, token.getSessionState());

        AccessToken.Access realmAccess = token.getRealmAccess();
        assertEquals(1, realmAccess.getRoles().size());
        Assert.assertTrue(realmAccess.isUserInRole("user"));

        Map<String,AccessToken.Access> resourceAccess = token.getResourceAccess();
        assertEquals(1, resourceAccess.size());
        assertEquals(1, resourceAccess.get("test-app").getRoles().size());
        Assert.assertTrue(resourceAccess.get("test-app").isUserInRole("customer-user"));

        events.expectCodeToToken(codeId, loginEvent.getSessionId()).client("third-party").assertEvent();

        accountAppsPage.open();

        assertEquals(1, driver.findElements(By.id("revoke-third-party")).size());

        accountAppsPage.revokeGrant("third-party");

        events.expect(EventType.REVOKE_GRANT)
                .client("account").detail(Details.REVOKED_CLIENT, "third-party").assertEvent();

        assertEquals(0, driver.findElements(By.id("revoke-third-party")).size());
    }

    @Test
    public void oauthGrantCancelTest() {
        oauth.clientId("third-party");
        oauth.doLoginGrant("test-user@localhost", "password");

        grantPage.assertCurrent();
        Assert.assertTrue(driver.getPageSource().contains(ROLE_USER));
        Assert.assertTrue(driver.getPageSource().contains(ROLE_CUSTOMER));

        grantPage.cancel();

        Assert.assertTrue(oauth.getCurrentQuery().containsKey(OAuth2Constants.ERROR));
        assertEquals("access_denied", oauth.getCurrentQuery().get(OAuth2Constants.ERROR));

        events.expectLogin()
                .client("third-party")
                .error("rejected_by_user")
                .removeDetail(Details.CONSENT)
                .assertEvent();
    }

    @Test
    public void oauthGrantNotShownWhenAlreadyGranted() {
        // Grant permissions on grant screen
        oauth.clientId("third-party");
        oauth.doLoginGrant("test-user@localhost", "password");

        grantPage.assertCurrent();
        grantPage.accept();

        events.expectLogin()
                .client("third-party")
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();

        // Assert permissions granted on Account mgmt. applications page
        accountAppsPage.open();
        AccountApplicationsPage.AppEntry thirdPartyEntry = accountAppsPage.getApplications().get("third-party");
        Assert.assertTrue(thirdPartyEntry.getRolesGranted().contains(ROLE_USER));
        Assert.assertTrue(thirdPartyEntry.getRolesGranted().contains("Have Customer User privileges in test-app"));
        Assert.assertTrue(thirdPartyEntry.getProtocolMappersGranted().contains("Full name"));
        Assert.assertTrue(thirdPartyEntry.getProtocolMappersGranted().contains("Email"));

        // Open login form and assert grantPage not shown
        oauth.openLoginForm();
        appPage.assertCurrent();
        events.expectLogin()
                .detail(Details.AUTH_METHOD, OIDCLoginProtocol.LOGIN_PROTOCOL)
                .detail(Details.CONSENT, Details.CONSENT_VALUE_PERSISTED_CONSENT)
                .removeDetail(Details.USERNAME)
                .client("third-party").assertEvent();

        // Revoke grant in account mgmt.
        accountAppsPage.open();
        accountAppsPage.revokeGrant("third-party");

        events.expect(EventType.REVOKE_GRANT)
                .client("account").detail(Details.REVOKED_CLIENT, "third-party").assertEvent();

        // Open login form again and assert grant Page is shown
        oauth.openLoginForm();
        grantPage.assertCurrent();
        Assert.assertTrue(driver.getPageSource().contains(ROLE_USER));
        Assert.assertTrue(driver.getPageSource().contains(ROLE_CUSTOMER));
    }

    @Test
    public void oauthGrantAddAnotherRoleAndMapper() {
        // Grant permissions on grant screen
        oauth.clientId("third-party");
        oauth.doLoginGrant("test-user@localhost", "password");

        // Add new protocolMapper and role before showing grant page
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                ProtocolMapperModel protocolMapper = UserSessionNoteMapper.createClaimMapper(KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME,
                        KerberosConstants.GSS_DELEGATION_CREDENTIAL,
                        KerberosConstants.GSS_DELEGATION_CREDENTIAL, "String",
                        true, KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME,
                        true, false);

                ClientModel thirdPartyApp = appRealm.getClientByClientId("third-party");
                thirdPartyApp.addProtocolMapper(protocolMapper);

                RoleModel newRole = appRealm.addRole("new-role");
                thirdPartyApp.addScopeMapping(newRole);
                UserModel testUser = manager.getSession().users().getUserByUsername("test-user@localhost", appRealm);
                testUser.grantRole(newRole);
            }

        });

        // Confirm grant page
        grantPage.assertCurrent();
        grantPage.accept();
        events.expectLogin()
                .client("third-party")
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();

        // Assert new role and protocol mapper not in account mgmt.
        accountAppsPage.open();
        AccountApplicationsPage.AppEntry appEntry = accountAppsPage.getApplications().get("third-party");
        Assert.assertFalse(appEntry.getRolesGranted().contains("new-role"));
        Assert.assertFalse(appEntry.getProtocolMappersGranted().contains(KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME));

        // Show grant page another time. Just new role and protocol mapper are on the page
        oauth.openLoginForm();
        grantPage.assertCurrent();
        Assert.assertFalse(driver.getPageSource().contains(ROLE_USER));
        Assert.assertFalse(driver.getPageSource().contains("Full name"));
        Assert.assertTrue(driver.getPageSource().contains("new-role"));
        Assert.assertTrue(driver.getPageSource().contains(KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME));
        grantPage.accept();
        events.expectLogin()
                .client("third-party")
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();

        // Go to account mgmt. Everything is granted now
        accountAppsPage.open();
        appEntry = accountAppsPage.getApplications().get("third-party");
        Assert.assertTrue(appEntry.getRolesGranted().contains("new-role"));
        Assert.assertTrue(appEntry.getProtocolMappersGranted().contains(KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME));

        // Revoke
        accountAppsPage.revokeGrant("third-party");
        events.expect(EventType.REVOKE_GRANT)
                .client("account").detail(Details.REVOKED_CLIENT, "third-party").assertEvent();

        // Cleanup
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                ClientModel thirdPartyApp = appRealm.getClientByClientId("third-party");
                ProtocolMapperModel gssMapper = thirdPartyApp.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME);
                thirdPartyApp.removeProtocolMapper(gssMapper);

                RoleModel newRole = appRealm.getRole("new-role");
                appRealm.removeRole(newRole);
            }

        });
    }

}
