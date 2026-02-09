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

package org.keycloak.tests.admin.group;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.GroupProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.GroupMembershipMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.UserAttributeMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.GroupConfigBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.ProtocolMapperUtil;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@KeycloakIntegrationTest
public class GroupMappersTest extends AbstractGroupTest {

    @InjectRealm(config = GroupMappersTestRealmConfig.class)
    ManagedRealm managedRealm;

    private static final String CLIENT_ID = "my-app";
    private static final String CLIENT_SECRET = "password";
    private static final String CLIENT_ROLE = "customer-user";
    private static final String TOP_GROUP = "topGroup";
    private static final String TOP_ATTRIBUTE = "topAttribute";
    private static final String LEVEL_2_ATTRIBUTE = "level2Attribute";
    private static final String LEVEL_2_GROUP = "level2group";
    private static final String TOP_GROUP_USER = "topGroupUser";
    private static final String LEVEL_2_GROUP_USER = "level2GroupUser";

    @Test
    @SuppressWarnings("unchecked")
    public void testGroupMappers() {
        {
            UserRepresentation user = AdminApiUtil.findUserByUsername(managedRealm.admin(), TOP_GROUP_USER);

            AccessToken token = login(user.getUsername(), CLIENT_ID, CLIENT_SECRET);
            Assertions.assertTrue(token.getRealmAccess().getRoles().contains("user"));
            Assertions.assertNotNull(token.getOtherClaims().get("groups"));
            Map<String, Collection<String>> groups = (Map<String, Collection<String>>) token.getOtherClaims().get("groups");
            MatcherAssert.assertThat(groups.get("groups"), Matchers.contains(TOP_GROUP));
            Assertions.assertEquals("true", token.getOtherClaims().get(TOP_ATTRIBUTE));
        }
        {
            UserRepresentation user = AdminApiUtil.findUserByUsername(managedRealm.admin(), LEVEL_2_GROUP_USER);

            AccessToken token = login(user.getUsername(), CLIENT_ID, CLIENT_SECRET);
            Assertions.assertTrue(token.getRealmAccess().getRoles().contains("user"));
            Assertions.assertTrue(token.getRealmAccess().getRoles().contains("admin"));
            Assertions.assertTrue(token.getResourceAccess(CLIENT_ID).getRoles().contains(CLIENT_ROLE));
            Assertions.assertNotNull(token.getOtherClaims().get("groups"));
            Map<String, Collection<String>> groups = (Map<String, Collection<String>>) token.getOtherClaims().get("groups");
            MatcherAssert.assertThat(groups.get("groups"), Matchers.contains(LEVEL_2_GROUP));
            Assertions.assertEquals("true", token.getOtherClaims().get(TOP_ATTRIBUTE));
            Assertions.assertEquals("true", token.getOtherClaims().get(LEVEL_2_ATTRIBUTE));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGroupMappersWithSlash() {
        RealmResource realm = managedRealm.admin();
        GroupRepresentation topGroup = realm.getGroupByPath("/" + TOP_GROUP);
        Assertions.assertNotNull(topGroup);
        GroupRepresentation childSlash = new GroupRepresentation();
        childSlash.setName("child/slash");
        Response response = realm.groups().group(topGroup.getId()).subGroup(childSlash);
        childSlash.setId(ApiUtil.getCreatedId(response));

        UserRepresentation user = AdminApiUtil.findUserByUsername(managedRealm.admin(), LEVEL_2_GROUP_USER);
        realm.users().get(user.getId()).joinGroup(childSlash.getId());

        ClientResource client = AdminApiUtil.findClientByClientId(realm, CLIENT_ID);
        ProtocolMappersResource protocolMappers = client.getProtocolMappers();
        ProtocolMapperRepresentation groupsMapper = ProtocolMapperUtil.getMapperByNameAndProtocol(
                protocolMappers, OIDCLoginProtocol.LOGIN_PROTOCOL, "groups");
        Assertions.assertNotNull(groupsMapper);
        groupsMapper.getConfig().put("full.path", Boolean.TRUE.toString());
        protocolMappers.update(groupsMapper.getId(), groupsMapper);

        groupsMapper.getConfig().remove("full.path");
        managedRealm.cleanup().add(r -> {
            r.users().get(user.getId()).leaveGroup(childSlash.getId());
            r.groups().group(childSlash.getId()).remove();
            r.clients().get(client.toRepresentation().getId())
                    .getProtocolMappers().update(groupsMapper.getId(), groupsMapper);
        });

        AccessToken token = login(user.getUsername(), CLIENT_ID, CLIENT_SECRET);
        Assertions.assertNotNull(token.getOtherClaims().get("groups"));
        Map<String, Collection<String>> groups = (Map<String, Collection<String>>) token.getOtherClaims().get("groups");
        MatcherAssert.assertThat(groups.get("groups"), Matchers.containsInAnyOrder(
                KeycloakModelUtils.buildGroupPath(GroupProvider.DEFAULT_ESCAPE_SLASHES, TOP_GROUP, LEVEL_2_GROUP),
                KeycloakModelUtils.buildGroupPath(GroupProvider.DEFAULT_ESCAPE_SLASHES, TOP_GROUP, "child/slash")));
    }

    private static class GroupMappersTestRealmConfig implements RealmConfig {

        private List<ProtocolMapperRepresentation> createMappers() {
            List<ProtocolMapperRepresentation> mappers = new LinkedList<>();
            ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
            mapper.setName("groups");
            mapper.setProtocolMapper(GroupMembershipMapper.PROVIDER_ID);
            mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            Map<String, String> config = new HashMap<>();
            config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "groups.groups");
            config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
            config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
            mapper.setConfig(config);
            mappers.add(mapper);

            mapper = new ProtocolMapperRepresentation();
            mapper.setName(TOP_ATTRIBUTE);
            mapper.setProtocolMapper(UserAttributeMapper.PROVIDER_ID);
            mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            config = new HashMap<>();
            config.put(ProtocolMapperUtils.USER_ATTRIBUTE, TOP_ATTRIBUTE);
            config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, TOP_ATTRIBUTE);
            config.put(OIDCAttributeMapperHelper.JSON_TYPE, ProviderConfigProperty.STRING_TYPE);
            config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
            config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
            mapper.setConfig(config);
            mappers.add(mapper);

            mapper = new ProtocolMapperRepresentation();
            mapper.setName(LEVEL_2_ATTRIBUTE);
            mapper.setProtocolMapper(UserAttributeMapper.PROVIDER_ID);
            mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            config = new HashMap<>();
            config.put(ProtocolMapperUtils.USER_ATTRIBUTE, LEVEL_2_ATTRIBUTE);
            config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, LEVEL_2_ATTRIBUTE);
            config.put(OIDCAttributeMapperHelper.JSON_TYPE, ProviderConfigProperty.STRING_TYPE);
            config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
            config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
            mapper.setConfig(config);
            mappers.add(mapper);
            return mappers;
        }

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            List<ProtocolMapperRepresentation> mappers = createMappers();

            realm.addClient(CLIENT_ID)
                    .enabled(true)
                    .secret(CLIENT_SECRET)
                    .directAccessGrantsEnabled(true)
                    .protocolMappers(mappers);

            realm.eventsEnabled(true)
                    .clientRoles(CLIENT_ID, CLIENT_ROLE);

            GroupRepresentation subGroup = GroupConfigBuilder.create()
                    .name(LEVEL_2_GROUP)
                    .realmRoles("admin")
                    .clientRoles(CLIENT_ID, CLIENT_ROLE)
                    .attribute(LEVEL_2_ATTRIBUTE, "true")
                    .build();


            GroupRepresentation subGroup2 = GroupConfigBuilder.create()
                    .name("level2group2")
                    .realmRoles("admin")
                    .clientRoles(CLIENT_ID, CLIENT_ROLE)
                    .attribute(LEVEL_2_ATTRIBUTE, "true")
                    .build();

            realm.addGroup(TOP_GROUP)
                    .attribute(TOP_ATTRIBUTE, "true")
                    .realmRoles("user")
                    .subGroups(subGroup, subGroup2);

            realm.addUser(TOP_GROUP_USER)
                    .name("John", "Doe")
                    .enabled(true)
                    .email("top@redhat.com")
                    .password("password")
                    .groups(TOP_GROUP);

            realm.addUser(LEVEL_2_GROUP_USER)
                    .name("Jane", "Doe")
                    .enabled(true)
                    .email("level2@redhat.com")
                    .password("password")
                    .groups("topGroup/level2group");

            return realm;
        }
    }
}
