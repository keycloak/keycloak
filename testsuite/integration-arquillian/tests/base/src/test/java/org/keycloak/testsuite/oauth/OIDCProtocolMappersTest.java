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

package org.keycloak.testsuite.oauth;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.Profile;
import org.keycloak.common.util.UriUtils;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.mappers.AddressMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AddressClaimSet;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.UserInfo;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.updaters.ProtocolMappersUpdater;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.ProtocolMapperUtil;
import org.keycloak.testsuite.util.UserInfoClientUtil;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.admin.ApiUtil.findClientByClientId;
import static org.keycloak.testsuite.admin.ApiUtil.findClientResourceByClientId;
import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsernameId;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createAddressMapper;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createClaimMapper;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createHardcodedClaim;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createHardcodedRole;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createRoleNameMapper;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createScriptMapper;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OIDCProtocolMappersTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Before
    public void clientConfiguration() {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").directAccessGrant(true);
        /*
         * Configure the default client ID. Seems like OAuthClient is keeping the state of clientID
         * For example: If some test case configure oauth.clientId("sample-public-client"), other tests
         * will faile and the clientID will always be "sample-public-client
         * @see AccessTokenTest#testAuthorizationNegotiateHeaderIgnored()
         */
        oauth.clientId("test-app");
    }


    private void deleteMappers(ProtocolMappersResource protocolMappers) {
        ProtocolMapperRepresentation mapper = ProtocolMapperUtil.getMapperByNameAndProtocol(protocolMappers, OIDCLoginProtocol.LOGIN_PROTOCOL, "Realm roles mapper");
        if (mapper != null) {
            protocolMappers.delete(mapper.getId());
        }

        mapper = ProtocolMapperUtil.getMapperByNameAndProtocol(protocolMappers, OIDCLoginProtocol.LOGIN_PROTOCOL, "Client roles mapper");
        if (mapper != null) {
            protocolMappers.delete(mapper.getId());
        }

        mapper = ProtocolMapperUtil.getMapperByNameAndProtocol(protocolMappers, OIDCLoginProtocol.LOGIN_PROTOCOL, "group-value");
        if (mapper != null) {
            protocolMappers.delete(mapper.getId());
        }
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realm);
    }

    @Test
    @EnableFeature(value = Profile.Feature.SCRIPTS) // This requires also SCRIPTS feature, therefore we need to restart container
    public void testTokenScriptMapping() throws Exception {
        {
            reconnectAdminClient();
            ClientResource app = findClientResourceByClientId(adminClient.realm("test"), "test-app");

            app.getProtocolMappers().createMapper(createScriptMapper("test-script-mapper1","computed-via-script", "computed-via-script", "String", true, true, "script-scripts/test-script-mapper1.js", false)).close();
            app.getProtocolMappers().createMapper(createScriptMapper("test-script-mapper2","multiValued-via-script", "multiValued-via-script", "String", true, true, "script-scripts/test-script-mapper2.js", true)).close();
            app.getProtocolMappers().createMapper(createScriptMapper("test-script-mapper3","computed-json-via-script", "computed-json-via-script", "JSON", true, true, "script-scripts/test-script-mapper3.js", false)).close();

            Response response = app.getProtocolMappers().createMapper(createScriptMapper("test-script-mapper3", "syntax-error-script", "syntax-error-script", "String", true, true, "script-scripts/test-bad-script-mapper3.js", false));
            assertThat(response.getStatusInfo().getFamily(), is(Response.Status.Family.CLIENT_ERROR));
            response.close();
        }
        {
            OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");
            AccessToken accessToken = oauth.verifyToken(response.getAccessToken());

            assertEquals("hello_test-user@localhost", accessToken.getOtherClaims().get("computed-via-script"));
            assertEquals(Arrays.asList("A","B"), accessToken.getOtherClaims().get("multiValued-via-script"));
            Object o = accessToken.getOtherClaims().get("computed-json-via-script");
            assertTrue("Computed json object should be a map", o instanceof Map);
            Map<String,Object> map = (Map<String,Object>)o;
            assertEquals(map.get("int"), 42);
            assertEquals(map.get("bool"), true);
            assertEquals(map.get("string"), "test");
        }
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testTokenMapping() throws Exception {
        {
            UserResource userResource = findUserByUsernameId(adminClient.realm("test"), "test-user@localhost");
            UserRepresentation user = userResource.toRepresentation();

            user.singleAttribute("street", "5 Yawkey Way");
            user.singleAttribute("locality", "Boston");
            user.singleAttribute("region_some", "MA"); // Custom name for userAttribute name, which will be mapped to region
            user.singleAttribute("postal_code", "02115");
            user.singleAttribute("country", "USA");
            user.singleAttribute("formatted", "6 Foo Street");
            user.singleAttribute("phone", "617-777-6666");
            user.singleAttribute("json-attribute", "{\"a\": 1, \"b\": 2, \"c\": [{\"a\": 1, \"b\": 2}], \"d\": {\"a\": 1, \"b\": 2}}");
            user.getAttributes().put("json-attribute-multi", Arrays.asList("{\"a\": 1, \"b\": 2, \"c\": [{\"a\": 1, \"b\": 2}], \"d\": {\"a\": 1, \"b\": 2}}", "{\"a\": 3, \"b\": 4, \"c\": [{\"a\": 1, \"b\": 2}], \"d\": {\"a\": 1, \"b\": 2}}"));

            List<String> departments = Arrays.asList("finance", "development");
            user.getAttributes().put("departments", departments);
            userResource.update(user);

            ClientResource app = findClientResourceByClientId(adminClient.realm("test"), "test-app");

            ProtocolMapperRepresentation mapper = createAddressMapper(true, true, true);
            mapper.getConfig().put(AddressMapper.getModelPropertyName(AddressClaimSet.REGION), "region_some");
            mapper.getConfig().put(AddressMapper.getModelPropertyName(AddressClaimSet.COUNTRY), "country_some");
            mapper.getConfig().remove(AddressMapper.getModelPropertyName(AddressClaimSet.POSTAL_CODE)); // Even if we remove protocolMapper config property, it should still default to postal_code
            app.getProtocolMappers().createMapper(mapper).close();

            ProtocolMapperRepresentation hard = createHardcodedClaim("hard", "hard", "coded", "String", true, true);
            app.getProtocolMappers().createMapper(hard).close();
            app.getProtocolMappers().createMapper(createHardcodedClaim("hard-nested", "nested.hard", "coded-nested", "String", true, true)).close();
            app.getProtocolMappers().createMapper(createClaimMapper("custom phone", "phone", "home_phone", "String", true, true, true)).close();
            app.getProtocolMappers().createMapper(createClaimMapper("nested phone", "phone", "home.phone", "String", true, true, true)).close();
            app.getProtocolMappers().createMapper(createClaimMapper("dotted phone", "phone", "home\\.phone", "String", true, true, true)).close();
            app.getProtocolMappers().createMapper(createClaimMapper("departments", "departments", "department", "String", true, true, true)).close();
            app.getProtocolMappers().createMapper(createClaimMapper("firstDepartment", "departments", "firstDepartment", "String", true, true, false)).close();
            app.getProtocolMappers().createMapper(createHardcodedRole("hard-realm", "hardcoded")).close();
            app.getProtocolMappers().createMapper(createHardcodedRole("hard-app", "app.hardcoded")).close();
            app.getProtocolMappers().createMapper(createRoleNameMapper("rename-app-role", "test-app.customer-user", "realm-user")).close();
            app.getProtocolMappers().createMapper(createScriptMapper("test-script-mapper1","computed-via-script", "computed-via-script", "String", true, true, "'hello_' + user.username", false)).close();
            app.getProtocolMappers().createMapper(createScriptMapper("test-script-mapper2","multiValued-via-script", "multiValued-via-script", "String", true, true, "new java.util.ArrayList(['A','B'])", true)).close();
            app.getProtocolMappers().createMapper(createClaimMapper("json-attribute-mapper", "json-attribute", "claim-from-json-attribute",
                    "JSON", true, true, false)).close();
            app.getProtocolMappers().createMapper(createClaimMapper("json-attribute-mapper-multi", "json-attribute-multi", "claim-from-json-attribute-multi",
                    "JSON", true, true, true)).close();

            Response response = app.getProtocolMappers().createMapper(createScriptMapper("test-script-mapper3", "syntax-error-script", "syntax-error-script", "String", true, true, "func_tion foo(){ return 'fail';} foo()", false));
            assertThat(response.getStatusInfo().getFamily(), is(Response.Status.Family.CLIENT_ERROR));
            response.close();
        }

        {
            OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");

            IDToken idToken = oauth.verifyIDToken(response.getIdToken());
            assertNotNull(idToken.getAddress());
            assertEquals(idToken.getName(), "Tom Brady");
            assertEquals(idToken.getAddress().getStreetAddress(), "5 Yawkey Way");
            assertEquals(idToken.getAddress().getLocality(), "Boston");
            assertEquals(idToken.getAddress().getRegion(), "MA");
            assertEquals(idToken.getAddress().getPostalCode(), "02115");
            assertNull(idToken.getAddress().getCountry()); // Null because we changed userAttribute name to "country_some", but user contains "country"
            assertEquals(idToken.getAddress().getFormattedAddress(), "6 Foo Street");
            assertNotNull(idToken.getOtherClaims().get("home_phone"));
            assertThat((List<String>) idToken.getOtherClaims().get("home_phone"), hasItems("617-777-6666"));
            assertNotNull(idToken.getOtherClaims().get("home.phone"));
            assertThat((List<String>) idToken.getOtherClaims().get("home.phone"), hasItems("617-777-6666"));
            assertEquals("coded", idToken.getOtherClaims().get("hard"));
            Map nested = (Map) idToken.getOtherClaims().get("nested");
            assertEquals("coded-nested", nested.get("hard"));
            nested = (Map) idToken.getOtherClaims().get("home");
            assertThat((List<String>) nested.get("phone"), hasItems("617-777-6666"));

            List<String> departments = (List<String>) idToken.getOtherClaims().get("department");
            assertThat(departments, containsInAnyOrder("finance", "development"));

            Object firstDepartment = idToken.getOtherClaims().get("firstDepartment");
            assertThat(firstDepartment, instanceOf(String.class));
            assertThat(firstDepartment, anyOf(is("finance"), is("development")));   // Has to be the first item

            Map jsonClaim = (Map) idToken.getOtherClaims().get("claim-from-json-attribute");
            assertThat(jsonClaim.get("a"), instanceOf(int.class));
            assertThat(jsonClaim.get("c"), instanceOf(Collection.class));
            assertThat(jsonClaim.get("d"), instanceOf(Map.class));

            List<Map> jsonClaims = (List<Map>) idToken.getOtherClaims().get("claim-from-json-attribute-multi");
            assertEquals(2, jsonClaims.size());
            assertThat(jsonClaims.get(0).get("a"), instanceOf(int.class));
            assertThat(jsonClaims.get(1).get("c"), instanceOf(Collection.class));
            assertThat(jsonClaims.get(1).get("d"), instanceOf(Map.class));

            AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
            assertEquals(accessToken.getName(), "Tom Brady");
            assertNotNull(accessToken.getAddress());
            assertEquals(accessToken.getAddress().getStreetAddress(), "5 Yawkey Way");
            assertEquals(accessToken.getAddress().getLocality(), "Boston");
            assertEquals(accessToken.getAddress().getRegion(), "MA");
            assertEquals(accessToken.getAddress().getPostalCode(), "02115");
            assertNull(idToken.getAddress().getCountry()); // Null because we changed userAttribute name to "country_some", but user contains "country"
            assertEquals(idToken.getAddress().getFormattedAddress(), "6 Foo Street");
            assertNotNull(accessToken.getOtherClaims().get("home_phone"));
            assertThat((List<String>) accessToken.getOtherClaims().get("home_phone"), hasItems("617-777-6666"));
            assertEquals("coded", accessToken.getOtherClaims().get("hard"));
            nested = (Map) accessToken.getOtherClaims().get("nested");
            assertEquals("coded-nested", nested.get("hard"));
            nested = (Map) accessToken.getOtherClaims().get("home");
            assertThat((List<String>) nested.get("phone"), hasItems("617-777-6666"));
            departments = (List<String>) idToken.getOtherClaims().get("department");
            assertEquals(2, departments.size());
            assertTrue(departments.contains("finance") && departments.contains("development"));
            assertTrue(accessToken.getRealmAccess().getRoles().contains("hardcoded"));
            assertTrue(accessToken.getRealmAccess().getRoles().contains("realm-user"));
            Assert.assertNull(accessToken.getResourceAccess("test-app"));
            assertTrue(accessToken.getResourceAccess("app").getRoles().contains("hardcoded"));

            // Assert audiences added through AudienceResolve mapper
            Assert.assertThat(accessToken.getAudience(), arrayContainingInAnyOrder( "app", "account"));

            // Assert allowed origins
            Assert.assertNames(accessToken.getAllowedOrigins(), "http://localhost:8180", "https://localhost:8543");

            jsonClaim = (Map) accessToken.getOtherClaims().get("claim-from-json-attribute");
            assertThat(jsonClaim.get("a"), instanceOf(int.class));
            assertThat(jsonClaim.get("c"), instanceOf(Collection.class));
            assertThat(jsonClaim.get("d"), instanceOf(Map.class));

            oauth.idTokenHint(response.getIdToken()).openLogout();
        }

        // undo mappers
        {
            ClientResource app = findClientByClientId(adminClient.realm("test"), "test-app");
            ClientRepresentation clientRepresentation = app.toRepresentation();
            for (ProtocolMapperRepresentation model : clientRepresentation.getProtocolMappers()) {
                if (model.getName().equals("address")
                        || model.getName().equals("hard")
                        || model.getName().equals("hard-nested")
                        || model.getName().equals("custom phone")
                        || model.getName().equals("dotted phone")
                        || model.getName().equals("departments")
                        || model.getName().equals("firstDepartment")
                        || model.getName().equals("nested phone")
                        || model.getName().equals("rename-app-role")
                        || model.getName().equals("hard-realm")
                        || model.getName().equals("hard-app")
                        || model.getName().equals("test-script-mapper")
                        || model.getName().equals("json-attribute-mapper")
                        ) {
                    app.getProtocolMappers().delete(model.getId());
                }
            }
        }

        events.clear();


        {
            OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");
            IDToken idToken = oauth.verifyIDToken(response.getIdToken());
            assertNull(idToken.getAddress());
            assertNull(idToken.getOtherClaims().get("home_phone"));
            assertNull(idToken.getOtherClaims().get("hard"));
            assertNull(idToken.getOtherClaims().get("nested"));
            assertNull(idToken.getOtherClaims().get("department"));

            oauth.idTokenHint(response.getIdToken()).openLogout();
        }


        events.clear();
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testTokenPropertiesMapping() throws Exception {
        UserResource userResource = findUserByUsernameId(adminClient.realm("test"), "test-user@localhost");
        UserRepresentation user = userResource.toRepresentation();
        user.singleAttribute("userid", "123456789");
        user.getAttributes().put("useraud", Arrays.asList("test-app", "other"));
        userResource.update(user);

        // create a user attr mapping for some claims that exist as properties in the tokens
        ClientResource app = findClientResourceByClientId(adminClient.realm("test"), "test-app");
        app.getProtocolMappers().createMapper(createClaimMapper("userid-as-sub", "userid", "sub", "String", true, true, false)).close();
        app.getProtocolMappers().createMapper(createClaimMapper("useraud", "useraud", "aud", "String", true, true, true)).close();
        app.getProtocolMappers().createMapper(createHardcodedClaim("website-hardcoded", "website", "http://localhost", "String", true, true)).close();
        app.getProtocolMappers().createMapper(createHardcodedClaim("iat-hardcoded", "iat", "123", "long", true, false)).close();

        // login
        OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");

        // assert mappers work as expected
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());
        assertEquals(user.firstAttribute("userid"), idToken.getSubject());
        assertEquals("http://localhost", idToken.getWebsite());
        assertNotNull(idToken.getAudience());
        assertThat(Arrays.asList(idToken.getAudience()), hasItems("test-app", "other"));

        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertEquals(user.firstAttribute("userid"), accessToken.getSubject());
        assertEquals("http://localhost", accessToken.getWebsite());
        assertNotNull(accessToken.getAudience());
        assertThat(Arrays.asList(accessToken.getAudience()), hasItems("test-app", "other"));
        assertNotEquals(123L, accessToken.getIat().longValue()); // iat should not be modified

        // assert that tokens are also OK in the UserInfo response (hardcoded mappers in IDToken are in UserInfo)
        Client client = AdminClientUtil.createResteasyClient();
        try {
            Response userInfoResponse = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, response.getAccessToken());
            UserInfo userInfo = userInfoResponse.readEntity(UserInfo.class);
            assertEquals(user.firstAttribute("userid"), userInfo.getSubject());
            assertEquals(user.getEmail(), userInfo.getEmail());
            assertEquals(user.getUsername(), userInfo.getPreferredUsername());
            assertEquals(user.getLastName(), userInfo.getFamilyName());
            assertEquals(user.getFirstName(), userInfo.getGivenName());
            assertEquals("http://localhost", userInfo.getWebsite());
            assertNotNull(accessToken.getAudience());
            assertThat(Arrays.asList(accessToken.getAudience()), hasItems("test-app", "other"));
        } finally {
            client.close();
        }

        // logout
        oauth.openLogout();

        // undo mappers
        app = findClientByClientId(adminClient.realm("test"), "test-app");
        ClientRepresentation clientRepresentation = app.toRepresentation();
        for (ProtocolMapperRepresentation model : clientRepresentation.getProtocolMappers()) {
            if (model.getName().equals("userid-as-sub") || model.getName().equals("website-hardcoded")
                    || model.getName().equals("iat-hardcoded") || model.getName().equals("useraud")) {
                app.getProtocolMappers().delete(model.getId());
            }
        }
        events.clear();
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testNullOrEmptyTokenMapping() throws Exception {
        {
            UserResource userResource = findUserByUsernameId(adminClient.realm("test"), "test-user@localhost");
            UserRepresentation user = userResource.toRepresentation();

            user.singleAttribute("empty", "");
            user.singleAttribute("null", null);
            userResource.update(user);

            ClientResource app = findClientResourceByClientId(adminClient.realm("test"), "test-app");
            app.getProtocolMappers().createMapper(createClaimMapper("empty", "empty", "empty", "String", true, true, false)).close();
            app.getProtocolMappers().createMapper(createClaimMapper("null", "null", "null", "String", true, true, false)).close();
        }

        {
            OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");

            IDToken idToken = oauth.verifyIDToken(response.getIdToken());
            Object empty = idToken.getOtherClaims().get("empty");
            assertThat((empty == null ? null : (String) empty), isEmptyOrNullString());
            Object nulll = idToken.getOtherClaims().get("null");
            assertNull(nulll);

            oauth.verifyToken(response.getAccessToken());
            oauth.idTokenHint(response.getIdToken()).openLogout();
        }

        // undo mappers
        {
            ClientResource app = findClientByClientId(adminClient.realm("test"), "test-app");
            ClientRepresentation clientRepresentation = app.toRepresentation();
            for (ProtocolMapperRepresentation model : clientRepresentation.getProtocolMappers()) {
                if (model.getName().equals("empty")
                        || model.getName().equals("null")
                        ) {
                    app.getProtocolMappers().delete(model.getId());
                }
            }
        }

        events.clear();

        {
            OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");
            IDToken idToken = oauth.verifyIDToken(response.getIdToken());
            assertNull(idToken.getAddress());
            assertNull(idToken.getOtherClaims().get("empty"));
            assertNull(idToken.getOtherClaims().get("null"));

            oauth.idTokenHint(response.getIdToken()).openLogout();
        }
        events.clear();
    }



    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testUserRoleToAttributeMappers() throws Exception {
        // Add mapper for realm roles
        ProtocolMapperRepresentation realmMapper = ProtocolMapperUtil.createUserRealmRoleMappingMapper("pref.", "Realm roles mapper", "roles-custom.realm", true, true);
        ProtocolMapperRepresentation clientMapper = ProtocolMapperUtil.createUserClientRoleMappingMapper("test-app", null, "Client roles mapper", "roles-custom.test-app", true, true);

        ProtocolMappersResource protocolMappers = ApiUtil.findClientResourceByClientId(adminClient.realm("test"), "test-app").getProtocolMappers();
        protocolMappers.createMapper(Arrays.asList(realmMapper, clientMapper));

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        // Verify attribute is filled
        Map<String, Object> roleMappings = (Map<String, Object>)idToken.getOtherClaims().get("roles-custom");
        Assert.assertThat(roleMappings.keySet(), containsInAnyOrder("realm", "test-app"));
        String realmRoleMappings = (String) roleMappings.get("realm");
        String testAppMappings = (String) roleMappings.get("test-app");
        assertRolesString(realmRoleMappings,
                "pref.user",                      // from direct assignment in user definition
                "pref.offline_access"             // from direct assignment in user definition
        );
        assertRolesString(testAppMappings,
                "customer-user"                   // from direct assignment in user definition
        );

        // Revert
        deleteMappers(protocolMappers);
    }


    // Test to update protocolMappers to not have roles on the default position (realm_access and resource_access properties)
    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testUserRolesMovedFromAccessTokenProperties() throws Exception {
        RealmResource realm = adminClient.realm("test");
        ClientScopeResource rolesScope = ApiUtil.findClientScopeByName(realm, OIDCLoginProtocolFactory.ROLES_SCOPE);

        // Update builtin protocolMappers to put roles to different position (claim "custom.roles") for both realm and client roles
        ProtocolMapperRepresentation realmRolesMapper = null;
        ProtocolMapperRepresentation clientRolesMapper = null;
        for (ProtocolMapperRepresentation rep : rolesScope.getProtocolMappers().getMappers()) {
            if (OIDCLoginProtocolFactory.REALM_ROLES.equals(rep.getName())) {
                realmRolesMapper = rep;
            } else if (OIDCLoginProtocolFactory.CLIENT_ROLES.equals(rep.getName())) {
                clientRolesMapper = rep;
            }
        }

        String realmRolesTokenClaimOrig = realmRolesMapper.getConfig().get(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME);
        String clientRolesTokenClaimOrig = clientRolesMapper.getConfig().get(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME);

        realmRolesMapper.getConfig().put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "custom.roles");
        rolesScope.getProtocolMappers().update(realmRolesMapper.getId(), realmRolesMapper);
        clientRolesMapper.getConfig().put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "custom.roles");
        rolesScope.getProtocolMappers().update(clientRolesMapper.getId(), clientRolesMapper);

        // Create some hardcoded role mapper
        Response resp = rolesScope.getProtocolMappers().createMapper(createHardcodedRole("hard-realm", "hardcoded"));
        String hardcodedMapperId = ApiUtil.getCreatedId(resp);
        resp.close();

        try {
            OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");
            AccessToken accessToken = oauth.verifyToken(response.getAccessToken());

            // Assert roles are not on their original positions
            Assert.assertNull(accessToken.getRealmAccess());
            Assert.assertTrue(accessToken.getResourceAccess().isEmpty());

            // KEYCLOAK-8481 Assert that accessToken JSON doesn't have "realm_access" or "resource_access" fields in it
            String accessTokenJson = new String(new JWSInput(response.getAccessToken()).getContent(), StandardCharsets.UTF_8);
            Assert.assertFalse(accessTokenJson.contains("realm_access"));
            Assert.assertFalse(accessTokenJson.contains("resource_access"));

            // Assert both realm and client roles on the new position. Hardcoded role should be here as well
            Map<String, Object> cst1 = (Map<String, Object>) accessToken.getOtherClaims().get("custom");
            List<String> roles = (List<String>) cst1.get("roles");
            Assert.assertNames(roles, "offline_access", "user", "customer-user", "hardcoded", AccountRoles.VIEW_PROFILE, AccountRoles.MANAGE_ACCOUNT, AccountRoles.MANAGE_ACCOUNT_LINKS);

            // Assert audience
            Assert.assertNames(Arrays.asList(accessToken.getAudience()), "account");
        } finally {
            // Revert
            rolesScope.getProtocolMappers().delete(hardcodedMapperId);

            realmRolesMapper.getConfig().put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, realmRolesTokenClaimOrig);
            rolesScope.getProtocolMappers().update(realmRolesMapper.getId(), realmRolesMapper);
            clientRolesMapper.getConfig().put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, clientRolesTokenClaimOrig);
            rolesScope.getProtocolMappers().update(clientRolesMapper.getId(), clientRolesMapper);
        }
    }


    @Test
    public void testRolesAndAllowedOriginsRemovedFromAccessToken() throws Exception {
        RealmResource realm = adminClient.realm("test");
        ClientScopeRepresentation allowedOriginsScope = ApiUtil.findClientScopeByName(realm, OIDCLoginProtocolFactory.WEB_ORIGINS_SCOPE).toRepresentation();
        ClientScopeRepresentation rolesScope = ApiUtil.findClientScopeByName(realm, OIDCLoginProtocolFactory.ROLES_SCOPE).toRepresentation();

        // Remove 'roles' and 'web-origins' scope from the client
        ClientResource testApp = ApiUtil.findClientByClientId(realm, "test-app");
        testApp.removeDefaultClientScope(allowedOriginsScope.getId());
        testApp.removeDefaultClientScope(rolesScope.getId());

        try {
            OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");
            AccessToken accessToken = oauth.verifyToken(response.getAccessToken());

            // Assert web origins are not in the token
            Assert.assertNull(accessToken.getAllowedOrigins());

            // Assert roles are not in the token
            Assert.assertNull(accessToken.getRealmAccess());
            Assert.assertTrue(accessToken.getResourceAccess().isEmpty());

            // Assert client not in the token audience. Just in "issuedFor"
            Assert.assertEquals("test-app", accessToken.getIssuedFor());
            Assert.assertFalse(accessToken.hasAudience("test-app"));

            // Assert IDToken still has "test-app" as an audience
            IDToken idToken = oauth.verifyIDToken(response.getIdToken());
            Assert.assertEquals("test-app", idToken.getIssuedFor());
            Assert.assertTrue(idToken.hasAudience("test-app"));
        } finally {
            // Revert
            testApp.addDefaultClientScope(allowedOriginsScope.getId());
            testApp.addDefaultClientScope(rolesScope.getId());
        }
    }


    /**
     * KEYCLOAK-4205
     * @throws Exception
     */
    @Test
    public void testUserRoleToAttributeMappersWithMultiValuedRoles() throws Exception {
        // Add mapper for realm roles
        ProtocolMapperRepresentation realmMapper = ProtocolMapperUtil.createUserRealmRoleMappingMapper("pref.", "Realm roles mapper", "roles-custom.realm", true, true, true);
        ProtocolMapperRepresentation clientMapper = ProtocolMapperUtil.createUserClientRoleMappingMapper("test-app", null, "Client roles mapper", "roles-custom.test-app", true, true, true);

        ProtocolMappersResource protocolMappers = ApiUtil.findClientResourceByClientId(adminClient.realm("test"), "test-app").getProtocolMappers();
        protocolMappers.createMapper(Arrays.asList(realmMapper, clientMapper));

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        // Verify attribute is filled
        Map<String, Object> roleMappings = (Map<String, Object>)idToken.getOtherClaims().get("roles-custom");
        Assert.assertThat(roleMappings.keySet(), containsInAnyOrder("realm", "test-app"));
        Assert.assertThat(roleMappings.get("realm"), CoreMatchers.instanceOf(List.class));
        Assert.assertThat(roleMappings.get("test-app"), CoreMatchers.instanceOf(List.class));

        List<String> realmRoleMappings = (List<String>) roleMappings.get("realm");
        List<String> testAppMappings = (List<String>) roleMappings.get("test-app");
        assertRoles(realmRoleMappings,
                "pref.user",                      // from direct assignment in user definition
                "pref.offline_access"             // from direct assignment in user definition
        );
        assertRoles(testAppMappings,
                "customer-user"                   // from direct assignment in user definition
        );

        // Revert
        deleteMappers(protocolMappers);
    }


    /**
     * KEYCLOAK-5259
     * @throws Exception
     */
    @Test
    public void testUserRoleToAttributeMappersWithFullScopeDisabled() throws Exception {
        // Add mapper for realm roles
        ProtocolMapperRepresentation realmMapper = ProtocolMapperUtil.createUserRealmRoleMappingMapper("pref.", "Realm roles mapper", "roles-custom.realm", true, true, true);
        ProtocolMapperRepresentation clientMapper = ProtocolMapperUtil.createUserClientRoleMappingMapper("test-app", null, "Client roles mapper", "roles-custom.test-app", true, true, true);

        ClientResource client = ApiUtil.findClientResourceByClientId(adminClient.realm("test"), "test-app");

        // Disable full-scope-allowed
        ClientRepresentation rep = client.toRepresentation();
        rep.setFullScopeAllowed(false);
        client.update(rep);

        ProtocolMappersResource protocolMappers = ApiUtil.findClientResourceByClientId(adminClient.realm("test"), "test-app").getProtocolMappers();
        protocolMappers.createMapper(Arrays.asList(realmMapper, clientMapper));

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        // Verify attribute is filled
        Map<String, Object> roleMappings = (Map<String, Object>)idToken.getOtherClaims().get("roles-custom");
        Assert.assertThat(roleMappings.keySet(), containsInAnyOrder("realm", "test-app"));
        Assert.assertThat(roleMappings.get("realm"), CoreMatchers.instanceOf(List.class));
        Assert.assertThat(roleMappings.get("test-app"), CoreMatchers.instanceOf(List.class));

        List<String> realmRoleMappings = (List<String>) roleMappings.get("realm");
        List<String> testAppMappings = (List<String>) roleMappings.get("test-app");
        assertRoles(realmRoleMappings,
                "pref.user"                      // from direct assignment in user definition
        );
        assertRoles(testAppMappings,
                "customer-user"                   // from direct assignment in user definition
        );

        // Revert
        deleteMappers(protocolMappers);

        rep = client.toRepresentation();
        rep.setFullScopeAllowed(true);
        client.update(rep);
    }


    // KEYCLOAK-8148 -- Test the scenario where:
    // -- user is member of 2 groups
    // -- both groups have same role "customer-user" assigned
    // -- User login. Role will appear just once in the token (not twice)
    @Test
    public void testRoleMapperWithRoleInheritedFromMoreGroups() throws Exception {
        // Create client-mapper
        String clientId = "test-app";
        ProtocolMapperRepresentation clientMapper = ProtocolMapperUtil.createUserClientRoleMappingMapper(clientId, null, "Client roles mapper", "roles-custom.test-app", true, true);

        ProtocolMappersResource protocolMappers = ApiUtil.findClientResourceByClientId(adminClient.realm("test"), clientId).getProtocolMappers();
        protocolMappers.createMapper(Arrays.asList(clientMapper));

        // Add user 'level2GroupUser' to the group 'level2Group2'
        GroupRepresentation level2Group2 = adminClient.realm("test").getGroupByPath("/topGroup/level2group2");
        UserResource level2GroupUser = ApiUtil.findUserByUsernameId(adminClient.realm("test"), "level2GroupUser");
        level2GroupUser.joinGroup(level2Group2.getId());

        oauth.clientId(clientId);
        OAuthClient.AccessTokenResponse response = browserLogin("password", "level2GroupUser", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        // Verify attribute is filled AND it is filled only once
        Map<String, Object> roleMappings = (Map<String, Object>)idToken.getOtherClaims().get("roles-custom");
        Assert.assertThat(roleMappings.keySet(), containsInAnyOrder(clientId));
        String testAppScopeMappings = (String) roleMappings.get(clientId);
        assertRolesString(testAppScopeMappings,
                "customer-user"      // from assignment to level2group or level2group2. It is filled just once
        );

        // Revert
        level2GroupUser.leaveGroup(level2Group2.getId());
        deleteMappers(protocolMappers);
    }


    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE)
    public void testUserGroupRoleToAttributeMappers() throws Exception {
        // Add mapper for realm roles
        String clientId = "test-app";
        ProtocolMapperRepresentation realmMapper = ProtocolMapperUtil.createUserRealmRoleMappingMapper("pref.", "Realm roles mapper", "roles-custom.realm", true, true);
        ProtocolMapperRepresentation clientMapper = ProtocolMapperUtil.createUserClientRoleMappingMapper(clientId, "ta.", "Client roles mapper", "roles-custom.test-app", true, true);

        ProtocolMappersResource protocolMappers = ApiUtil.findClientResourceByClientId(adminClient.realm("test"), clientId).getProtocolMappers();
        protocolMappers.createMapper(Arrays.asList(realmMapper, clientMapper));

        // Login user
        OAuthClient.AccessTokenResponse response = browserLogin("password", "rich.roles@redhat.com", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        // Verify attribute is filled
        Map<String, Object> roleMappings = (Map<String, Object>)idToken.getOtherClaims().get("roles-custom");
        Assert.assertThat(roleMappings.keySet(), containsInAnyOrder("realm", clientId));
        String realmRoleMappings = (String) roleMappings.get("realm");
        String testAppMappings = (String) roleMappings.get(clientId);
        assertRolesString(realmRoleMappings,
          "pref.admin",                     // from direct assignment to /roleRichGroup/level2group
          "pref.user",                      // from parent group of /roleRichGroup/level2group, i.e. from /roleRichGroup
          "pref.customer-user-premium",     // from client role customer-admin-composite-role - realm role for test-app
          "pref.realm-composite-role",      // from parent group of /roleRichGroup/level2group, i.e. from /roleRichGroup
          "pref.sample-realm-role"          // from realm role realm-composite-role
        );
        assertRolesString(testAppMappings,
          "ta.customer-user",                  // from direct assignment to /roleRichGroup/level2group
          "ta.customer-admin-composite-role",  // from direct assignment to /roleRichGroup/level2group
          "ta.customer-admin",                 // from client role customer-admin-composite-role - client role for test-app
          "ta.sample-client-role"              // from realm role realm-composite-role - client role for test-app
        );

        // Revert
        deleteMappers(protocolMappers);
    }

    @Test
    public void testUserGroupRoleToAttributeMappersNotScopedOtherApp() throws Exception {
        String clientId = "test-app-authz";
        ProtocolMapperRepresentation realmMapper = ProtocolMapperUtil.createUserRealmRoleMappingMapper("pref.", "Realm roles mapper", "roles-custom.realm", true, true);
        ProtocolMapperRepresentation clientMapper = ProtocolMapperUtil.createUserClientRoleMappingMapper(clientId, null, "Client roles mapper", "roles-custom." + clientId, true, true);

        ProtocolMappersResource protocolMappers = ApiUtil.findClientResourceByClientId(adminClient.realm("test"), clientId).getProtocolMappers();
        protocolMappers.createMapper(Arrays.asList(realmMapper, clientMapper));

        // Login user
        ClientManager.realm(adminClient.realm("test")).clientId(clientId).directAccessGrant(true);
        oauth.clientId(clientId);

        String oldRedirectUri = oauth.getRedirectUri();
        oauth.redirectUri(UriUtils.getOrigin(oldRedirectUri) + "/test-app-authz");

        OAuthClient.AccessTokenResponse response = browserLogin("secret", "rich.roles@redhat.com", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        // revert redirect_uri
        oauth.redirectUri(oldRedirectUri);

        // Verify attribute is filled
        Map<String, Object> roleMappings = (Map<String, Object>)idToken.getOtherClaims().get("roles-custom");
        Assert.assertThat(roleMappings.keySet(), containsInAnyOrder("realm"));
        String realmRoleMappings = (String) roleMappings.get("realm");
        String testAppAuthzMappings = (String) roleMappings.get(clientId);
        assertRolesString(realmRoleMappings,
          "pref.admin",                     // from direct assignment to /roleRichGroup/level2group
          "pref.user",                      // from parent group of /roleRichGroup/level2group, i.e. from /roleRichGroup
          "pref.customer-user-premium",     // from client role customer-admin-composite-role - realm role for test-app
          "pref.realm-composite-role",      // from parent group of /roleRichGroup/level2group, i.e. from /roleRichGroup
          "pref.sample-realm-role"          // from realm role realm-composite-role
        );
        assertNull(testAppAuthzMappings);  // There is no client role defined for test-app-authz

        // Revert
        deleteMappers(protocolMappers);
    }

    @Test
    public void testUserGroupRoleToAttributeMappersScoped() throws Exception {
        String clientId = "test-app-scope";
        ProtocolMapperRepresentation realmMapper = ProtocolMapperUtil.createUserRealmRoleMappingMapper("pref.", "Realm roles mapper", "roles-custom.realm", true, true);
        ProtocolMapperRepresentation clientMapper = ProtocolMapperUtil.createUserClientRoleMappingMapper(clientId, null, "Client roles mapper", "roles-custom.test-app-scope", true, true);

        ProtocolMappersResource protocolMappers = ApiUtil.findClientResourceByClientId(adminClient.realm("test"), clientId).getProtocolMappers();
        protocolMappers.createMapper(Arrays.asList(realmMapper, clientMapper));

        // Login user
        ClientManager.realm(adminClient.realm("test")).clientId(clientId).directAccessGrant(true);
        oauth.clientId(clientId);
        OAuthClient.AccessTokenResponse response = browserLogin("password", "rich.roles@redhat.com", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        // Verify attribute is filled
        Map<String, Object> roleMappings = (Map<String, Object>)idToken.getOtherClaims().get("roles-custom");
        Assert.assertThat(roleMappings.keySet(), containsInAnyOrder("realm", clientId));
        String realmRoleMappings = (String) roleMappings.get("realm");
        String testAppScopeMappings = (String) roleMappings.get(clientId);
        assertRolesString(realmRoleMappings,
          "pref.admin",                     // from direct assignment to /roleRichGroup/level2group
          "pref.user",                       // from parent group of /roleRichGroup/level2group, i.e. from /roleRichGroup
          "pref.customer-user-premium"
        );
        assertRolesString(testAppScopeMappings,
          "test-app-allowed-by-scope",       // from direct assignment to roleRichUser, present as scope allows it
                "test-app-disallowed-by-scope"
        );

        // Revert
        deleteMappers(protocolMappers);
    }

    @Test
    public void testUserGroupRoleToAttributeMappersScopedClientNotSet() throws Exception {
        String clientId = "test-app-scope";
        ProtocolMapperRepresentation realmMapper = ProtocolMapperUtil.createUserRealmRoleMappingMapper("pref.", "Realm roles mapper", "roles-custom.realm", true, true);
        ProtocolMapperRepresentation clientMapper = ProtocolMapperUtil.createUserClientRoleMappingMapper(null, null, "Client roles mapper", "roles-custom.test-app-scope", true, true);

        ProtocolMappersResource protocolMappers = ApiUtil.findClientResourceByClientId(adminClient.realm("test"), clientId).getProtocolMappers();
        protocolMappers.createMapper(Arrays.asList(realmMapper, clientMapper));

        // Login user
        ClientManager.realm(adminClient.realm("test")).clientId(clientId).directAccessGrant(true);
        oauth.clientId(clientId);
        OAuthClient.AccessTokenResponse response = browserLogin("password", "rich.roles@redhat.com", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        // Verify attribute is filled
        Map<String, Object> roleMappings = (Map<String, Object>)idToken.getOtherClaims().get("roles-custom");
        Assert.assertThat(roleMappings.keySet(), containsInAnyOrder("realm", clientId));
        String realmRoleMappings = (String) roleMappings.get("realm");
        String testAppScopeMappings = (String) roleMappings.get(clientId);
        assertRolesString(realmRoleMappings,
          "pref.admin",                     // from direct assignment to /roleRichGroup/level2group
          "pref.user",  // from parent group of /roleRichGroup/level2group, i.e. from /roleRichGroup
          "pref.customer-user-premium"
        );
        assertRolesString(testAppScopeMappings,
          "test-app-allowed-by-scope",      // from direct assignment to roleRichUser, present as scope allows it
          "test-app-disallowed-by-scope"   // from direct assignment to /roleRichGroup/level2group, present as scope allows it
        );

        // Revert
        deleteMappers(protocolMappers);
    }

    @Test
    public void testUserGroupRoleToAttributeMappersScopedWithDifferentClient() throws Exception {
        final String clientId = "test-app-scope";
        final String diffClient = "test-app";
        final String realmName = "test";

        final ProtocolMapperRepresentation realmMapper = ProtocolMapperUtil.createUserRealmRoleMappingMapper("pref.", "Realm roles mapper", "roles-custom.realm", true, true);
        final ProtocolMapperRepresentation clientMapper = ProtocolMapperUtil.createUserClientRoleMappingMapper(diffClient, null, "Client roles mapper", "roles-custom.test-app", true, true);

        try (ClientAttributeUpdater cau = ClientAttributeUpdater.forClient(adminClient, realmName, clientId).setDirectAccessGrantsEnabled(true);
             ProtocolMappersUpdater protocolMappers = new ProtocolMappersUpdater(cau.getResource().getProtocolMappers())) {

            protocolMappers.add(realmMapper, clientMapper).update();

            // Login user
            oauth.clientId(clientId);
            OAuthClient.AccessTokenResponse response = browserLogin("password", "rich.roles@redhat.com", "password");
            IDToken idToken = oauth.verifyIDToken(response.getIdToken());

            // Verify attribute is filled
            Map<String, Object> roleMappings = (Map<String, Object>) idToken.getOtherClaims().get("roles-custom");
            assertNotNull(roleMappings);
            assertThat(roleMappings.keySet(), containsInAnyOrder("realm", diffClient));
            String realmRoleMappings = (String) roleMappings.get("realm");
            String testAppScopeMappings = (String) roleMappings.get(diffClient);
            assertRolesString(realmRoleMappings,
                    "pref.admin",
                    "pref.user",
                    "pref.customer-user-premium"
            );
            assertRolesString(testAppScopeMappings,
                    "customer-admin-composite-role",
                    "customer-admin"
            );
        }
    }

    @Test
    public void testGroupAttributeUserOneGroupNoMultivalueNoAggregate() throws Exception {
        // get the user
        UserResource userResource = findUserByUsernameId(adminClient.realm("test"), "test-user@localhost");
        UserRepresentation user = userResource.toRepresentation();
        user.setAttributes(new HashMap<>());
        user.getAttributes().put("group-value", Arrays.asList("user-value1", "user-value2"));
        userResource.update(user);
        // create a group1 with two values
        GroupRepresentation group1 = new GroupRepresentation();
        group1.setName("group1");
        group1.setAttributes(new HashMap<>());
        group1.getAttributes().put("group-value", Arrays.asList("value1", "value2"));
        adminClient.realm("test").groups().add(group1);
        group1 = adminClient.realm("test").getGroupByPath("/group1");
        userResource.joinGroup(group1.getId());
        // create the attribute mapper
        ProtocolMappersResource protocolMappers = findClientResourceByClientId(adminClient.realm("test"), "test-app").getProtocolMappers();
        protocolMappers.createMapper(createClaimMapper("group-value", "group-value", "group-value", "String", true, true, false, false)).close();

        try {
            // test it
            OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");

            IDToken idToken = oauth.verifyIDToken(response.getIdToken());
            assertNotNull(idToken.getOtherClaims());
            assertNotNull(idToken.getOtherClaims().get("group-value"));
            assertTrue(idToken.getOtherClaims().get("group-value") instanceof String);
            assertTrue("user-value1".equals(idToken.getOtherClaims().get("group-value")) ||
                    "user-value2".equals(idToken.getOtherClaims().get("group-value")));
        } finally {
            // revert
            user.getAttributes().remove("group-value");
            userResource.update(user);
            userResource.leaveGroup(group1.getId());
            adminClient.realm("test").groups().group(group1.getId()).remove();
            deleteMappers(protocolMappers);
        }
    }

    @Test
    public void testGroupAttributeUserOneGroupMultivalueNoAggregate() throws Exception {
        // get the user
        UserResource userResource = findUserByUsernameId(adminClient.realm("test"), "test-user@localhost");
        UserRepresentation user = userResource.toRepresentation();
        user.setAttributes(new HashMap<>());
        user.getAttributes().put("group-value", Arrays.asList("user-value1", "user-value2"));
        userResource.update(user);
        // create a group1 with two values
        GroupRepresentation group1 = new GroupRepresentation();
        group1.setName("group1");
        group1.setAttributes(new HashMap<>());
        group1.getAttributes().put("group-value", Arrays.asList("value1", "value2"));
        adminClient.realm("test").groups().add(group1);
        group1 = adminClient.realm("test").getGroupByPath("/group1");
        userResource.joinGroup(group1.getId());
        // create the attribute mapper
        ProtocolMappersResource protocolMappers = findClientResourceByClientId(adminClient.realm("test"), "test-app").getProtocolMappers();
        protocolMappers.createMapper(createClaimMapper("group-value", "group-value", "group-value", "String", true, true, true, false)).close();

        try {
            // test it
            OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");

            IDToken idToken = oauth.verifyIDToken(response.getIdToken());
            assertNotNull(idToken.getOtherClaims());
            assertNotNull(idToken.getOtherClaims().get("group-value"));
            assertTrue(idToken.getOtherClaims().get("group-value") instanceof List);
            assertEquals(2, ((List) idToken.getOtherClaims().get("group-value")).size());
            assertTrue(((List) idToken.getOtherClaims().get("group-value")).contains("user-value1"));
            assertTrue(((List) idToken.getOtherClaims().get("group-value")).contains("user-value2"));
        } finally {
            // revert
            user.getAttributes().remove("group-value");
            userResource.update(user);
            userResource.leaveGroup(group1.getId());
            adminClient.realm("test").groups().group(group1.getId()).remove();
            deleteMappers(protocolMappers);
        }
    }

    @Test
    public void testGroupAttributeUserOneGroupMultivalueAggregate() throws Exception {
        // get the user
        UserResource userResource = findUserByUsernameId(adminClient.realm("test"), "test-user@localhost");
        UserRepresentation user = userResource.toRepresentation();
        user.setAttributes(new HashMap<>());
        user.getAttributes().put("group-value", Arrays.asList("user-value1", "user-value2"));
        userResource.update(user);
        // create a group1 with two values
        GroupRepresentation group1 = new GroupRepresentation();
        group1.setName("group1");
        group1.setAttributes(new HashMap<>());
        group1.getAttributes().put("group-value", Arrays.asList("value1", "value2"));
        adminClient.realm("test").groups().add(group1);
        group1 = adminClient.realm("test").getGroupByPath("/group1");
        userResource.joinGroup(group1.getId());
        // create the attribute mapper
        ProtocolMappersResource protocolMappers = findClientResourceByClientId(adminClient.realm("test"), "test-app").getProtocolMappers();
        protocolMappers.createMapper(createClaimMapper("group-value", "group-value", "group-value", "String", true, true, true, true)).close();

        try {
            // test it
            OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");

            IDToken idToken = oauth.verifyIDToken(response.getIdToken());
            assertNotNull(idToken.getOtherClaims());
            assertNotNull(idToken.getOtherClaims().get("group-value"));
            assertTrue(idToken.getOtherClaims().get("group-value") instanceof List);
            assertEquals(4, ((List) idToken.getOtherClaims().get("group-value")).size());
            assertTrue(((List) idToken.getOtherClaims().get("group-value")).contains("user-value1"));
            assertTrue(((List) idToken.getOtherClaims().get("group-value")).contains("user-value2"));
            assertTrue(((List) idToken.getOtherClaims().get("group-value")).contains("value1"));
            assertTrue(((List) idToken.getOtherClaims().get("group-value")).contains("value2"));
        } finally {
            // revert
            user.getAttributes().remove("group-value");
            userResource.update(user);
            userResource.leaveGroup(group1.getId());
            adminClient.realm("test").groups().group(group1.getId()).remove();
            deleteMappers(protocolMappers);
        }
    }

    @Test
    public void testGroupAttributeOneGroupNoMultivalueNoAggregate() throws Exception {
        // get the user
        UserResource userResource = findUserByUsernameId(adminClient.realm("test"), "test-user@localhost");
        // create a group1 with two values
        GroupRepresentation group1 = new GroupRepresentation();
        group1.setName("group1");
        group1.setAttributes(new HashMap<>());
        group1.getAttributes().put("group-value", Arrays.asList("value1", "value2"));
        adminClient.realm("test").groups().add(group1);
        group1 = adminClient.realm("test").getGroupByPath("/group1");
        userResource.joinGroup(group1.getId());
        // create the attribute mapper
        ProtocolMappersResource protocolMappers = findClientResourceByClientId(adminClient.realm("test"), "test-app").getProtocolMappers();
        protocolMappers.createMapper(createClaimMapper("group-value", "group-value", "group-value", "String", true, true, false, false)).close();

        try {
            // test it
            OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");

            IDToken idToken = oauth.verifyIDToken(response.getIdToken());
            assertNotNull(idToken.getOtherClaims());
            assertNotNull(idToken.getOtherClaims().get("group-value"));
            assertTrue(idToken.getOtherClaims().get("group-value") instanceof String);
            assertTrue("value1".equals(idToken.getOtherClaims().get("group-value"))
                    || "value2".equals(idToken.getOtherClaims().get("group-value")));
        } finally {
            // revert
            userResource.leaveGroup(group1.getId());
            adminClient.realm("test").groups().group(group1.getId()).remove();
            deleteMappers(protocolMappers);
        }
    }

    @Test
    public void testGroupAttributeOneGroupMultiValueNoAggregate() throws Exception {
        // get the user
        UserResource userResource = findUserByUsernameId(adminClient.realm("test"), "test-user@localhost");
        // create a group1 with two values
        GroupRepresentation group1 = new GroupRepresentation();
        group1.setName("group1");
        group1.setAttributes(new HashMap<>());
        group1.getAttributes().put("group-value", Arrays.asList("value1", "value2"));
        adminClient.realm("test").groups().add(group1);
        group1 = adminClient.realm("test").getGroupByPath("/group1");
        userResource.joinGroup(group1.getId());

        // create the attribute mapper
        ProtocolMappersResource protocolMappers = findClientResourceByClientId(adminClient.realm("test"), "test-app").getProtocolMappers();
        protocolMappers.createMapper(createClaimMapper("group-value", "group-value", "group-value", "String", true, true, true, false)).close();

        try {
            // test it
            OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");

            IDToken idToken = oauth.verifyIDToken(response.getIdToken());
            assertNotNull(idToken.getOtherClaims());
            assertNotNull(idToken.getOtherClaims().get("group-value"));
            assertTrue(idToken.getOtherClaims().get("group-value") instanceof List);
            assertEquals(2, ((List) idToken.getOtherClaims().get("group-value")).size());
            assertTrue(((List) idToken.getOtherClaims().get("group-value")).contains("value1"));
            assertTrue(((List) idToken.getOtherClaims().get("group-value")).contains("value2"));
        } finally {
            // revert
            userResource.leaveGroup(group1.getId());
            adminClient.realm("test").groups().group(group1.getId()).remove();
            deleteMappers(protocolMappers);
        }
    }

    @Test
    public void testGroupAttributeOneGroupMultiValueAggregate() throws Exception {
        // get the user
        UserResource userResource = findUserByUsernameId(adminClient.realm("test"), "test-user@localhost");
        // create a group1 with two values
        GroupRepresentation group1 = new GroupRepresentation();
        group1.setName("group1");
        group1.setAttributes(new HashMap<>());
        group1.getAttributes().put("group-value", Arrays.asList("value1", "value2"));
        adminClient.realm("test").groups().add(group1);
        group1 = adminClient.realm("test").getGroupByPath("/group1");
        userResource.joinGroup(group1.getId());

        // create the attribute mapper
        ProtocolMappersResource protocolMappers = findClientResourceByClientId(adminClient.realm("test"), "test-app").getProtocolMappers();
        protocolMappers.createMapper(createClaimMapper("group-value", "group-value", "group-value", "String", true, true, true, true)).close();

        try {
            // test it
            OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");

            IDToken idToken = oauth.verifyIDToken(response.getIdToken());
            assertNotNull(idToken.getOtherClaims());
            assertNotNull(idToken.getOtherClaims().get("group-value"));
            assertTrue(idToken.getOtherClaims().get("group-value") instanceof List);
            assertEquals(2, ((List) idToken.getOtherClaims().get("group-value")).size());
            assertTrue(((List) idToken.getOtherClaims().get("group-value")).contains("value1"));
            assertTrue(((List) idToken.getOtherClaims().get("group-value")).contains("value2"));
        } finally {
            // revert
            userResource.leaveGroup(group1.getId());
            adminClient.realm("test").groups().group(group1.getId()).remove();
            deleteMappers(protocolMappers);
        }
    }

    @Test
    public void testGroupAttributeTwoGroupNoMultivalueNoAggregate() throws Exception {
        // get the user
        UserResource userResource = findUserByUsernameId(adminClient.realm("test"), "test-user@localhost");
        // create two groups with two values (one is the same value)
        GroupRepresentation group1 = new GroupRepresentation();
        group1.setName("group1");
        group1.setAttributes(new HashMap<>());
        group1.getAttributes().put("group-value", Arrays.asList("value1", "value2"));
        adminClient.realm("test").groups().add(group1);
        group1 = adminClient.realm("test").getGroupByPath("/group1");
        userResource.joinGroup(group1.getId());
        GroupRepresentation group2 = new GroupRepresentation();
        group2.setName("group2");
        group2.setAttributes(new HashMap<>());
        group2.getAttributes().put("group-value", Arrays.asList("value2", "value3"));
        adminClient.realm("test").groups().add(group2);
        group2 = adminClient.realm("test").getGroupByPath("/group2");
        userResource.joinGroup(group2.getId());

        // create the attribute mapper
        ProtocolMappersResource protocolMappers = findClientResourceByClientId(adminClient.realm("test"), "test-app").getProtocolMappers();
        protocolMappers.createMapper(createClaimMapper("group-value", "group-value", "group-value", "String", true, true, false, false)).close();

        try {
            // test it
            OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");

            IDToken idToken = oauth.verifyIDToken(response.getIdToken());
            assertNotNull(idToken.getOtherClaims());
            assertNotNull(idToken.getOtherClaims().get("group-value"));
            assertTrue(idToken.getOtherClaims().get("group-value") instanceof String);
            assertTrue("value1".equals(idToken.getOtherClaims().get("group-value"))
                    || "value2".equals(idToken.getOtherClaims().get("group-value"))
                    || "value3".equals(idToken.getOtherClaims().get("group-value")));
        } finally {
            // revert
            userResource.leaveGroup(group1.getId());
            adminClient.realm("test").groups().group(group1.getId()).remove();
            userResource.leaveGroup(group2.getId());
            adminClient.realm("test").groups().group(group2.getId()).remove();
            deleteMappers(protocolMappers);
        }
    }

    @Test
    public void testGroupAttributeTwoGroupMultiValueNoAggregate() throws Exception {
        // get the user
        UserResource userResource = findUserByUsernameId(adminClient.realm("test"), "test-user@localhost");
        // create two groups with two values (one is the same value)
        GroupRepresentation group1 = new GroupRepresentation();
        group1.setName("group1");
        group1.setAttributes(new HashMap<>());
        group1.getAttributes().put("group-value", Arrays.asList("value1", "value2"));
        adminClient.realm("test").groups().add(group1);
        group1 = adminClient.realm("test").getGroupByPath("/group1");
        userResource.joinGroup(group1.getId());
        GroupRepresentation group2 = new GroupRepresentation();
        group2.setName("group2");
        group2.setAttributes(new HashMap<>());
        group2.getAttributes().put("group-value", Arrays.asList("value2", "value3"));
        adminClient.realm("test").groups().add(group2);
        group2 = adminClient.realm("test").getGroupByPath("/group2");
        userResource.joinGroup(group2.getId());

        // create the attribute mapper
        ProtocolMappersResource protocolMappers = findClientResourceByClientId(adminClient.realm("test"), "test-app").getProtocolMappers();
        protocolMappers.createMapper(createClaimMapper("group-value", "group-value", "group-value", "String", true, true, true, false)).close();

        try {
            // test it
            OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");

            IDToken idToken = oauth.verifyIDToken(response.getIdToken());
            assertNotNull(idToken.getOtherClaims());
            assertNotNull(idToken.getOtherClaims().get("group-value"));
            assertTrue(idToken.getOtherClaims().get("group-value") instanceof List);
            assertEquals(2, ((List) idToken.getOtherClaims().get("group-value")).size());
            assertTrue((((List) idToken.getOtherClaims().get("group-value")).contains("value1")
                    && ((List) idToken.getOtherClaims().get("group-value")).contains("value2"))
                    || (((List) idToken.getOtherClaims().get("group-value")).contains("value2")
                    && ((List) idToken.getOtherClaims().get("group-value")).contains("value3")));
        } finally {
            // revert
            userResource.leaveGroup(group1.getId());
            adminClient.realm("test").groups().group(group1.getId()).remove();
            userResource.leaveGroup(group2.getId());
            adminClient.realm("test").groups().group(group2.getId()).remove();
            deleteMappers(protocolMappers);
        }
    }

    @Test
    public void testGroupAttributeTwoGroupMultiValueAggregate() throws Exception {
        // get the user
        UserResource userResource = findUserByUsernameId(adminClient.realm("test"), "test-user@localhost");
        // create two groups with two values (one is the same value)
        GroupRepresentation group1 = new GroupRepresentation();
        group1.setName("group1");
        group1.setAttributes(new HashMap<>());
        group1.getAttributes().put("group-value", Arrays.asList("value1", "value2"));
        adminClient.realm("test").groups().add(group1);
        group1 = adminClient.realm("test").getGroupByPath("/group1");
        userResource.joinGroup(group1.getId());
        GroupRepresentation group2 = new GroupRepresentation();
        group2.setName("group2");
        group2.setAttributes(new HashMap<>());
        group2.getAttributes().put("group-value", Arrays.asList("value2", "value3"));
        adminClient.realm("test").groups().add(group2);
        group2 = adminClient.realm("test").getGroupByPath("/group2");
        userResource.joinGroup(group2.getId());

        // create the attribute mapper
        ProtocolMappersResource protocolMappers = findClientResourceByClientId(adminClient.realm("test"), "test-app").getProtocolMappers();
        protocolMappers.createMapper(createClaimMapper("group-value", "group-value", "group-value", "String", true, true, true, true)).close();

        try {
            // test it
            OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");

            IDToken idToken = oauth.verifyIDToken(response.getIdToken());
            assertNotNull(idToken.getOtherClaims());
            assertNotNull(idToken.getOtherClaims().get("group-value"));
            assertTrue(idToken.getOtherClaims().get("group-value") instanceof List);
            assertEquals(3, ((List) idToken.getOtherClaims().get("group-value")).size());
            assertTrue(((List) idToken.getOtherClaims().get("group-value")).contains("value1"));
            assertTrue(((List) idToken.getOtherClaims().get("group-value")).contains("value2"));
            assertTrue(((List) idToken.getOtherClaims().get("group-value")).contains("value3"));
        } finally {
            // revert
            userResource.leaveGroup(group1.getId());
            adminClient.realm("test").groups().group(group1.getId()).remove();
            userResource.leaveGroup(group2.getId());
            adminClient.realm("test").groups().group(group2.getId()).remove();
            deleteMappers(protocolMappers);
        }
    }

    @Test
    @EnableFeature(value = Profile.Feature.DYNAMIC_SCOPES, skipRestart = true)
    public void executeTokenMappersOnDynamicScopes() {
        ClientResource clientResource = findClientResourceByClientId(adminClient.realm("test"), "test-app");
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("dyn-scope-with-mapper");
        scopeRep.setProtocol("openid-connect");
        scopeRep.setAttributes(new HashMap<String, String>() {{
            put(ClientScopeModel.IS_DYNAMIC_SCOPE, "true");
            put(ClientScopeModel.DYNAMIC_SCOPE_REGEXP, "dyn-scope-with-mapper:*");
        }});
        // create the attribute mapper
        ProtocolMapperRepresentation protocolMapperRepresentation = createHardcodedClaim("dynamic-scope-hardcoded-mapper", "hardcoded-foo", "hardcoded-bar", "String", true, true);
        scopeRep.setProtocolMappers(Collections.singletonList(protocolMapperRepresentation));

        try (Response resp = adminClient.realm("test").clientScopes().create(scopeRep)) {
            assertEquals(201, resp.getStatus());
            String clientScopeId = ApiUtil.getCreatedId(resp);
            getCleanup().addClientScopeId(clientScopeId);
            clientResource.addOptionalClientScope(clientScopeId);
        }

        oauth.scope("openid dyn-scope-with-mapper:value");
        OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());

        assertNotNull(idToken.getOtherClaims());
        assertNotNull(idToken.getOtherClaims().get("hardcoded-foo"));
        assertTrue(idToken.getOtherClaims().get("hardcoded-foo") instanceof String);
        assertEquals("hardcoded-bar", idToken.getOtherClaims().get("hardcoded-foo"));

        assertNotNull(accessToken.getOtherClaims());
        assertNotNull(accessToken.getOtherClaims().get("hardcoded-foo"));
        assertTrue(accessToken.getOtherClaims().get("hardcoded-foo") instanceof String);
        assertEquals("hardcoded-bar", accessToken.getOtherClaims().get("hardcoded-foo"));
    }

    private void assertRoles(List<String> actualRoleList, String ...expectedRoles){
        Assert.assertNames(actualRoleList, expectedRoles);
    }

    private void assertRolesString(String actualRoleString, String...expectedRoles) {

        Assert.assertThat(actualRoleString.matches("^\\[.*\\]$"), is(true));
        String[] roles = actualRoleString.substring(1, actualRoleString.length() - 1).split(",\\s*");

        if (expectedRoles == null || expectedRoles.length == 0) {
            Assert.assertThat(roles, arrayContainingInAnyOrder(""));
        } else {
            Assert.assertThat(roles, arrayContainingInAnyOrder(expectedRoles));
        }
    }


    private ProtocolMapperRepresentation makeMapper(String name, String mapperType, Map<String, String> config) {
        ProtocolMapperRepresentation rep = new ProtocolMapperRepresentation();
        rep.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        rep.setName(name);
        rep.setProtocolMapper(mapperType);
        rep.setConfig(config);
        return rep;
    }

    private OAuthClient.AccessTokenResponse browserLogin(String clientSecret, String username, String password) {
        OAuthClient.AuthorizationEndpointResponse authzEndpointResponse = oauth.doLogin(username, password);
        return oauth.doAccessTokenRequest(authzEndpointResponse.getCode(), clientSecret);
    }

}
