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

package org.keycloak.testsuite.client;

import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.common.Profile;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.services.resources.admin.permissions.AdminPermissionManagement;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.RealmBuilder;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.keycloak.services.resources.admin.permissions.ClientPermissionManagement.ALLOW_REGEX_REDIRECT_URI_SCOPE;
import static org.keycloak.testsuite.AssertEvents.isCodeId;
import static org.keycloak.testsuite.AssertEvents.isUUID;
import static org.keycloak.testsuite.util.Matchers.statusCodeIs;

/**
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class ClientRedirectTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        RealmBuilder.edit(testRealm)
                .client(ClientBuilder.create().clientId("launchpad-test").baseUrl("").rootUrl("http://example.org/launchpad"))
                .client(ClientBuilder.create().clientId("dummy-test").baseUrl("/base-path").rootUrl("http://example.org/dummy"));
    }

    /**
     * Integration test for {@link org.keycloak.services.resources.RealmsResource#getRedirect(String, String)}.
     *
     * @throws Exception
     */
    @Test
    @DisableFeature(value = Profile.Feature.ACCOUNT2, skipRestart = true) // TODO remove this (KEYCLOAK-16228)
    public void testClientRedirectEndpoint() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        driver.get(getAuthServerRoot().toString() + "realms/test/clients/launchpad-test/redirect");
        assertEquals("http://example.org/launchpad", driver.getCurrentUrl());

        driver.get(getAuthServerRoot().toString() + "realms/test/clients/dummy-test/redirect");
        assertEquals("http://example.org/dummy/base-path", driver.getCurrentUrl());

        driver.get(getAuthServerRoot().toString() + "realms/test/clients/account/redirect");
        assertEquals(getAuthServerRoot().toString() + "realms/test/account/", driver.getCurrentUrl());
    }

    @Test
    public void testRedirectStatusCode() {
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        String token = oauth.doAccessTokenRequest(code, "password").getAccessToken();

        Client client = AdminClientUtil.createResteasyClient();
        String redirectUrl = getAuthServerRoot().toString() + "realms/test/clients/launchpad-test/redirect";
        Response response = client.target(redirectUrl).request().header(HttpHeaders.AUTHORIZATION, "Bearer " + token).get();
        assertEquals(303, response.getStatus());
        client.close();
    }

    // KEYCLOAK-7707
    @Test
    public void testRedirectToDisabledClientRedirectURI() throws Exception {
        log.debug("Creating disabled-client with redirect uri \"*\"");
        String clientId;
        try (Response create = adminClient.realm("test").clients().create(ClientBuilder.create().clientId("disabled-client").enabled(false).redirectUris("*").build())) {
            clientId = ApiUtil.getCreatedId(create);
            assertThat(create, statusCodeIs(Status.CREATED));
        }

        try {
            log.debug("log in");
            oauth.doLogin("test-user@localhost", "password");
            events.expectLogin().assertEvent();

            URI logout = KeycloakUriBuilder.fromUri(suiteContext.getAuthServerInfo().getBrowserContextRoot().toURI())
                    .path("auth" + ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH)
                    .queryParam(OIDCLoginProtocol.REDIRECT_URI_PARAM, "http://example.org/redirected")
                    .build("test");

            log.debug("log out using: " + logout.toURL());
            driver.navigate().to(logout.toURL());
            log.debug("Current URL: " + driver.getCurrentUrl());

            log.debug("check logout_error");
            events.expectLogoutError(OAuthErrorException.INVALID_REDIRECT_URI).assertEvent();
            assertThat(driver.getCurrentUrl(), is(not(equalTo("http://example.org/redirected"))));
        } finally {
            log.debug("removing disabled-client");
            adminClient.realm("test").clients().get(clientId).remove();
        }
    }

    // KEYCLOAK-18051
    @Test
    @EnableFeature(value = Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ, skipRestart = true)
    public void testRegexRedirectURI() throws URISyntaxException, MalformedURLException {
        String clientId = null;
        try {

            ClientsResource clientsResource = testRealm().clients();
            testingClient.server().run(ClientRedirectTest::setupAllowRegexRedirectUriPermission);
            clientId = clientsResource.findByClientId("regex-client").get(0).getId();

            ClientResource clientResource = clientsResource.get(clientId);
            ClientRepresentation clientRepresentation = clientResource.toRepresentation();

            clientRepresentation.setRedirectUris(Collections.singletonList("http://[a-zA-Z]+\\.[a-zA-Z]+/[a-zA-Z]+"));
            clientsResource.get(clientId).update(clientRepresentation);

            URI login = KeycloakUriBuilder.fromUri(suiteContext.getAuthServerInfo().getBrowserContextRoot().toURI())
                .path("auth" + ServiceUrlConstants.AUTH_PATH)
                .queryParam(OIDCLoginProtocol.CLIENT_ID_PARAM, clientRepresentation.getClientId())
                .queryParam(OIDCLoginProtocol.RESPONSE_TYPE_PARAM, "code").queryParam(OIDCLoginProtocol.SCOPE_PARAM, "openid")
                .queryParam(OIDCLoginProtocol.REDIRECT_URI_PARAM, "http://example.org/launchpad").build("test");

            driver.navigate().to(login.toURL());

            oauth.fillLoginForm("test-user@localhost", "password");

            events.expect(EventType.LOGIN).client(clientRepresentation.getClientId()).session(isUUID()).detail(Details.CODE_ID, isCodeId())
                .assertEvent();

            URI notAllowedRedirectUri = KeycloakUriBuilder.fromUri(suiteContext.getAuthServerInfo().getBrowserContextRoot().toURI())
                .path("auth" + ServiceUrlConstants.AUTH_PATH)
                .queryParam(OIDCLoginProtocol.CLIENT_ID_PARAM, clientRepresentation.getClientId())
                .queryParam(OIDCLoginProtocol.RESPONSE_TYPE_PARAM, "code").queryParam(OIDCLoginProtocol.SCOPE_PARAM, "openid")
                .queryParam(OIDCLoginProtocol.REDIRECT_URI_PARAM, "https://example.org/launchpad").build("test");

            driver.navigate().to(notAllowedRedirectUri.toURL());

            events.expect(EventType.LOGIN_ERROR).error(OAuthErrorException.INVALID_REDIRECT_URI).client((String) null).user((String) null);

        } finally {
            log.debug("removing disabled-client");
            if (clientId != null) {
                adminClient.realm("test").clients().get(clientId).remove();
            }
        }
    }

    public static void setupAllowRegexRedirectUriPermission(KeycloakSession session) {
        
        RealmModel realm = session.realms().getRealmByName("test");
        ClientModel client = session.clients().getClientByClientId(realm, "regex-client");
        // lazy init
        if (client != null) return;
        client = realm.addClient("regex-client");
        client.setSecret("secret");
        client.setPublicClient(false);
        client.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        client.setEnabled(true);
        client.setDirectAccessGrantsEnabled(true);

        ClientPolicyRepresentation clientPolicyRep = new ClientPolicyRepresentation();
        clientPolicyRep.setName("client-policy");
        clientPolicyRep.addClient(client.getId());

        AdminPermissionManagement management = AdminPermissions.management(session, realm);
        management.clients().setPermissionsEnabled(client,true);

        String resourceName = "client.resource." + client.getId();
        ResourceRepresentation resourceRepresentation = new ResourceRepresentation();
        resourceRepresentation.setName(resourceName);
        resourceRepresentation.addScope(ALLOW_REGEX_REDIRECT_URI_SCOPE);

        ResourceServer server = management.realmResourceServer();
        Policy clientPolicy = management.authz().getStoreFactory().getPolicyStore().create(clientPolicyRep, server);
        Resource resource = management.authz().getStoreFactory().getResourceStore().create(resourceName, server, "realm-management");

        management.clients().allowRegexRedirectUriPermission(client).addAssociatedPolicy(clientPolicy);
        management.clients().allowRegexRedirectUriPermission(client).addResource(resource);
        
    }
    
    
}
