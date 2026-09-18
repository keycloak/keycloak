/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.oauth;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.ClientSessionNoteMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.tests.common.TestRealmUserConfig;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies that {@link ClientSessionNoteMapper} maps a client session note into the access token response.
 */
@KeycloakIntegrationTest
public class ClientSessionNoteMapperTest {

    private static final String CLAIM_NAME = "client_session_note_startedAt";

    @InjectUser(config = TestRealmUserConfig.class)
    ManagedUser user;

    @InjectOAuthClient(config = ClientSessionNoteClientConfig.class)
    OAuthClient oauth;

    @Test
    public void mapsClientSessionNoteToAccessTokenResponse() {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);

        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getOtherClaims().get(CLAIM_NAME));
    }

    public static class ClientSessionNoteClientConfig implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
            mapper.setName("client-session-note-mapper");
            mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            mapper.setProtocolMapper(ClientSessionNoteMapper.PROVIDER_ID);
            Map<String, String> config = new HashMap<>();
            config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, CLAIM_NAME);
            config.put(ProtocolMapperUtils.CLIENT_SESSION_NOTE, AuthenticatedClientSessionModel.STARTED_AT_NOTE);
            config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN_RESPONSE, "true");
            mapper.setConfig(config);

            return client.clientId("test-app")
                    .secret("password")
                    .protocolMappers(mapper);
        }
    }
}
