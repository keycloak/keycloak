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

package org.keycloak.tests.oidc;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientScopeBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.ProtocolMapperUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

/**
 * Test for the 'aud' claim in tokens
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@KeycloakIntegrationTest
public class AudienceTest extends AbstractOIDCScopeTest {

    @InjectRealm(config = AudienceRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectClient(config = AudienceClientConfig.class)
    ManagedClient client;

    @Test
    public void testAudienceProtocolMapperWithClientAudience() {
        // Add audience protocol mapper to the clientScope "audience-scope"
        ProtocolMapperRepresentation audienceMapper = ProtocolMapperUtil.createAudienceMapper("audience mapper", "service-client",
                null, true, false, true);
        ClientScopeResource clientScope = AdminApiUtil.findClientScopeByName(managedRealm.admin(), "audience-scope");
        Response resp = clientScope.getProtocolMappers().createMapper(audienceMapper);
        String mapperId = ApiUtil.getCreatedId(resp);
        resp.close();

        String userId = getUserId();
        // Login and check audiences in the token (just accessToken contains it)
        oauth.client(client.getClientId(), "password");
        oauth.scope("openid audience-scope");
        oauth.doLogin("john", "password");
        EventRepresentation loginEvent = EventAssertion.expectLoginSuccess(events.poll())
                .userId(userId).getEvent();
        Tokens tokens = sendTokenRequest(loginEvent, userId, "openid profile email audience-scope", client.getClientId());

        assertAudiences(tokens.accessToken, client.getClientId(), "service-client");
        assertAudiences(tokens.idToken, client.getClientId());

        // Revert
        clientScope.getProtocolMappers().delete(mapperId);
    }


    @Test
    public void testAudienceProtocolMapperWithCustomAudience() {
        // Add audience protocol mapper to the clientScope "audience-scope"
        ProtocolMapperRepresentation audienceMapper = ProtocolMapperUtil.createAudienceMapper("audience mapper 1", null,
                "http://host/service/ctx1", true, false, true);
        ClientScopeResource clientScope = AdminApiUtil.findClientScopeByName(managedRealm.admin(), "audience-scope");
        Response resp = clientScope.getProtocolMappers().createMapper(audienceMapper);
        String mapper1Id = ApiUtil.getCreatedId(resp);
        resp.close();

        audienceMapper = ProtocolMapperUtil.createAudienceMapper("audience mapper 2", null,
                "http://host/service/ctx2", true, true, true);
        resp = clientScope.getProtocolMappers().createMapper(audienceMapper);
        String mapper2Id = ApiUtil.getCreatedId(resp);
        resp.close();

        String userId = getUserId();
        // Login and check audiences in the token
        oauth.client(client.getClientId(), "password");
        oauth.scope("openid audience-scope");
        oauth.doLogin("john", "password");
        EventRepresentation loginEvent = EventAssertion.expectLoginSuccess(events.poll()).userId(userId).getEvent();

        Tokens tokens = sendTokenRequest(loginEvent, userId, "openid profile email audience-scope", client.getClientId());

        assertAudiences(tokens.accessToken, client.getClientId(), "http://host/service/ctx1", "http://host/service/ctx2");
        assertAudiences(tokens.idToken, client.getClientId(), "http://host/service/ctx2");

        // Revert
        clientScope.getProtocolMappers().delete(mapper1Id);
        clientScope.getProtocolMappers().delete(mapper2Id);
    }

    private String getUserId() {
        return AdminApiUtil.findUserByUsername(managedRealm.admin(), "john").getId();
    }


    private void assertAudiences(JsonWebToken token, String... expectedAudience) {
        Collection<String> audiences = token.getAudience() == null ? Collections.emptyList() : Arrays.asList(token.getAudience());
        Collection<String> expectedAudiences = Arrays.asList(expectedAudience);
        Assertions.assertTrue(expectedAudiences.containsAll(audiences) && audiences.containsAll(expectedAudiences),
                "Not matched. expectedAudiences: " + expectedAudiences + ", audiences: " + audiences);
    }

    private static class AudienceRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.name("test")
                    .clients(
                            ClientBuilder.create("service-client")
                                    .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                                    .bearerOnly(true)
                                    .baseUrl("http://foo/service-client")
                    )
                    .clientRoles("service-client", "role1")
                    .clientScopes(
                            ClientScopeBuilder.create()
                                    .name("profile")
                                    .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL),
                            ClientScopeBuilder.create()
                                    .name("email")
                                    .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL),
                            ClientScopeBuilder.create()
                                    .name("audience-scope")
                                    .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                    )
                    .users(
                            UserBuilder.create("john")
                                    .email("john@email.cz")
                                    .firstName("John")
                                    .lastName("Doe")
                                    .password("password")
                                    .clientRoles("account", "manage-account", "view-profile")
                                    .clientRoles("service-client", "role1")
                    );
        }
    }

    private static class AudienceClientConfig implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client
                    .clientId("audience-client")
                    .secret("password")
                    .redirectUris("*")
                    .fullScopeEnabled(false)
                    .defaultClientScopes("profile", "email")
                    .optionalClientScopes("audience-scope")
                    .protocolMappers(ProtocolMapperUtil.createAudienceMapper("audience-audience-client", null, "audience-client", true, false, true)
                    );
        }
    }
}
