/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.authz;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.GroupMembershipMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.GroupBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.UserBuilder;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class GroupPathWithoutGroupClaimPolicyTest extends GroupPathPolicyTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        ProtocolMapperRepresentation groupProtocolMapper = new ProtocolMapperRepresentation();

        groupProtocolMapper.setName("groups");
        groupProtocolMapper.setProtocolMapper(GroupMembershipMapper.PROVIDER_ID);
        groupProtocolMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> config = new HashMap<>();
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "groups");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        groupProtocolMapper.setConfig(config);

        testRealms.add(RealmBuilder.create().name("authz-test")
                .realmRoles("uma_authorization")
                .groups(GroupBuilder.create().name("Group A")
                    .subGroups(GroupBuilder.create("Group B").subGroups("Group C", "Group E"))
                    .subGroups("Group D"))
                .groups(GroupBuilder.create().name("Group E"))
                .users(UserBuilder.create().username("marta").password("password").realmRoles("uma_authorization").groups("Group A"))
                .users(UserBuilder.create().username("alice").password("password").realmRoles("uma_authorization"))
                .users(UserBuilder.create().username("kolo").password("password").realmRoles("uma_authorization"))
                .clients(ClientBuilder.create().clientId("resource-server-test")
                    .secret("secret")
                    .authorizationServicesEnabled(true)
                    .redirectUris("http://localhost/resource-server-test")
                    .defaultRoles("uma_protection")
                    .directAccessGrantsEnabled())
                .build());
    }
}
