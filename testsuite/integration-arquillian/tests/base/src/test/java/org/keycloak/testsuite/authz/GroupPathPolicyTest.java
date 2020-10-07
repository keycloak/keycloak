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
package org.keycloak.testsuite.authz;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authorization.client.AuthorizationDeniedException;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.GroupMembershipMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation;
import org.keycloak.representations.idm.authorization.PermissionRequest;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.GroupBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.RolesBuilder;
import org.keycloak.testsuite.util.UserBuilder;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class GroupPathPolicyTest extends AbstractAuthzTest {

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
        config.put("full.path", "true");
        groupProtocolMapper.setConfig(config);

        testRealms.add(RealmBuilder.create().name("authz-test")
                .roles(RolesBuilder.create()
                        .realmRole(RoleBuilder.create().name("uma_authorization").build())
                )
                .group(GroupBuilder.create().name("Group A")
                    .subGroups(Arrays.asList("Group B", "Group D").stream().map(name -> {
                        if ("Group B".equals(name)) {
                            return GroupBuilder.create().name(name).subGroups(Arrays.asList("Group C", "Group E").stream().map(new Function<String, GroupRepresentation>() {
                                @Override
                                public GroupRepresentation apply(String name) {
                                    return GroupBuilder.create().name(name).build();
                                }
                            }).collect(Collectors.toList())).build();
                        }
                        return GroupBuilder.create().name(name).build();
                    }).collect(Collectors.toList())).build())
                .group(GroupBuilder.create().name("Group E").build())
                .user(UserBuilder.create().username("marta").password("password").addRoles("uma_authorization").addGroups("Group A"))
                .user(UserBuilder.create().username("alice").password("password").addRoles("uma_authorization"))
                .user(UserBuilder.create().username("kolo").password("password").addRoles("uma_authorization"))
                .client(ClientBuilder.create().clientId("resource-server-test")
                    .secret("secret")
                    .authorizationServicesEnabled(true)
                    .redirectUris("http://localhost/resource-server-test")
                    .defaultRoles("uma_protection")
                    .directAccessGrants()
                    .protocolMapper(groupProtocolMapper))
                .build());
    }

    @Before
    public void configureAuthorization() throws Exception {
        createResource("Resource A");
        createResource("Resource B");

        createGroupPolicy("Parent And Children Policy", "/Group A", true);
        createGroupPolicy("Only Children Policy", "/Group A/Group B/Group C", false);

        createResourcePermission("Resource A Permission", "Resource A", "Parent And Children Policy");
        createResourcePermission("Resource B Permission", "Resource B", "Only Children Policy");
    }

    @Test
    public void testAllowParentAndChildren() {
        AuthzClient authzClient = getAuthzClient();
        PermissionRequest request = new PermissionRequest("Resource A");
        String ticket = authzClient.protection().permission().create(request).getTicket();
        AuthorizationResponse response = authzClient.authorization("marta", "password").authorize(new AuthorizationRequest(ticket));

        assertNotNull(response.getToken());

        RealmResource realm = getRealm();
        GroupRepresentation group = getGroup("/Group A/Group B/Group C");
        UserRepresentation user = realm.users().search("kolo").get(0);

        realm.users().get(user.getId()).joinGroup(group.getId());

        ticket = authzClient.protection().permission().create(request).getTicket();
        response = authzClient.authorization("kolo", "password").authorize(new AuthorizationRequest(ticket));

        assertNotNull(response.getToken());
    }

    @Test
    public void testOnlyChildrenPolicy() throws Exception {
        RealmResource realm = getRealm();
        AuthzClient authzClient = getAuthzClient();
        PermissionRequest request = new PermissionRequest("Resource B");
        String ticket = authzClient.protection().permission().create(request).getTicket();

        try {
            authzClient.authorization("kolo", "password").authorize(new AuthorizationRequest(ticket));
            fail("Should fail because user is not granted with expected role");
        } catch (AuthorizationDeniedException ignore) {

        }

        GroupRepresentation group = getGroup("/Group A/Group B/Group C");
        UserRepresentation user = realm.users().search("kolo").get(0);

        realm.users().get(user.getId()).joinGroup(group.getId());

        AuthorizationResponse response = authzClient.authorization("kolo", "password").authorize(new AuthorizationRequest(ticket));

        assertNotNull(response.getToken());

        try {
            authzClient.authorization("marta", "password").authorize(new AuthorizationRequest(ticket));
            fail("Should fail because user is not granted with expected role");
        } catch (AuthorizationDeniedException ignore) {

        }
    }

    private void createGroupPolicy(String name, String groupPath, boolean extendChildren) {
        GroupPolicyRepresentation policy = new GroupPolicyRepresentation();

        policy.setName(name);
        policy.setGroupsClaim("groups");
        policy.addGroupPath(groupPath, extendChildren);

        getClient().authorization().policies().group().create(policy).close();
    }

    private void createResourcePermission(String name, String resource, String... policies) {
        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();

        permission.setName(name);
        permission.addResource(resource);
        permission.addPolicy(policies);

        getClient().authorization().permissions().resource().create(permission).close();
    }

    private void createResource(String name) {
        AuthorizationResource authorization = getClient().authorization();
        ResourceRepresentation resource = new ResourceRepresentation(name);

        authorization.resources().create(resource).close();
    }

    private RealmResource getRealm() {
        try {
            return getAdminClient().realm("authz-test");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create admin client");
        }
    }

    private ClientResource getClient(RealmResource realm) {
        ClientsResource clients = realm.clients();
        return clients.findByClientId("resource-server-test").stream().map(representation -> clients.get(representation.getId())).findFirst().orElseThrow(() -> new RuntimeException("Expected client [resource-server-test]"));
    }

    private AuthzClient getAuthzClient() {
        return AuthzClient.create(getClass().getResourceAsStream("/authorization-test/default-keycloak.json"));
    }

    private ClientResource getClient() {
        return getClient(getRealm());
    }

    private GroupRepresentation getGroup(String path) {
        String[] parts = path.split("/");
        RealmResource realm = getRealm();
        GroupRepresentation parent = null;

        for (String part : parts) {
            if ("".equals(part)) {
                continue;
            }
            if (parent == null) {
                parent = realm.groups().groups().stream().filter(new Predicate<GroupRepresentation>() {
                    @Override
                    public boolean test(GroupRepresentation groupRepresentation) {
                        return part.equals(groupRepresentation.getName());
                    }
                }).findFirst().get();
                continue;
            }

            GroupRepresentation group = getGroup(part, parent.getSubGroups());

            if (path.endsWith(group.getName())) {
                return group;
            }

            parent = group;
        }

        return null;
    }

    private GroupRepresentation getGroup(String name, List<GroupRepresentation> groups) {
        for (GroupRepresentation group : groups) {
            if (name.equals(group.getName())) {
                return group;
            }

            GroupRepresentation child = getGroup(name, group.getSubGroups());

            if (child != null && name.equals(child.getName())) {
                return child;
            }
        }

        return null;
    }
}
