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

package org.keycloak.testsuite.admin.group;

import jakarta.ws.rs.core.Response;
import java.util.*;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
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
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.ProtocolMapperUtil;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class GroupMappersTest extends AbstractGroupTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealmRep = loadTestRealm(testRealms);

        testRealmRep.setEventsEnabled(true);

        ClientRepresentation client = getClientByAlias(testRealmRep, "test-app");
        Assert.assertNotNull("test-app client exists", client);

        client.setDirectAccessGrantsEnabled(true);

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
        mapper.setName("topAttribute");
        mapper.setProtocolMapper(UserAttributeMapper.PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        config = new HashMap<>();
        config.put(ProtocolMapperUtils.USER_ATTRIBUTE, "topAttribute");
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "topAttribute");
        config.put(OIDCAttributeMapperHelper.JSON_TYPE, ProviderConfigProperty.STRING_TYPE);
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        mapper.setConfig(config);
        mappers.add(mapper);

        mapper = new ProtocolMapperRepresentation();
        mapper.setName("level2Attribute");
        mapper.setProtocolMapper(UserAttributeMapper.PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        config = new HashMap<>();
        config.put(ProtocolMapperUtils.USER_ATTRIBUTE, "level2Attribute");
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "level2Attribute");
        config.put(OIDCAttributeMapperHelper.JSON_TYPE, ProviderConfigProperty.STRING_TYPE);
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        mapper.setConfig(config);
        mappers.add(mapper);

        client.setProtocolMappers(mappers);
    }

    private ClientRepresentation getClientByAlias(RealmRepresentation testRealmRep, String alias) {
        for (ClientRepresentation client: testRealmRep.getClients()) {
            if (alias.equals(client.getClientId())) {
                return client;
            }
        }
        return null;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGroupMappers() throws Exception {
        RealmResource realm = adminClient.realms().realm("test");
        {
            UserRepresentation user = realm.users().search("topGroupUser", -1, -1).get(0);

            AccessToken token = login(user.getUsername(), "test-app", "password", user.getId());
            Assert.assertTrue(token.getRealmAccess().getRoles().contains("user"));
            Assert.assertNotNull(token.getOtherClaims().get("groups"));
            Map<String, Collection<String>> groups = (Map<String, Collection<String>>) token.getOtherClaims().get("groups");
            MatcherAssert.assertThat(groups.get("groups"), Matchers.contains("topGroup"));
            Assert.assertEquals("true", token.getOtherClaims().get("topAttribute"));
        }
        {
            UserRepresentation user = realm.users().search("level2GroupUser", -1, -1).get(0);

            AccessToken token = login(user.getUsername(), "test-app", "password", user.getId());
            Assert.assertTrue(token.getRealmAccess().getRoles().contains("user"));
            Assert.assertTrue(token.getRealmAccess().getRoles().contains("admin"));
            Assert.assertTrue(token.getResourceAccess("test-app").getRoles().contains("customer-user"));
            Assert.assertNotNull(token.getOtherClaims().get("groups"));
            Map<String, Collection<String>> groups = (Map<String, Collection<String>>) token.getOtherClaims().get("groups");
            MatcherAssert.assertThat(groups.get("groups"), Matchers.contains("level2group"));
            Assert.assertEquals("true", token.getOtherClaims().get("topAttribute"));
            Assert.assertEquals("true", token.getOtherClaims().get("level2Attribute"));
        }
    }

    @Test
    public void testGroupMappersWithSlash() throws Exception {
        RealmResource realm = adminClient.realms().realm("test");
        GroupRepresentation topGroup = realm.getGroupByPath("/topGroup");
        Assert.assertNotNull(topGroup);
        GroupRepresentation childSlash = new GroupRepresentation();
        childSlash.setName("child/slash");
        try (Response response = realm.groups().group(topGroup.getId()).subGroup(childSlash)) {
            Assert.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            childSlash.setId(ApiUtil.getCreatedId(response));
        }
        List<UserRepresentation> users = realm.users().search("level2GroupUser", true);
        Assert.assertEquals(1, users.size());
        UserRepresentation user = users.iterator().next();
        realm.users().get(user.getId()).joinGroup(childSlash.getId());

        ProtocolMappersResource protocolMappers = ApiUtil.findClientResourceByClientId(realm, "test-app").getProtocolMappers();
        ProtocolMapperRepresentation groupsMapper = ProtocolMapperUtil.getMapperByNameAndProtocol(
                protocolMappers, OIDCLoginProtocol.LOGIN_PROTOCOL, "groups");
        groupsMapper.getConfig().put("full.path", Boolean.TRUE.toString());
        protocolMappers.update(groupsMapper.getId(), groupsMapper);

        try {
            AccessToken token = login(user.getUsername(), "test-app", "password", user.getId());
            Assert.assertNotNull(token.getOtherClaims().get("groups"));
            Map<String, Collection<String>> groups = (Map<String, Collection<String>>) token.getOtherClaims().get("groups");
            MatcherAssert.assertThat(groups.get("groups"), Matchers.containsInAnyOrder(
                    KeycloakModelUtils.buildGroupPath(GroupProvider.DEFAULT_ESCAPE_SLASHES, "topGroup", "level2group"),
                    KeycloakModelUtils.buildGroupPath(GroupProvider.DEFAULT_ESCAPE_SLASHES, "topGroup", "child/slash")));
        } finally {
            realm.users().get(user.getId()).leaveGroup(childSlash.getId());
            realm.groups().group(childSlash.getId()).remove();
            groupsMapper.getConfig().remove("full.path");
            protocolMappers.update(groupsMapper.getId(), groupsMapper);
        }
    }
}
