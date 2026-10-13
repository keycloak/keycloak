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

package org.keycloak.tests.admin.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.UserClientRoleMappingMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.oauth.DefaultOAuthClientConfiguration;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * @author <a href="mailto:daniel.lekberg@redpill-linpro.com">Daniel Lekberg</a>
 */
@KeycloakIntegrationTest
public class ClientRoleMapperTest {

    private static final String CLIENT_ROLE = "customer-user";
    private static final String CLAIM_NAME = "roles";

    @InjectOAuthClient(config = TestClient.class)
    OAuthClient oAuthClient;

    @InjectUser(config = TestUser.class)
    ManagedUser user;

    private String mapperId;

    @TestSetup
    public void grantClientRoleToUser() {
        oAuthClient.clientResource().roles().create(new RoleRepresentation(CLIENT_ROLE, "", false));

        String clientUuid = oAuthClient.clientResource().toRepresentation().getId();
        RoleRepresentation role = oAuthClient.clientResource().roles().get(CLIENT_ROLE).toRepresentation();
        user.admin().roles().clientLevel(clientUuid).add(List.of(role));
    }

    @AfterEach
    public void removeMapperAndUserSessions() {
        // Only remove the mapper added by the test, so the mappers installed by DefaultOAuthClientConfiguration survive
        oAuthClient.clientResource().getProtocolMappers().delete(mapperId);
        user.admin().logout();
    }

    static Stream<Arguments> prefixTestCases() {
        return Stream.of(
                arguments(null, List.of(CLIENT_ROLE)),
                arguments("", List.of(CLIENT_ROLE)),
                arguments("client_id::", List.of("client_id::" + CLIENT_ROLE)),
                arguments("${client_id}::", List.of("test-app::" + CLIENT_ROLE))
        );
    }

    @ParameterizedTest(name = "rolePrefix={0}")
    @MethodSource("prefixTestCases")
    public void testRoleMappingWithPrefix(String prefix, List<String> expectedRoles) {
        mapperId = ApiUtil.getCreatedId(oAuthClient.clientResource().getProtocolMappers().createMapper(roleMapper(prefix)));

        AuthorizationEndpointResponse login = oAuthClient.doLogin(user.getUsername(), user.getPassword());
        AccessTokenResponse response = oAuthClient.doAccessTokenRequest(login.getCode());
        IDToken idToken = oAuthClient.verifyIDToken(response.getIdToken());
        AccessToken accessToken = oAuthClient.verifyToken(response.getAccessToken());

        Assertions.assertNotNull(idToken);
        Assertions.assertNotNull(accessToken);
        Assertions.assertEquals(expectedRoles, idToken.getOtherClaims().get(CLAIM_NAME));
        Assertions.assertEquals(expectedRoles, accessToken.getOtherClaims().get(CLAIM_NAME));
    }

    private ProtocolMapperRepresentation roleMapper(String prefix) {
        Map<String, String> config = new HashMap<>();
        config.put(ProtocolMapperUtils.USER_MODEL_CLIENT_ROLE_MAPPING_ROLE_PREFIX, prefix);
        config.put(ProtocolMapperUtils.MULTIVALUED, "true");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_INTROSPECTION, "true");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "true");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, CLAIM_NAME);
        config.put(OIDCAttributeMapperHelper.JSON_TYPE, "String");

        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        mapper.setName("userClientRoleMappingMapperTest");
        mapper.setProtocolMapper(UserClientRoleMappingMapper.PROVIDER_ID);
        mapper.setConfig(config);
        return mapper;
    }

    public static class TestClient extends DefaultOAuthClientConfiguration {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return super
                    .configure(client)
                    .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                    .fullScopeEnabled(false);
        }
    }

    public static class TestUser implements UserConfig {

        @Override
        public UserBuilder configure(UserBuilder user) {
            return user
                    .username("test-user@localhost")
                    .password("password")
                    .firstName("Test")
                    .lastName("User")
                    .email("test-user@localhost");
        }
    }

}
