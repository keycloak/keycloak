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

package org.keycloak.tests.admin.group;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.ChildGroupMembershipMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.GroupBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.tests.utils.admin.AdminApiUtil;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class ChildGroupMembershipMapperTest extends AbstractGroupTest {

    @InjectRealm(config = ChildGroupMembershipMapperTestRealmConfig.class)
    ManagedRealm managedRealm;

    private static final String CLIENT_ID = "my-app";
    private static final String CLIENT_SECRET = "password";
    private static final String PARENT_GROUP = "parent-group";
    private static final String CHILD_GROUP = "child-group";
    private static final String GRANDCHILD_GROUP = "grandchild-group";
    private static final String UNRELATED_GROUP = "unrelated-group";
    private static final String PARENT_GROUP_USER = "parentGroupUser";
    private static final String CHILD_GROUP_USER = "childGroupUser";
    private static final String GRANDCHILD_GROUP_USER = "grandchildGroupUser";
    private static final String UNRELATED_GROUP_USER = "unrelatedGroupUser";

    @Test
    @SuppressWarnings("unchecked")
    public void directChildOfParentIsIncluded() {
        UserRepresentation user = AdminApiUtil.findUserByUsername(managedRealm.admin(), CHILD_GROUP_USER);
        AccessToken token = login(user.getUsername(), CLIENT_ID, CLIENT_SECRET);
        Collection<String> groups = (Collection<String>) token.getOtherClaims().get("groups");
        MatcherAssert.assertThat(groups, Matchers.contains(CHILD_GROUP));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void indirectDescendantOfParentIsIncluded() {
        UserRepresentation user = AdminApiUtil.findUserByUsername(managedRealm.admin(), GRANDCHILD_GROUP_USER);
        AccessToken token = login(user.getUsername(), CLIENT_ID, CLIENT_SECRET);
        Collection<String> groups = (Collection<String>) token.getOtherClaims().get("groups");
        MatcherAssert.assertThat(groups, Matchers.contains(GRANDCHILD_GROUP));
    }

    @Test
    public void directMemberOfParentIsExcluded() {
        UserRepresentation user = AdminApiUtil.findUserByUsername(managedRealm.admin(), PARENT_GROUP_USER);
        AccessToken token = login(user.getUsername(), CLIENT_ID, CLIENT_SECRET);
        Assertions.assertNull(token.getOtherClaims().get("groups"));
    }

    @Test
    public void memberOfUnrelatedGroupIsExcluded() {
        UserRepresentation user = AdminApiUtil.findUserByUsername(managedRealm.admin(), UNRELATED_GROUP_USER);
        AccessToken token = login(user.getUsername(), CLIENT_ID, CLIENT_SECRET);
        Assertions.assertNull(token.getOtherClaims().get("groups"));
    }

    private static class ChildGroupMembershipMapperTestRealmConfig implements RealmConfig {

        private ProtocolMapperRepresentation createMapper() {
            ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
            mapper.setName("child-groups");
            mapper.setProtocolMapper(ChildGroupMembershipMapper.PROVIDER_ID);
            mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            Map<String, String> config = new HashMap<>();
            config.put(ChildGroupMembershipMapper.PARENT_GROUP_PATH, "/" + PARENT_GROUP);
            config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "groups");
            config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
            config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
            config.put(OIDCAttributeMapperHelper.MULTIVALUED, "true");
            mapper.setConfig(config);
            return mapper;
        }

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.clients(ClientBuilder.create(CLIENT_ID)
                    .enabled(true)
                    .secret(CLIENT_SECRET)
                    .directAccessGrantsEnabled(true)
                    .protocolMappers(createMapper()));

            realm.groups(GroupBuilder.create(PARENT_GROUP)
                    .subGroups(GroupBuilder.create(CHILD_GROUP)
                            .subGroups(GroupBuilder.create(GRANDCHILD_GROUP).build())
                            .build()));

            realm.groups(GroupBuilder.create(UNRELATED_GROUP));

            realm.users(UserBuilder.create(PARENT_GROUP_USER)
                    .enabled(true)
                    .password("password")
                    .groups(PARENT_GROUP));

            realm.users(UserBuilder.create(CHILD_GROUP_USER)
                    .enabled(true)
                    .password("password")
                    .groups(PARENT_GROUP + "/" + CHILD_GROUP));

            realm.users(UserBuilder.create(GRANDCHILD_GROUP_USER)
                    .enabled(true)
                    .password("password")
                    .groups(PARENT_GROUP + "/" + CHILD_GROUP + "/" + GRANDCHILD_GROUP));

            realm.users(UserBuilder.create(UNRELATED_GROUP_USER)
                    .enabled(true)
                    .password("password")
                    .groups(UNRELATED_GROUP));

            return realm;
        }
    }
}
