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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.PermissionRequest;
import org.keycloak.representations.idm.authorization.PermissionResponse;
import org.keycloak.representations.idm.authorization.RegexPolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.GroupBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:yoshiyuki.tabata.jy@hitachi.com">Yoshiyuki Tabata</a>
 */
public class RegexPolicyTest extends AbstractAuthzTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        Map<String, String> claims = new HashMap<>();

        claims.put("user.attribute", "foo");
        claims.put("claim.name", "foo");
        ProtocolMapperRepresentation userAttrFooProtocolMapper = addClaimMapper("userAttrFoo", claims);

        claims.put("user.attribute", "bar");
        claims.put("claim.name", "bar");
        ProtocolMapperRepresentation userAttrBarProtocolMapper = addClaimMapper("userAttrBar", claims);

        claims.put("user.attribute", "json-simple");
        claims.put("claim.name", "userinfo");
        ProtocolMapperRepresentation userAttrJsonProtocolMapper = addClaimMapper("userAttrJsonSimple", claims);

        claims.put("user.attribute", "json-complex");
        claims.put("claim.name", "json-complex");
        ProtocolMapperRepresentation userAttrJsonComplexProtocolMapper = addClaimMapper("userAttrJsonComplex", claims);

        ProtocolMapperRepresentation userAttributesProtocolMapper = new ProtocolMapperRepresentation();
        userAttributesProtocolMapper.setName("canWriteItems");
        userAttributesProtocolMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        userAttributesProtocolMapper.setProtocolMapper(UserAttributeMapper.PROVIDER_ID);
        Map<String, String> PMUserConfig = new HashMap<>();
        PMUserConfig.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME,"customPermissions.canCreateItems");
        PMUserConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN,"true");
        PMUserConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO,"true");
        PMUserConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN,"true");
        PMUserConfig.put("user.attribute","canCreateItems");
        PMUserConfig.put("aggregate.attrs","false");
        PMUserConfig.put("multivalued","false");
        userAttributesProtocolMapper.setConfig(PMUserConfig);

        ProtocolMapperRepresentation groupAttributesProtocolMapper = new ProtocolMapperRepresentation();
        groupAttributesProtocolMapper.setName("Group_Mapper");
        groupAttributesProtocolMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        groupAttributesProtocolMapper.setProtocolMapper(UserAttributeMapper.PROVIDER_ID);
        Map<String, String> PMgroupConfig = new HashMap<>();
        PMgroupConfig.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME,"attributes.values");
        PMgroupConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN,"true");
        PMgroupConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN,"true");
        PMgroupConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO,"true");
        PMgroupConfig.put("user.attribute","attribute");
        PMgroupConfig.put("aggregate.attrs","false");
        PMgroupConfig.put("multivalued","false");
        groupAttributesProtocolMapper.setConfig(PMgroupConfig);

        //        For JSON-based claims, you can use dot notation for nesting and square brackets to access array fields by index. For example, contact.address[0].country.

        testRealms.add(RealmBuilder.create().name("authz-test")
            .user(UserBuilder.create().username("marta").password("password").addAttribute("foo", "foo").addAttribute("bar",
                "barbar").addAttribute("json-simple", "{\"tenant\": \"abc\"}")
                    .addAttribute("json-complex", "{\"userinfo\": {\"tenant\": \"abc\"}, \"some-array\": [\"foo\",\"bar\"]}"))
            .user(UserBuilder.create().username("taro").password("password").addAttribute("foo", "faa").addAttribute("bar",
                "bbarbar"))
            .user(UserBuilder.create().username("my-user").password("password").addAttribute("canCreateItems","true"))
            .user(UserBuilder.create().username("my-user2").password("password").addAttribute("canCreateItems","false"))
            .user(UserBuilder.create().username("my-user3").password("password").addAttribute("otherClaim","something"))
            .user(UserBuilder.create().username("context-user").password("password").addAttribute("custom", "foo"))
            .group(GroupBuilder.create().name("ADMIN").singleAttribute("attribute","example").build())
            .user(UserBuilder.create().username("admin").password("password").addGroups("ADMIN"))


            .client(ClientBuilder.create().clientId("resource-server-test").secret("secret").authorizationServicesEnabled(true)
                .redirectUris("http://localhost/resource-server-test").directAccessGrants()
                .protocolMapper(userAttrFooProtocolMapper, userAttrBarProtocolMapper, userAttrJsonProtocolMapper, userAttrJsonComplexProtocolMapper,userAttributesProtocolMapper,groupAttributesProtocolMapper))
                .build());
    }

    @Before
    public void configureAuthorization() throws Exception {
        createResource("Resource A");
        createResource("Resource B");
        createResource("Resource C");
        createResource("Resource D");
        createResource("Resource E");
        createResource("Resource ITEM");
        createResource("Resource CONTEXT");
        ScopeRepresentation scopeRead = new ScopeRepresentation();
        scopeRead.setName("read");
        ScopeRepresentation scopeDelete = new ScopeRepresentation();
        scopeDelete.setName("delete");
        getClient().authorization().scopes().scopes().add(scopeRead);
        getClient().authorization().scopes().scopes().add(scopeDelete);

        createResourceWithScopes("service",Set.of(scopeRead));

        createRegexPolicy("Regex foo Policy", "foo", "foo");
        createRegexPolicy("Regex bar Policy", "bar", "^bar.+$");
        createRegexPolicy("Regex json-simple Policy", "userinfo.tenant", "^abc$");
        createRegexPolicy("Regex json-complex Policy", "json-complex.userinfo.tenant", "^abc$");
        createRegexPolicy("Regex json-array Policy", "json-complex.some-array[1]", "bar");
        createRegexPolicy("Regex user attribute to json-Complex Policy", "customPermissions.canCreateItems", "true");
        createRegexPolicyExtended("attribute-policy","attributes.values","^example$",Logic.POSITIVE);
        createRegexPolicy("Regex context policy", "custom", "^foo$", true);

        createResourcePermission("Resource A Permission", "Resource A", "Regex foo Policy");
        createResourcePermission("Resource B Permission", "Resource B", "Regex bar Policy");
        createResourcePermission("Resource C Permission", "Resource C", "Regex json-simple Policy");
        createResourcePermission("Resource D Permission", "Resource D", "Regex json-complex Policy");
        createResourcePermission("Resource E Permission", "Resource E", "Regex json-array Policy");

        createResourcePermission("Resource ITEM Permission", "Resource ITEM", "Regex user attribute to json-Complex Policy");
        createResourceScopesPermissionExtended("read-permission","service",DecisionStrategy.UNANIMOUS,"read","attribute-policy");

        createResourcePermission("Resource CONTEXT Permission", "Resource CONTEXT", "Regex context policy");
    }

    private void createResource(String name) {
        AuthorizationResource authorization = getClient().authorization();
        ResourceRepresentation resource = new ResourceRepresentation(name);

        authorization.resources().create(resource).close();
    }

    private void createResourceWithScopes(String name, Set<ScopeRepresentation> scopes) {
        AuthorizationResource authorization = getClient().authorization();
        ResourceRepresentation resource = new ResourceRepresentation(name);
        resource.setScopes(scopes);
        authorization.resources().create(resource).close();
    }


    private void createRegexPolicy(String name, String targetClaim, String pattern) {
        createRegexPolicy(name, targetClaim, pattern, false);
    }

    private void createRegexPolicy(String name, String targetClaim, String pattern, Boolean targetContextAttributes) {
        RegexPolicyRepresentation policy = new RegexPolicyRepresentation();

        policy.setName(name);
        policy.setTargetClaim(targetClaim);
        policy.setPattern(pattern);
        policy.setTargetContextAttributes(targetContextAttributes);

        getClient().authorization().policies().regex().create(policy).close();
    }
private void createRegexPolicyExtended(String name, String targetClaim, String pattern,Logic logic) {
        RegexPolicyRepresentation policy = new RegexPolicyRepresentation();

        policy.setName(name);
        policy.setTargetClaim(targetClaim);
        policy.setPattern(pattern);
        policy.setLogic(logic);
        getClient().authorization().policies().regex().create(policy).close();
    }

    private void createResourcePermission(String name, String resource, String... policies) {
        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();

        permission.setName(name);
        permission.addResource(resource);
        permission.addPolicy(policies);

        getClient().authorization().permissions().resource().create(permission).close();
    }
    private void createResourceScopesPermissionExtended(String name, String resource, DecisionStrategy strategy,String scope,String... policies) {
        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();
        permission.setName(name);
        permission.addResource(resource);
        permission.addPolicy(policies);
        permission.addScope(scope);
        permission.setDecisionStrategy(strategy);
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

        request = new PermissionRequest("Resource C");
        ticket = authzClient.protection().permission().create(request).getTicket();
        response = authzClient.authorization("marta", "password").authorize(new AuthorizationRequest(ticket));
        assertNotNull(response.getToken());

        request = new PermissionRequest("Resource D");
        ticket = authzClient.protection().permission().create(request).getTicket();
        response = authzClient.authorization("marta", "password").authorize(new AuthorizationRequest(ticket));
        assertNotNull(response.getToken());

        request = new PermissionRequest("Resource E");
        ticket = authzClient.protection().permission().create(request).getTicket();
        response = authzClient.authorization("marta", "password").authorize(new AuthorizationRequest(ticket));
        assertNotNull(response.getToken());
    }

    @Test
    public void testWithExpectedUserAttributeValueMappedToComplexJsonClaim() {
        AuthzClient authzClient = getAuthzClient();
        PermissionRequest request = new PermissionRequest("Resource ITEM");
        String ticket = authzClient.protection().permission().create(request).getTicket();
        AuthorizationRequest theRequest = new AuthorizationRequest(ticket);
        AuthorizationResponse response = authzClient.authorization("my-user", "password").authorize(theRequest);
        assertNotNull(response.getToken());

    }

        @Test
    public void testWithExpectedUserAttributeValueMappedToComplexJsonClaimPermissions() {
        AuthzClient authzClient = getAuthzClient();
        PermissionRequest request = new PermissionRequest("service","read");
        String ticket = authzClient.protection().permission().create(request).getTicket();
        AuthorizationRequest theRequest = new AuthorizationRequest(ticket);
        AuthorizationRequest.Metadata metadata = new AuthorizationRequest.Metadata();
        metadata.setResponseMode("permissions");
        metadata.setIncludeResourceName(true);
        metadata.setPermissionResourceFormat("uri");
        theRequest.setMetadata(metadata);
        List<Permission> permissions = authzClient.authorization("admin", "password").getPermissions(theRequest);
        assertNotNull(permissions);
        Assert.assertTrue(permissions.get(0).getResourceName().equals("service"));
        Assert.assertTrue(permissions.get(0).getScopes().contains("read"));
    }



    @Test
    public void testWithNotExpectedUserAttributeValueMappedToComplexJsonClaim() {
        AuthzClient authzClient = getAuthzClient();
        PermissionRequest request = new PermissionRequest("Resource ITEM");
        String ticket = authzClient.protection().permission().create(request).getTicket();
        AuthorizationResponse response= null;
        try {

            response = authzClient.authorization("my-user2", "password").authorize(new AuthorizationRequest(ticket));
            fail("failed because it should thrown an exception with 403 Forbidden Status");
        } catch (AuthorizationDeniedException e) {

        }
    }

    @Test
    public void testWithAbsentUserAttributeThusNotMappedToComplexJsonClaim() {
        AuthzClient authzClient = getAuthzClient();
        PermissionRequest request = new PermissionRequest("Resource ITEM");
        String ticket = authzClient.protection().permission().create(request).getTicket();
            AuthorizationResponse response=null;
            try {
                response = authzClient.authorization("my-user3", "password").authorize(new AuthorizationRequest(ticket));
                fail("failed because it should thrown an exception with 403 Forbidden Status");
            } catch (AuthorizationDeniedException e) {

            }


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

        // Access Resource C with taro.
        request = new PermissionRequest("Resource C");
        ticket = authzClient.protection().permission().create(request).getTicket();
        try {
            authzClient.authorization("taro", "password").authorize(new AuthorizationRequest(ticket));
            fail("Should fail.");
        } catch (AuthorizationDeniedException ignore) {

        }

        // Access Resource D with taro.
        request = new PermissionRequest("Resource D");
        PermissionResponse foo = authzClient.protection().permission().create(request);

        ticket = authzClient.protection().permission().create(request).getTicket();
        try {
            authzClient.authorization("taro", "password").authorize(new AuthorizationRequest(ticket));
            fail("Should fail.");
        } catch (AuthorizationDeniedException ignore) {

        }
    }

    @Test
    public void testWithExpectedContextAttribute() {
        AuthzClient authzClient = getAuthzClient();
        PermissionRequest request = new PermissionRequest("Resource CONTEXT");
        request.setClaim("custom", "foo");
        String ticket = authzClient.protection().permission().create(request).getTicket();
        AuthorizationRequest theRequest = new AuthorizationRequest(ticket);
        AuthorizationResponse response = authzClient.authorization("my-user", "password").authorize(theRequest);
        assertNotNull(response.getToken());
    }

    @Test
    public void testWithExpectedContextAttributeAsUserAttribute() {
        AuthzClient authzClient = getAuthzClient();
        PermissionRequest request = new PermissionRequest("Resource CONTEXT");
        String ticket = authzClient.protection().permission().create(request).getTicket();
        try {
            authzClient.authorization("context-user", "password").authorize(new AuthorizationRequest(ticket));
            fail("failed because it should thrown an exception with 403 Forbidden Status");
        } catch (AuthorizationDeniedException ignored) {

        }
    }

    @Test
    public void testWithoutExpectedContextAttribute() {
        AuthzClient authzClient = getAuthzClient();
        PermissionRequest request = new PermissionRequest("Resource CONTEXT");
        String ticket = authzClient.protection().permission().create(request).getTicket();
        try {
            authzClient.authorization("my-user", "password").authorize(new AuthorizationRequest(ticket));
            fail("failed because it should thrown an exception with 403 Forbidden Status");
        } catch (AuthorizationDeniedException ignored) {

        }
    }

    private AuthzClient getAuthzClient() {
        return AuthzClient.create(getClass().getResourceAsStream("/authorization-test/default-keycloak.json"));
    }

    private ProtocolMapperRepresentation addClaimMapper(String name, Map<String, String> claims) {
        ProtocolMapperRepresentation userAttrBarProtocolMapper = new ProtocolMapperRepresentation();
        userAttrBarProtocolMapper.setName(name);
        userAttrBarProtocolMapper.setProtocolMapper(UserAttributeMapper.PROVIDER_ID);
        userAttrBarProtocolMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> configBar = new HashMap<>(claims);
        configBar.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        configBar.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        configBar.put(OIDCAttributeMapperHelper.JSON_TYPE, "String");
        userAttrBarProtocolMapper.setConfig(configBar);
        return userAttrBarProtocolMapper;
    }
}
