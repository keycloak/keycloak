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
package org.keycloak.tests.authz.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.GroupMembershipMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.GroupBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.authz.services.config.DefaultAuthzServicesServerConfig;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest(config = DefaultAuthzServicesServerConfig.class)
public class GroupPolicyTest {

    private static final String PASSWORD = "password";
    private static final String NESTED_USER = "nested-user";
    private static final String ROOT_USER = "root-user";
    private static final String PATH_LIKE_USER = "path-like-user";
    private static final String NESTED_GROUP_PATH = "/Group A/Group B/Group E";
    private static final String ROOT_GROUP_PATH = "/Group E";
    private static final String RESOURCE = "protected-resource";
    private static final String POLICY = "group-policy";

    @InjectRealm(config = GroupPolicyRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectClient(config = ResourceServerConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedClient resourceServer;

    @InjectOAuthClient(lifecycle = LifeCycle.METHOD)
    OAuthClient oauth;

    @Test
    public void testBareGroupNameOnlyMatchesTopLevelGroup() {
        configureGroupPolicy(ROOT_GROUP_PATH);

        assertDecision(ROOT_USER, true);
        assertDecision(NESTED_USER, true);
    }

    @Test
    public void testBareGroupNameDoesNotMatchNestedGroup() {
        configureGroupPolicy(NESTED_GROUP_PATH);

        assertDecision(NESTED_USER, false);
        assertDecision(ROOT_USER, false);
    }

    @Test
    public void testFullGroupPathMatchesNestedGroup() {
        setFullPathMapper(true);
        configureGroupPolicy(NESTED_GROUP_PATH);

        assertDecision(NESTED_USER, true);
        assertDecision(ROOT_USER, false);
    }

    @Test
    public void testPathLikeTopLevelGroupNameDoesNotMatchNestedGroup() {
        GroupRepresentation group = GroupBuilder.create().name(NESTED_GROUP_PATH).build();
        String groupId;

        try (Response response = realm.admin().groups().add(group)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            groupId = ApiUtil.getCreatedId(response);
        }

        UserRepresentation user = AdminApiUtil.findUserByUsername(realm.admin(), PATH_LIKE_USER);
        realm.admin().users().get(user.getId()).joinGroup(groupId);
        configureGroupPolicy(NESTED_GROUP_PATH);

        assertDecision(PATH_LIKE_USER, false);
    }

    private void configureGroupPolicy(String groupPath) {
        GroupPolicyRepresentation policy = new GroupPolicyRepresentation();
        policy.setName(POLICY);
        policy.setGroupsClaim("groups");
        policy.addGroupPath(groupPath, false);

        try (Response response = resourceServer.admin().authorization().policies().group().create(policy)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        }

        policy = resourceServer.admin().authorization().policies().group().findByName(POLICY);
        assertNotNull(policy);

        Authz.create(resourceServer, ResourceRepresentation.create()
                .name(RESOURCE)
                .build());
        Authz.create(resourceServer, ResourcePermissionRepresentation.create()
                .name("resource-permission")
                .resources(Set.of(RESOURCE))
                .policies(Set.of(policy.getId()))
                .build());
    }

    private void assertDecision(String username, boolean granted) {
        AccessTokenResponse tokenResponse = oauth.client(resourceServer.getClientId(), resourceServer.getSecret())
                .doPasswordGrantRequest(username, PASSWORD);
        assertNotNull(tokenResponse.getIdToken());

        AccessTokenResponse authorizationResponse = oauth.client(resourceServer.getClientId(), resourceServer.getSecret())
                .permissionGrantRequest()
                .claimToken(tokenResponse.getIdToken())
                .send();

        assertEquals(granted, authorizationResponse.getAccessToken() != null,
                () -> "Unexpected authorization decision: " + authorizationResponse.getErrorDescription());
    }

    private void setFullPathMapper(boolean fullPath) {
        ProtocolMapperRepresentation mapper = resourceServer.admin().getProtocolMappers().getMappers().stream()
                .filter(candidate -> "groups".equals(candidate.getName()))
                .findFirst()
                .orElseThrow();
        mapper.getConfig().put("full.path", Boolean.toString(fullPath));
        resourceServer.admin().getProtocolMappers().update(mapper.getId(), mapper);
    }

    public static class GroupPolicyRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                    .groups(GroupBuilder.create("Group A")
                            .subGroups(GroupBuilder.create("Group B").subGroups("Group E")))
                    .groups(GroupBuilder.create("Group E"))
                    .users(UserBuilder.create(NESTED_USER)
                            .name("Nested", "User")
                            .email("nested-user@localhost")
                            .password(PASSWORD)
                            .enabled(true)
                            .groups("Group A/Group B/Group E"))
                    .users(UserBuilder.create(ROOT_USER)
                            .name("Root", "User")
                            .email("root-user@localhost")
                            .password(PASSWORD)
                            .enabled(true)
                            .groups("Group E"))
                    .users(UserBuilder.create(PATH_LIKE_USER)
                            .name("Path-like", "User")
                            .email("path-like-user@localhost")
                            .password(PASSWORD)
                            .enabled(true));
        }
    }

    public static class ResourceServerConfig implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client
                    .secret("secret")
                    .authorizationServicesEnabled(true)
                    .directAccessGrantsEnabled(true)
                    .protocolMappers(createGroupMapper());
        }

        private ProtocolMapperRepresentation createGroupMapper() {
            ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
            mapper.setName("groups");
            mapper.setProtocolMapper(GroupMembershipMapper.PROVIDER_ID);
            mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);

            Map<String, String> config = new HashMap<>();
            config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "groups");
            config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
            config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
            config.put("full.path", "false");
            mapper.setConfig(config);
            return mapper;
        }
    }
}
