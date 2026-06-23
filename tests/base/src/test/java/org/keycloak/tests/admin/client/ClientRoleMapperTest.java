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
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;

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
public class ClientRoleMapperTest extends AbstractProtocolMapperTest {

    @InjectOAuthClient(config = TestClient.class)
    OAuthClient oAuthClient;

    @InjectUser(config = TestUser.class)
    ManagedUser user;

    @TestSetup
    public void setupRealms() {
        adminClient
                .realm("default")
                .clients()
                .get("test-app")
                .roles()
                .create(new RoleRepresentation(
                        "customer-user",
                        "",
                        false
                ));
        RoleRepresentation roleRepresentation = adminClient
                .realm("default")
                .clients()
                .get(oAuthClient.getClientId())
                .roles()
                .get("customer-user")
                .toRepresentation();
        adminClient
                .realm("default")
                .users()
                .get(user.getId())
                .roles()
                .clientLevel(oAuthClient.getClientId())
                .add(List.of(roleRepresentation));
    }

    @AfterEach
    public void cleanup() {
        adminClient
                .realm("default")
                .users()
                .get(user.getId())
                .logout();

        var resource = adminClient
                .realm("default")
                .clients()
                .get(oAuthClient.getClientId())
                .getProtocolMappers();
        resource
                .getMappers()
                .forEach(mapper -> resource.delete(mapper.getId()));
    }

    static Stream<Arguments> prefixTestCases() {
        return Stream.of(
                arguments(null, List.of("customer-user")),
                arguments("", List.of("customer-user")),
                arguments("client_id::", List.of("client_id::customer-user")),
                arguments("${client_id}::", List.of("test-app::customer-user"))
        );
    }

    @ParameterizedTest
    @MethodSource("prefixTestCases")
    public void testRoleMappingWithPrefix(String prefix, List<String> expectedRoles) {
        ProtocolMapperRepresentation protocolMapper = getProtocolMapper(prefix);
        applyProtocolMapper(protocolMapper);
        AuthorizationEndpointResponse login = oAuthClient.doLogin(user.getUsername(), user.getPassword());
        String code = login.getCode();
        AccessTokenResponse response = oAuthClient.doAccessTokenRequest(code);
        IDToken idToken = oAuthClient.verifyIDToken(response.getIdToken());
        AccessToken accessToken = oAuthClient.verifyToken(response.getAccessToken());

        Assertions.assertNotNull(idToken);
        Assertions.assertNotNull(accessToken);
        Assertions.assertEquals(expectedRoles, idToken.getOtherClaims().get("roles"));
        Assertions.assertEquals(expectedRoles, accessToken.getOtherClaims().get("roles"));
    }

    public static class TestClient extends DefaultOAuthClientConfiguration {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return super
                    .configure(client)
                    .id("test-app")
                    .protocol("openid-connect")
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

    private ProtocolMapperRepresentation getProtocolMapper(String prefix) {
        HashMap<String, String> configuration = new HashMap<>();
        configuration.put("usermodel.clientRoleMapping.rolePrefix", prefix);
        configuration.put("introspection.token.claim", "true");
        configuration.put("multivalued", "true");
        configuration.put("userinfo.token.claim", "true");
        configuration.put("id.token.claim", "true");
        configuration.put("access.token.claim", "true");
        configuration.put("claim.name", "roles");
        configuration.put("jsonType.label", "String");

        return makeMapper(
                "openid-connect",
                "userClientRoleMappingMapperTest",
                "oidc-usermodel-client-role-mapper",
                configuration
        );
    }

    private void applyProtocolMapper(ProtocolMapperRepresentation protocolMapper) {
        Response response = adminClient
                .realm("default")
                .clients()
                .get(oAuthClient.getClientId())
                .getProtocolMappers()
                .createMapper(protocolMapper);
        Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        response.close();
    }

}
