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

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.GroupMembershipMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.UserAttributeMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.*;

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
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "groups");
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
            List<String> groups = (List<String>) token.getOtherClaims().get("groups");
            Assert.assertNotNull(groups);
            Assert.assertTrue(groups.size() == 1);
            Assert.assertEquals("topGroup", groups.get(0));
            Assert.assertEquals("true", token.getOtherClaims().get("topAttribute"));
        }
        {
            UserRepresentation user = realm.users().search("level2GroupUser", -1, -1).get(0);

            AccessToken token = login(user.getUsername(), "test-app", "password", user.getId());
            Assert.assertTrue(token.getRealmAccess().getRoles().contains("user"));
            Assert.assertTrue(token.getRealmAccess().getRoles().contains("admin"));
            Assert.assertTrue(token.getResourceAccess("test-app").getRoles().contains("customer-user"));
            List<String> groups = (List<String>) token.getOtherClaims().get("groups");
            Assert.assertNotNull(groups);
            Assert.assertTrue(groups.size() == 1);
            Assert.assertEquals("level2group", groups.get(0));
            Assert.assertEquals("true", token.getOtherClaims().get("topAttribute"));
            Assert.assertEquals("true", token.getOtherClaims().get("level2Attribute"));
        }
    }
}
