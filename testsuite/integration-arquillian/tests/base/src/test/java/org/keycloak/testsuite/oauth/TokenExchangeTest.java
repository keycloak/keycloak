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

package org.keycloak.testsuite.oauth;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.FineGrainAdminUnitTest;
import org.keycloak.testsuite.runonserver.RunOnServerDeployment;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.RealmManager;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.UserManager;
import org.keycloak.util.JsonSerialization;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TokenExchangeTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Deployment
    public static WebArchive deploy() {
        return RunOnServerDeployment.create(TokenExchangeTest.class);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealmRep = new RealmRepresentation();
        testRealmRep.setId(TEST);
        testRealmRep.setRealm(TEST);
        testRealmRep.setEnabled(true);
        testRealms.add(testRealmRep);
    }

    public static void setupRealm(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(TEST);
        RoleModel realmExchangeable = AdminPermissions.management(session, realm).getRealmManagementClient().addRole(OAuth2Constants.TOKEN_EXCHANGEABLE);

        RoleModel exampleRole = realm.addRole("example");

        ClientModel target = realm.addClient("target");
        target.setDirectAccessGrantsEnabled(true);
        target.setEnabled(true);
        target.setSecret("secret");
        target.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        target.setFullScopeAllowed(false);
        target.addScopeMapping(exampleRole);
        RoleModel targetExchangeable = target.addRole(OAuth2Constants.TOKEN_EXCHANGEABLE);

        target = realm.addClient("realm-exchanger");
        target.setClientId("realm-exchanger");
        target.setDirectAccessGrantsEnabled(true);
        target.setEnabled(true);
        target.setSecret("secret");
        target.setServiceAccountsEnabled(true);
        target.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        target.setFullScopeAllowed(false);
        new org.keycloak.services.managers.ClientManager(new org.keycloak.services.managers.RealmManager(session)).enableServiceAccount(target);
        session.users().getServiceAccount(target).grantRole(realmExchangeable);

        target = realm.addClient("client-exchanger");
        target.setClientId("client-exchanger");
        target.setDirectAccessGrantsEnabled(true);
        target.setEnabled(true);
        target.setSecret("secret");
        target.setServiceAccountsEnabled(true);
        target.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        target.setFullScopeAllowed(false);
        new org.keycloak.services.managers.ClientManager(new org.keycloak.services.managers.RealmManager(session)).enableServiceAccount(target);
        session.users().getServiceAccount(target).grantRole(targetExchangeable);

        target = realm.addClient("account-not-allowed");
        target.setClientId("account-not-allowed");
        target.setDirectAccessGrantsEnabled(true);
        target.setEnabled(true);
        target.setSecret("secret");
        target.setServiceAccountsEnabled(true);
        target.setFullScopeAllowed(false);
        target.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        new org.keycloak.services.managers.ClientManager(new org.keycloak.services.managers.RealmManager(session)).enableServiceAccount(target);

        target = realm.addClient("no-account");
        target.setClientId("no-account");
        target.setDirectAccessGrantsEnabled(true);
        target.setEnabled(true);
        target.setSecret("secret");
        target.setServiceAccountsEnabled(true);
        target.setFullScopeAllowed(false);
        target.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

        UserModel user = session.users().addUser(realm, "user");
        user.setEnabled(true);
        session.userCredentialManager().updateCredential(realm, user, UserCredentialModel.password("password"));
        user.grantRole(exampleRole);

    }

    @Override
    protected boolean isImportAfterEachMethod() {
        return true;
    }


    @Test
    public void testExchange() throws Exception {
        testingClient.server().run(TokenExchangeTest::setupRealm);

        oauth.realm(TEST);
        oauth.clientId("realm-exchanger");

        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("secret", "user", "password");
        String accessToken = response.getAccessToken();

        response = oauth.doTokenExchange(TEST,accessToken, "target", "realm-exchanger", "secret");

        String exchangedTokenString = response.getAccessToken();
        TokenVerifier<AccessToken> verifier = TokenVerifier.create(exchangedTokenString, AccessToken.class);
        AccessToken exchangedToken = verifier.parse().getToken();
        Assert.assertEquals(exchangedToken.getPreferredUsername(), "user");
        Assert.assertTrue(exchangedToken.getRealmAccess().isUserInRole("example"));


    }
}
