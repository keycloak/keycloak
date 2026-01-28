/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.oidc;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.ProtocolMapperUtil;
import org.keycloak.testsuite.util.UserBuilder;

import org.junit.Before;
import org.junit.Test;

/**
 * Test for the 'aud' claim in tokens
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AudienceTest extends AbstractOIDCScopeTest {

    private static String userId;


    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        // Create service client with some client role
        ClientRepresentation client1 = new ClientRepresentation();
        client1.setClientId("service-client");
        client1.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        client1.setBearerOnly(true);
        client1.setBaseUrl("http://foo/service-client");
        testRealm.getClients().add(client1);

        RoleRepresentation role1 = new RoleRepresentation();
        role1.setName("role1");
        testRealm.getRoles().getClient().put("service-client", Arrays.asList(role1));

        // Disable FullScopeAllowed for the 'test-app' client
        ClientRepresentation testApp = testRealm.getClients().stream().filter((ClientRepresentation client) -> {
            return "test-app".equals(client.getClientId());
        }).findFirst().get();

        testApp.setFullScopeAllowed(false);

        // Create sample user
        UserRepresentation user = UserBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .username("john")
                .enabled(true)
                .email("john@email.cz")
                .firstName("John")
                .lastName("Doe")
                .password("password")
                .role("account", "manage-account")
                .role("account", "view-profile")
                .role("service-client", "role1")
                .build();
        testRealm.getUsers().add(user);
    }

    @Override
    public void importTestRealms() {
        super.importTestRealms();
        userId = adminClient.realm("test").users().search("john", true).get(0).getId();
    }

    @Before
    public void beforeTest() {
        // Check if already exists
        ClientScopeResource clientScopeRes = ApiUtil.findClientScopeByName(testRealm(), "audience-scope");
        if (clientScopeRes != null) {
            return;
        }

        // Create client scope 'audience-scope' and add as optional scope to the 'test-app' client
        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("audience-scope");
        clientScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Response resp = testRealm().clientScopes().create(clientScope);
        String clientScopeId = ApiUtil.getCreatedId(resp);
        resp.close();

        ClientResource client = ApiUtil.findClientByClientId(testRealm(), "test-app");
        client.addOptionalClientScope(clientScopeId);
    }


    @Test
    public void testAudienceProtocolMapperWithClientAudience() throws Exception {
        // Add audience protocol mapper to the clientScope "audience-scope"
        ProtocolMapperRepresentation audienceMapper = ProtocolMapperUtil.createAudienceMapper("audience mapper", "service-client",
                null, true, false, true);
        ClientScopeResource clientScope = ApiUtil.findClientScopeByName(testRealm(), "audience-scope");
        Response resp = clientScope.getProtocolMappers().createMapper(audienceMapper);
        String mapperId = ApiUtil.getCreatedId(resp);
        resp.close();

        // Login and check audiences in the token (just accessToken contains it)
        oauth.scope("openid audience-scope");
        oauth.doLogin("john", "password");
        EventRepresentation loginEvent = events.expectLogin()
                .user(userId)
                .assertEvent();
        Tokens tokens = sendTokenRequest(loginEvent, userId, "openid profile email audience-scope", "test-app");

        assertAudiences(tokens.accessToken, "service-client");
        assertAudiences(tokens.idToken, "test-app");

        // Revert
        clientScope.getProtocolMappers().delete(mapperId);
    }


    @Test
    public void testAudienceProtocolMapperWithCustomAudience() throws Exception {
        // Add audience protocol mapper to the clientScope "audience-scope"
        ProtocolMapperRepresentation audienceMapper = ProtocolMapperUtil.createAudienceMapper("audience mapper 1", null,
                "http://host/service/ctx1", true, false, true);
        ClientScopeResource clientScope = ApiUtil.findClientScopeByName(testRealm(), "audience-scope");
        Response resp = clientScope.getProtocolMappers().createMapper(audienceMapper);
        String mapper1Id = ApiUtil.getCreatedId(resp);
        resp.close();

        audienceMapper = ProtocolMapperUtil.createAudienceMapper("audience mapper 2", null,
                "http://host/service/ctx2", true, true, true);
        resp = clientScope.getProtocolMappers().createMapper(audienceMapper);
        String mapper2Id = ApiUtil.getCreatedId(resp);
        resp.close();

        // Login and check audiences in the token
        oauth.scope("openid audience-scope");
        oauth.doLogin("john", "password");
        EventRepresentation loginEvent = events.expectLogin()
                .user(userId)
                .assertEvent();
        Tokens tokens = sendTokenRequest(loginEvent, userId, "openid profile email audience-scope", "test-app");

        assertAudiences(tokens.accessToken, "http://host/service/ctx1", "http://host/service/ctx2");
        assertAudiences(tokens.idToken, "test-app", "http://host/service/ctx2");

        // Revert
        clientScope.getProtocolMappers().delete(mapper1Id);
        clientScope.getProtocolMappers().delete(mapper2Id);
    }


    private void assertAudiences(JsonWebToken token, String... expectedAudience) {
        Collection<String> audiences = token.getAudience() == null ? Collections.emptyList() : Arrays.asList(token.getAudience());
        Collection<String> expectedAudiences = Arrays.asList(expectedAudience);
        Assert.assertTrue("Not matched. expectedAudiences: " + expectedAudiences + ", audiences: " + audiences,
                expectedAudiences.containsAll(audiences) && audiences.containsAll(expectedAudiences));
    }
}
