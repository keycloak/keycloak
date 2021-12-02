/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authorization.client.AuthorizationDeniedException;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.UserAttributeMapper;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.PermissionRequest;
import org.keycloak.representations.idm.authorization.RegexPolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;

/**
 * @author <a href="mailto:yoshiyuki.tabata.jy@hitachi.com">Yoshiyuki Tabata</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class RegexPolicyTest extends AbstractAuthzTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        ProtocolMapperRepresentation userAttrFooProtocolMapper = new ProtocolMapperRepresentation();
        userAttrFooProtocolMapper.setName("userAttrFoo");
        userAttrFooProtocolMapper.setProtocolMapper(UserAttributeMapper.PROVIDER_ID);
        userAttrFooProtocolMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> configFoo = new HashMap<>();
        configFoo.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        configFoo.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        configFoo.put(OIDCAttributeMapperHelper.JSON_TYPE, "String");
        configFoo.put("user.attribute", "foo");
        configFoo.put("claim.name", "foo");
        userAttrFooProtocolMapper.setConfig(configFoo);

        ProtocolMapperRepresentation userAttrBarProtocolMapper = new ProtocolMapperRepresentation();
        userAttrBarProtocolMapper.setName("userAttrBar");
        userAttrBarProtocolMapper.setProtocolMapper(UserAttributeMapper.PROVIDER_ID);
        userAttrBarProtocolMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> configBar = new HashMap<>();
        configBar.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        configBar.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        configBar.put(OIDCAttributeMapperHelper.JSON_TYPE, "String");
        configBar.put("user.attribute", "bar");
        configBar.put("claim.name", "bar");
        userAttrBarProtocolMapper.setConfig(configBar);

        testRealms.add(RealmBuilder.create().name("authz-test")
            .user(UserBuilder.create().username("marta").password("password").addAttribute("foo", "foo").addAttribute("bar",
                "barbar"))
            .user(UserBuilder.create().username("taro").password("password").addAttribute("foo", "faa").addAttribute("bar",
                "bbarbar"))
            .client(ClientBuilder.create().clientId("resource-server-test").secret("secret").authorizationServicesEnabled(true)
                .redirectUris("http://localhost/resource-server-test").directAccessGrants()
                .protocolMapper(userAttrFooProtocolMapper, userAttrBarProtocolMapper))
            .build());
    }

    @Before
    public void configureAuthorization() throws Exception {
        createResource("Resource A");
        createResource("Resource B");

        createRegexPolicy("Regex foo Policy", "foo", "foo");
        createRegexPolicy("Regex bar Policy", "bar", "^bar.+$");

        createResourcePermission("Resource A Permission", "Resource A", "Regex foo Policy");
        createResourcePermission("Resource B Permission", "Resource B", "Regex bar Policy");
    }

    private void createResource(String name) {
        AuthorizationResource authorization = getClient().authorization();
        ResourceRepresentation resource = new ResourceRepresentation(name);

        authorization.resources().create(resource).close();
    }

    private void createRegexPolicy(String name, String targetClaim, String pattern) {
        RegexPolicyRepresentation policy = new RegexPolicyRepresentation();

        policy.setName(name);
        policy.setTargetClaim(targetClaim);
        policy.setPattern(pattern);

        getClient().authorization().policies().regex().create(policy).close();
    }

    private void createResourcePermission(String name, String resource, String... policies) {
        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();

        permission.setName(name);
        permission.addResource(resource);
        permission.addPolicy(policies);

        getClient().authorization().permissions().resource().create(permission).close();
    }

    private ClientResource getClient() {
        return getClient(getRealm());
    }

    private ClientResource getClient(RealmResource realm) {
        ClientsResource clients = realm.clients();
        return clients.findByClientId("resource-server-test").stream()
            .map(representation -> clients.get(representation.getId())).findFirst()
            .orElseThrow(() -> new RuntimeException("Expected client [resource-server-test]"));
    }

    private RealmResource getRealm() {
        try {
            return getAdminClient().realm("authz-test");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create admin client");
        }
    }

    @Test
    public void testWithExpectedUserAttribute() {
        // Access Resource A with marta.
        AuthzClient authzClient = getAuthzClient();
        PermissionRequest request = new PermissionRequest("Resource A");
        String ticket = authzClient.protection().permission().create(request).getTicket();
        AuthorizationResponse response = authzClient.authorization("marta", "password")
            .authorize(new AuthorizationRequest(ticket));
        assertNotNull(response.getToken());

        // Access Resource B with marta.
        request = new PermissionRequest("Resource B");
        ticket = authzClient.protection().permission().create(request).getTicket();
        response = authzClient.authorization("marta", "password").authorize(new AuthorizationRequest(ticket));
        assertNotNull(response.getToken());
    }

    @Test
    public void testWithoutExpectedUserAttribute() {
        // Access Resource A with taro.
        AuthzClient authzClient = getAuthzClient();
        PermissionRequest request = new PermissionRequest("Resource A");
        String ticket = authzClient.protection().permission().create(request).getTicket();
        try {
            authzClient.authorization("taro", "password").authorize(new AuthorizationRequest(ticket));
            fail("Should fail.");
        } catch (AuthorizationDeniedException ignore) {

        }

        // Access Resource B with taro.
        request = new PermissionRequest("Resource B");
        ticket = authzClient.protection().permission().create(request).getTicket();
        try {
            authzClient.authorization("taro", "password").authorize(new AuthorizationRequest(ticket));
            fail("Should fail.");
        } catch (AuthorizationDeniedException ignore) {

        }
    }

    private AuthzClient getAuthzClient() {
        return AuthzClient.create(getClass().getResourceAsStream("/authorization-test/default-keycloak.json"));
    }
}
