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

import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.UserConsentManager;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.pages.AccountApplicationsPage;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.ProtocolMapperUtil;
import org.openqa.selenium.By;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.admin.ApiUtil.findClientByClientId;

/**
 * @author <a href="mailto:dgozalob@redhat.com">Daniel Gozalo</a>
 */
@EnableFeature(value = Profile.Feature.DYNAMIC_SCOPES, skipRestart = true)
public class OAuthGrantDynamicScopesTest extends OAuthGrantTest {

    @Test
    public void dynamicScopeParamStoredAsUserProfileTest() {
        RealmResource appRealm = adminClient.realm(REALM_NAME);
        ClientResource thirdParty = findClientByClientId(appRealm, THIRD_PARTY_APP);

        // Create clientScope
        ClientScopeRepresentation scope = new ClientScopeRepresentation();
        scope.setName("bar-dynamic-scope");
        scope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        scope.setAttributes(new HashMap<String, String>() {{
            put(ClientScopeModel.IS_DYNAMIC_SCOPE, "true");
            put(ClientScopeModel.DYNAMIC_SCOPE_REGEXP, "bar-dynamic-scope:*");
        }});
        Response response = appRealm.clientScopes().create(scope);
        String dynamicFooScopeId = ApiUtil.getCreatedId(response);
        response.close();
        getCleanup().addClientScopeId(dynamicFooScopeId);

        // Add clientScope as optional to client
        thirdParty.addOptionalClientScope(dynamicFooScopeId);

        Optional<ClientRepresentation> clientRep = appRealm.clients().findByClientId(THIRD_PARTY_APP).stream().findAny();
        if (!clientRep.isPresent()) {
            Assert.fail();
        }

        // Assert clientScope not on grant screen when not requested
        oauth.clientId(THIRD_PARTY_APP);
        oauth.scope("bar-dynamic-scope:storedparam");
        oauth.doLogin("test-user@localhost", "password");
        grantPage.assertCurrent();
        List<String> grants = grantPage.getDisplayedGrants();
        Assert.assertTrue(grants.contains("bar-dynamic-scope: storedparam"));
        grantPage.accept();

        EventRepresentation loginEvent = events.expectLogin()
                .client(THIRD_PARTY_APP)
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();

        Optional<UserRepresentation> userRep = appRealm.users().searchByAttributes(String.format("%1s:%2s", String.format(UserModel.CONSENT_ATTR, clientRep.get().getId(), dynamicFooScopeId), "storedparam")).stream().findAny();
        if (!userRep.isPresent()) {
            Assert.fail();
        }

        oauth.openLogout();

        events.expectLogout(loginEvent.getSessionId()).assertEvent();

        // login again to check whether the Dynamic scope is NOT requested again.
        oauth.scope("bar-dynamic-scope:storedParam2");
        oauth.doLogin("test-user@localhost", "password");
        grantPage.assertCurrent();
        grantPage.accept();

        events.expectLogin()
                .client(THIRD_PARTY_APP)
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();

        userRep = appRealm.users().searchByAttributes(String.format("%1s:%2s", String.format(UserModel.CONSENT_ATTR, clientRep.get().getId(), dynamicFooScopeId), "storedparam")).stream().findAny();
        if (!userRep.isPresent()) {
            Assert.fail();
        }
        userRep = appRealm.users().searchByAttributes(String.format("%1s:%2s", String.format(UserModel.CONSENT_ATTR, clientRep.get().getId(), dynamicFooScopeId), "storedparam2")).stream().findAny();
        if (!userRep.isPresent()) {
            Assert.fail();
        }

        // Revoke
        accountAppsPage.open();
        accountAppsPage.revokeGrant(THIRD_PARTY_APP);
        events.expect(EventType.REVOKE_GRANT)
                .client("account").detail(Details.REVOKED_CLIENT, THIRD_PARTY_APP).assertEvent();

        UserRepresentation user = appRealm.users().search("test-user@localhost", true).stream().findAny().get();
        Assert.assertNull(user.getAttributes());

        // cleanup
        oauth.scope(null);
        thirdParty.removeOptionalClientScope(dynamicFooScopeId);
    }

    @Test
    public void staticScopeNotStoredInUserProfileTest() {
        RealmResource appRealm = adminClient.realm(REALM_NAME);
        ClientResource thirdParty = findClientByClientId(appRealm, THIRD_PARTY_APP);

        // Create clientScope
        ClientScopeRepresentation scope = new ClientScopeRepresentation();
        scope.setName("testscope");
        scope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Response response = appRealm.clientScopes().create(scope);
        String staticScopeId = ApiUtil.getCreatedId(response);
        response.close();
        getCleanup().addClientScopeId(staticScopeId);

        // Add clientScope as optional to client
        thirdParty.addDefaultClientScope(staticScopeId);

        Optional<ClientRepresentation> clientRep = appRealm.clients().findByClientId(THIRD_PARTY_APP).stream().findAny();
        if (!clientRep.isPresent()) {
            Assert.fail();
        }

        // Assert clientScope not on grant screen when not requested
        oauth.clientId(THIRD_PARTY_APP);
        oauth.scope("testscope");
        oauth.doLogin("test-user@localhost", "password");
        grantPage.assertCurrent();
        List<String> grants = grantPage.getDisplayedGrants();
        Assert.assertTrue(grants.contains("testscope"));
        grantPage.accept();

        events.expectLogin()
                .client(THIRD_PARTY_APP)
                .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                .assertEvent();


        UserRepresentation user = appRealm.users().search("test-user@localhost", true).stream().findAny().get();
        Assert.assertNull(user.getAttributes());

        // cleanup
        oauth.scope(null);
        thirdParty.removeOptionalClientScope(staticScopeId);
    }

}
