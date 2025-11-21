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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserProfileResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.Profile;
import org.keycloak.common.util.UriUtils;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.mappers.AddressMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.UserPropertyMapper;
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
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.updaters.ProtocolMappersUpdater;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.ProtocolMapperUtil;
import org.keycloak.testsuite.util.UserInfoClientUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.userprofile.UserProfileUtil;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.OAuth2Constants.SCOPE_PROFILE;
import static org.keycloak.testsuite.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.admin.ApiUtil.findClientByClientId;
import static org.keycloak.testsuite.admin.ApiUtil.findClientResourceByClientId;
import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsernameId;
import static org.keycloak.testsuite.admin.ApiUtil.getCreatedId;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createAddressMapper;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createClaimMapper;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createHardcodedClaim;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createHardcodedRole;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createRoleNameMapper;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createScriptMapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
        oauth.client("test-app", "password");

        // enable user profile unmanaged attributes
        UserProfileResource upResource = adminClient.realm("test").users().userProfile();
        UserProfileUtil.enableUnmanagedAttributes(upResource);
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

            app.getProtocolMappers().createMapper(createScriptMapper("test-script-mapper1","computed-via-script", "computed-via-script", "String", true, true, true,"script-scripts/test-script-mapper1.js", false)).close();
            app.getProtocolMappers().createMapper(createScriptMapper("test-script-mapper2","multiValued-via-script", "multiValued-via-script", "String", true, true, true, "script-scripts/test-script-mapper2.js", true)).close();
            app.getProtocolMappers().createMapper(createScriptMapper("test-script-mapper3","computed-json-via-script", "computed-json-via-script", "JSON", true, true, true, "script-scripts/test-script-mapper3.js", false)).close();

            Response response = app.getProtocolMappers().createMapper(createScriptMapper("test-script-mapper3", "syntax-error-script", "syntax-error-script", "String", true, true, true, "script-scripts/test-bad-script-mapper3.js", false));
            assertThat(response.getStatusInfo().getFamily(), is(Response.Status.Family.CLIENT_ERROR));
            response.close();
        }
        {
            AccessTokenResponse response = browserLogin("test-user@localhost", "password");
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
            user.getAttributes().put("multi1", Stream.of("abc","bcd").collect(Collectors.toList()));
            user.getAttributes().put("multi2", Stream.of("abc","cde").collect(Collectors.toList()));
            user.singleAttribute("json-attribute", "{\"a\": 1, \"b\": 2, \"c\": [{\"a\": 1, \"b\": 2}], \"d\": {\"a\": 1, \"b\": 2}}");
            user.getAttributes().put("json-attribute-multi", Arrays.asList("{\"a\": 1, \"b\": 2, \"c\": [{\"a\": 1, \"b\": 2}], \"d\": {\"a\": 1, \"b\": 2}}", "{\"a\": 3, \"b\": 4, \"c\": [{\"a\": 1, \"b\": 2}], \"d\": {\"a\": 1, \"b\": 2}}"));

            List<String> departments = Arrays.asList("finance", "development");
            user.getAttributes().put("departments", departments);
            userResource.update(user);

            ClientResource app = findClientResourceByClientId(adminClient.realm("test"), "test-app");

            ProtocolMapperRepresentation mapper = createAddressMapper(true, true, true, true);
            mapper.getConfig().put(AddressMapper.getModelPropertyName(AddressClaimSet.REGION), "region_some");
            mapper.getConfig().put(AddressMapper.getModelPropertyName(AddressClaimSet.COUNTRY), "country_some");
            mapper.getConfig().remove(AddressMapper.getModelPropertyName(AddressClaimSet.POSTAL_CODE)); // Even if we remove protocolMapper config property, it should still default to postal_code
            app.getProtocolMappers().createMapper(mapper).close();

            ProtocolMapperRepresentation hard = createHardcodedClaim("hard", "hard", "coded", "String", true, true, true);
            app.getProtocolMappers().createMapper(hard).close();
            app.getProtocolMappers().createMapper(createHardcodedClaim("hard-nested", "nested.hard", "coded-nested", "String", true, true, true)).close();
            app.getProtocolMappers().createMapper(createClaimMapper("custom phone", "phone", "home_phone", "String", true, true, true, true)).close();
            app.getProtocolMappers().createMapper(createClaimMapper("nested phone", "phone", "home.phone", "String", true, true, true, true)).close();
            app.getProtocolMappers().createMapper(createClaimMapper("dotted phone", "phone", "home\\.phone", "String", true, true, true, true)).close();
            app.getProtocolMappers().createMapper(createClaimMapper("departments", "departments", "department", "String", true, true, true, true)).close();
            app.getProtocolMappers().createMapper(createClaimMapper("multi1", "multi1", "multi", "String", true, true, true, true)).close();
            app.getProtocolMappers().createMapper(createClaimMapper("multi2", "multi2", "multi", "String", true, true, true, true)).close();
            app.getProtocolMappers().createMapper(createClaimMapper("firstDepartment", "departments", "firstDepartment", "String", true, true, true,false)).close();
            app.getProtocolMappers().createMapper(createHardcodedRole("hard-realm", "hardcoded")).close();
            app.getProtocolMappers().createMapper(createHardcodedRole("hard-app", "app.hardcoded")).close();
            app.getProtocolMappers().createMapper(createRoleNameMapper("rename-app-role", "test-app.customer-user", "realm-user")).close();
            app.getProtocolMappers().createMapper(createScriptMapper("test-script-mapper1","computed-via-script", "computed-via-script", "String", true, true, true, "'hello_' + user.username", false)).close();
            app.getProtocolMappers().createMapper(createScriptMapper("test-script-mapper2","multiValued-via-script", "multiValued-via-script", "String", true, true, true, "new java.util.ArrayList(['A','B'])", true)).close();
            app.getProtocolMappers().createMapper(createClaimMapper("json-attribute-mapper", "json-attribute", "claim-from-json-attribute",
                    "JSON", true, true, true, false)).close();
            app.getProtocolMappers().createMapper(createClaimMapper("json-attribute-mapper-multi", "json-attribute-multi", "claim-from-json-attribute-multi",
                    "JSON", true, true, true, true)).close();

            Response response = app.getProtocolMappers().createMapper(createScriptMapper("test-script-mapper3", "syntax-error-script", "syntax-error-script", "String", true, true, true, "func_tion foo(){ return 'fail';} foo()", false));
            assertThat(response.getStatusInfo().getFamily(), is(Response.Status.Family.CLIENT_ERROR));
            response.close();
        }

        {
            AccessTokenResponse response = browserLogin("test-user@localhost", "password");

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
            assertThat(accessToken.getAudience(), arrayContainingInAnyOrder( "app", "account"));

            // Assert allowed origins
            Assert.assertNames(accessToken.getAllowedOrigins(), "http://localhost:8180", "https://localhost:8543");

            jsonClaim = (Map) accessToken.getOtherClaims().get("claim-from-json-attribute");
            assertThat(jsonClaim.get("a"), instanceOf(int.class));
            assertThat(jsonClaim.get("c"), instanceOf(Collection.class));
            assertThat(jsonClaim.get("d"), instanceOf(Map.class));

            //assert that token claim is combination of two protocol mappers values
            List <String> multiClaim = ( List <String>) accessToken.getOtherClaims().get("multi");
            assertEquals(3, multiClaim.size());
            assertThat(multiClaim, containsInAnyOrder("abc", "bcd", "cde"));

            oauth.logoutForm().idTokenHint(response.getIdToken()).open();
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
            AccessTokenResponse response = browserLogin("test-user@localhost", "password");
            IDToken idToken = oauth.verifyIDToken(response.getIdToken());
            assertNull(idToken.getAddress());
            assertNull(idToken.getOtherClaims().get("home_phone"));
            assertNull(idToken.getOtherClaims().get("hard"));
            assertNull(idToken.getOtherClaims().get("nested"));
            assertNull(idToken.getOtherClaims().get("department"));

            oauth.logoutForm().idTokenHint(response.getIdToken()).open();
        }


        events.clear();
    }

    @Test
    
    public void testTokenPropertiesMapping() throws Exception {
        UserResource userResource = findUserByUsernameId(adminClient.realm("test"), "test-user@localhost");
        UserRepresentation user = userResource.toRepresentation();
        user.singleAttribute("userid", "123456789");
        user.getAttributes().put("useraud", Arrays.asList("test-app", "other"));
        userResource.update(user);

        // create a user attr mapping for some claims that exist as properties in the tokens
        ClientResource app = findClientResourceByClientId(adminClient.realm("test"), "test-app");
        app.getProtocolMappers().createMapper(createClaimMapper("userid-as-sub", "userid", "sub", "String", false, true, true,false)).close();
        app.getProtocolMappers().createMapper(createClaimMapper("useraud", "useraud", "aud", "String", true, true, true, true)).close();
        app.getProtocolMappers().createMapper(createHardcodedClaim("website-hardcoded", "website", "http://localhost", "String", true, true, true)).close();
        app.getProtocolMappers().createMapper(createHardcodedClaim("iat-hardcoded", "iat", "123", "long", true, false, true)).close();

        // login
        AccessTokenResponse response = browserLogin("test-user@localhost", "password");

        // assert mappers work as expected
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());
        assertEquals(user.firstAttribute("userid"), idToken.getSubject());
        assertEquals("http://localhost", idToken.getWebsite());
        assertNotNull(idToken.getAudience());
        assertThat(Arrays.asList(idToken.getAudience()), hasItems("test-app", "other"));

        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertNotEquals(user.firstAttribute("userid"), accessToken.getSubject());
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
        oauth.openLogoutForm();

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
    public void testClaimFromUserPropertyMapperWithOptionalProfileScope() {
        RealmResource realm = adminClient.realm("test");
        UserResource userResource = findUserByUsernameId(realm, "test-user@localhost");
        UserRepresentation user = userResource.toRepresentation();
        ClientResource client = findClientResourceByClientId(realm, "test-app");
        Optional<ClientScopeRepresentation> profileScope = realm.clientScopes().findAll().stream().filter(scope -> SCOPE_PROFILE.equals(scope.getName())).findAny();

        assertTrue(profileScope.isPresent());

        String mapperId = null;

        try (Response response = client.getProtocolMappers().createMapper(ModelToRepresentation.toRepresentation(UserPropertyMapper.createClaimMapper(
                "test-property-mapper",
                "email",
                "claim-name",
                String.class.getSimpleName(),
                true,
                true,
                true
        )))) {
            mapperId = getCreatedId(response);
            List<ClientScopeRepresentation> defaultClientScopes = client.getDefaultClientScopes();

            assertTrue(defaultClientScopes.contains(profileScope.get()));

            client.removeDefaultClientScope(profileScope.get().getId());
            client.addOptionalClientScope(profileScope.get().getId());

            oauth.scope(SCOPE_PROFILE);

            AuthorizationEndpointResponse authzEndpointResponse = oauth.doLogin("test-user@localhost", "password");
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(authzEndpointResponse.getCode());

            assertTrue(tokenResponse.getScope().contains("profile"));

            IDToken idToken = oauth.verifyIDToken(tokenResponse.getIdToken());
            assertEquals(user.getEmail(), idToken.getOtherClaims().get("claim-name"));

            AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
            assertEquals(user.getEmail(), accessToken.getOtherClaims().get("claim-name"));
        } finally {
            if (mapperId != null) {
                client.getProtocolMappers().delete(mapperId);
            }
            client.removeOptionalClientScope(profileScope.get().getId());
            client.addDefaultClientScope(profileScope.get().getId());
        }
    }

    @Test
    public void testClaimFromUserPropertyMapperWithDefaultProfileScope() {
        RealmResource realm = adminClient.realm("test");
        UserResource userResource = findUserByUsernameId(realm, "test-user@localhost");
        UserRepresentation user = userResource.toRepresentation();
        ClientResource client = findClientResourceByClientId(realm, "test-app");
        Optional<ClientScopeRepresentation> profileScope = realm.clientScopes().findAll().stream().filter(scope -> SCOPE_PROFILE.equals(scope.getName())).findAny();

        assertTrue(profileScope.isPresent());

        String mapperId = null;

        try (Response response = client.getProtocolMappers().createMapper(ModelToRepresentation.toRepresentation(UserPropertyMapper.createClaimMapper(
                "test-property-mapper",
                "email",
                "claim-name",
                String.class.getSimpleName(),
                true,
                true,
                true
        )))) {
            mapperId = getCreatedId(response);
            List<ClientScopeRepresentation> defaultClientScopes = client.getDefaultClientScopes();

            assertTrue(defaultClientScopes.contains(profileScope.get()));

            oauth.scope(SCOPE_PROFILE);

            AuthorizationEndpointResponse authzEndpointResponse = oauth.doLogin("test-user@localhost", "password");
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(authzEndpointResponse.getCode());

            assertTrue(tokenResponse.getScope().contains("profile"));

            IDToken idToken = oauth.verifyIDToken(tokenResponse.getIdToken());
            assertEquals(user.getEmail(), idToken.getOtherClaims().get("claim-name"));

            AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
            assertEquals(user.getEmail(), accessToken.getOtherClaims().get("claim-name"));
        } finally {
            if (mapperId != null) {
                client.getProtocolMappers().delete(mapperId);
            }
        }
    }

    @Test
    
    public void testNullOrEmptyTokenMapping() throws Exception {
        {
            UserResource userResource = findUserByUsernameId(adminClient.realm("test"), "test-user@localhost");
            UserRepresentation user = userResource.toRepresentation();

            user.singleAttribute("empty", "");
            user.singleAttribute("null", null);
            userResource.update(user);

            ClientResource app = findClientResourceByClientId(adminClient.realm("test"), "test-app");
            app.getProtocolMappers().createMapper(createClaimMapper("empty", "empty", "empty", "String", true, true, true,false)).close();
            app.getProtocolMappers().createMapper(createClaimMapper("null", "null", "null", "String", true, true, true,false)).close();
        }

        {
            AccessTokenResponse response = browserLogin("test-user@localhost", "password");

            IDToken idToken = oauth.verifyIDToken(response.getIdToken());
            Object empty = idToken.getOtherClaims().get("empty");
            assertThat((empty == null ? null : (String) empty), is(emptyOrNullString()));
            Object nulll = idToken.getOtherClaims().get("null");
            assertNull(nulll);

            oauth.verifyToken(response.getAccessToken());
            oauth.logoutForm().idTokenHint(response.getIdToken()).open();
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
            AccessTokenResponse response = browserLogin("test-user@localhost", "password");
            IDToken idToken = oauth.verifyIDToken(response.getIdToken());
            assertNull(idToken.getAddress());
            assertNull(idToken.getOtherClaims().get("empty"));
            assertNull(idToken.getOtherClaims().get("null"));

            oauth.logoutForm().idTokenHint(response.getIdToken()).open();
        }
        events.clear();
    }



    @Test
    
    public void testUserRoleToAttributeMappers() throws Exception {
        // Add mapper for realm roles
        ProtocolMapperRepresentation realmMapper = ProtocolMapperUtil.createUserRealmRoleMappingMapper("pref.", "Realm roles mapper", "roles-custom.realm", true, true, true);
        ProtocolMapperRepresentation clientMapper = ProtocolMapperUtil.createUserClientRoleMappingMapper("test-app", null, "Client roles mapper", "roles-custom.test-app", true, true, true);

        ProtocolMappersResource protocolMappers = ApiUtil.findClientResourceByClientId(adminClient.realm("test"), "test-app").getProtocolMappers();
        protocolMappers.createMapper(Arrays.asList(realmMapper, clientMapper));

        // Login user
        AccessTokenResponse response = browserLogin("test-user@localhost", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        // Verify attribute is filled
        Map<String, Object> roleMappings = (Map<String, Object>)idToken.getOtherClaims().get("roles-custom");
        assertThat(roleMappings.keySet(), containsInAnyOrder("realm", "test-app"));
        List<String> realmRoleMappings = (List<String>) roleMappings.get("realm");
        List<String> testAppMappings = (List<String>) roleMappings.get("test-app");
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
            AccessTokenResponse response = browserLogin("test-user@localhost", "password");
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
            AccessTokenResponse response = browserLogin("test-user@localhost", "password");
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
        AccessTokenResponse response = browserLogin("test-user@localhost", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        // Verify attribute is filled
        Map<String, Object> roleMappings = (Map<String, Object>)idToken.getOtherClaims().get("roles-custom");
        assertThat(roleMappings.keySet(), containsInAnyOrder("realm", "test-app"));
        assertThat(roleMappings.get("realm"), CoreMatchers.instanceOf(List.class));
        assertThat(roleMappings.get("test-app"), CoreMatchers.instanceOf(List.class));

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
        AccessTokenResponse response = browserLogin("test-user@localhost", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        // Verify attribute is filled
        Map<String, Object> roleMappings = (Map<String, Object>)idToken.getOtherClaims().get("roles-custom");
        assertThat(roleMappings.keySet(), containsInAnyOrder("realm", "test-app"));
        assertThat(roleMappings.get("realm"), CoreMatchers.instanceOf(List.class));
        assertThat(roleMappings.get("test-app"), CoreMatchers.instanceOf(List.class));

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
        ProtocolMapperRepresentation clientMapper = ProtocolMapperUtil.createUserClientRoleMappingMapper(clientId, null, "Client roles mapper", "roles-custom.test-app", true, true, true);

        ProtocolMappersResource protocolMappers = ApiUtil.findClientResourceByClientId(adminClient.realm("test"), clientId).getProtocolMappers();
        protocolMappers.createMapper(Arrays.asList(clientMapper));

        // Add user 'level2GroupUser' to the group 'level2Group2'
        GroupRepresentation level2Group2 = adminClient.realm("test").getGroupByPath("/topGroup/level2group2");
        UserResource level2GroupUser = ApiUtil.findUserByUsernameId(adminClient.realm("test"), "level2GroupUser");
        level2GroupUser.joinGroup(level2Group2.getId());

        oauth.client(clientId, "password");
        AccessTokenResponse response = browserLogin("level2GroupUser", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        // Verify attribute is filled AND it is filled only once
        Map<String, Object> roleMappings = (Map<String, Object>)idToken.getOtherClaims().get("roles-custom");
        assertThat(roleMappings.keySet(), containsInAnyOrder(clientId));
        List<String> testAppScopeMappings = (List<String>) roleMappings.get(clientId);
        assertRolesString(testAppScopeMappings,
                "customer-user"      // from assignment to level2group or level2group2. It is filled just once
        );

        // Revert
        level2GroupUser.leaveGroup(level2Group2.getId());
        deleteMappers(protocolMappers);
    }


    @Test
    
    public void testUserGroupRoleToAttributeMappers() throws Exception {
        // Add mapper for realm roles
        String clientId = "test-app";
        ProtocolMapperRepresentation realmMapper = ProtocolMapperUtil.createUserRealmRoleMappingMapper("pref.", "Realm roles mapper", "roles-custom.realm", true, true, true);
        ProtocolMapperRepresentation clientMapper = ProtocolMapperUtil.createUserClientRoleMappingMapper(clientId, "ta.", "Client roles mapper", "roles-custom.test-app", true, true, true);

        ProtocolMappersResource protocolMappers = ApiUtil.findClientResourceByClientId(adminClient.realm("test"), clientId).getProtocolMappers();
        protocolMappers.createMapper(Arrays.asList(realmMapper, clientMapper));

        // Login user
        AccessTokenResponse response = browserLogin("rich.roles@redhat.com", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        // Verify attribute is filled
        Map<String, Object> roleMappings = (Map<String, Object>)idToken.getOtherClaims().get("roles-custom");
        assertThat(roleMappings.keySet(), containsInAnyOrder("realm", clientId));
        List<String> realmRoleMappings = (List<String>) roleMappings.get("realm");
        List<String> testAppMappings = (List<String>) roleMappings.get(clientId);
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
        ProtocolMapperRepresentation realmMapper = ProtocolMapperUtil.createUserRealmRoleMappingMapper("pref.", "Realm roles mapper", "roles-custom.realm", true, true, true);
        ProtocolMapperRepresentation clientMapper = ProtocolMapperUtil.createUserClientRoleMappingMapper(clientId, null, "Client roles mapper", "roles-custom." + clientId, true, true, true);

        ProtocolMappersResource protocolMappers = ApiUtil.findClientResourceByClientId(adminClient.realm("test"), clientId).getProtocolMappers();
        protocolMappers.createMapper(Arrays.asList(realmMapper, clientMapper));

        // Login user
        ClientManager.realm(adminClient.realm("test")).clientId(clientId).directAccessGrant(true);
        oauth.client(clientId, "secret");

        String oldRedirectUri = oauth.getRedirectUri();
        oauth.redirectUri(UriUtils.getOrigin(oldRedirectUri) + "/test-app-authz");

        AccessTokenResponse response = browserLogin("rich.roles@redhat.com", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        // revert redirect_uri
        oauth.redirectUri(oldRedirectUri);

        // Verify attribute is filled
        Map<String, Object> roleMappings = (Map<String, Object>)idToken.getOtherClaims().get("roles-custom");
        assertThat(roleMappings.keySet(), containsInAnyOrder("realm"));
        List<String> realmRoleMappings = (List<String>) roleMappings.get("realm");
        List<String> testAppAuthzMappings = (List<String>) roleMappings.get(clientId);
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
        ProtocolMapperRepresentation realmMapper = ProtocolMapperUtil.createUserRealmRoleMappingMapper("pref.", "Realm roles mapper", "roles-custom.realm", true, true, true);
        ProtocolMapperRepresentation clientMapper = ProtocolMapperUtil.createUserClientRoleMappingMapper(clientId, null, "Client roles mapper", "roles-custom.test-app-scope", true, true, true);

        ProtocolMappersResource protocolMappers = ApiUtil.findClientResourceByClientId(adminClient.realm("test"), clientId).getProtocolMappers();
        protocolMappers.createMapper(Arrays.asList(realmMapper, clientMapper));

        // Login user
        ClientManager.realm(adminClient.realm("test")).clientId(clientId).directAccessGrant(true);
        oauth.client(clientId, "password");
        AccessTokenResponse response = browserLogin("rich.roles@redhat.com", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        // Verify attribute is filled
        Map<String, Object> roleMappings = (Map<String, Object>)idToken.getOtherClaims().get("roles-custom");
        assertThat(roleMappings.keySet(), containsInAnyOrder("realm", clientId));
        List<String> realmRoleMappings = (List<String>) roleMappings.get("realm");
        List<String> testAppScopeMappings = (List<String>) roleMappings.get(clientId);
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
        ProtocolMapperRepresentation realmMapper = ProtocolMapperUtil.createUserRealmRoleMappingMapper("pref.", "Realm roles mapper", "roles-custom.realm", true, true, true);
        ProtocolMapperRepresentation clientMapper = ProtocolMapperUtil.createUserClientRoleMappingMapper(null, null, "Client roles mapper", "roles-custom.test-app-scope", true, true, true);

        ProtocolMappersResource protocolMappers = ApiUtil.findClientResourceByClientId(adminClient.realm("test"), clientId).getProtocolMappers();
        protocolMappers.createMapper(Arrays.asList(realmMapper, clientMapper));

        // Login user
        ClientManager.realm(adminClient.realm("test")).clientId(clientId).directAccessGrant(true);
        oauth.client(clientId, "password");
        AccessTokenResponse response = browserLogin("rich.roles@redhat.com", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        // Verify attribute is filled
        Map<String, Object> roleMappings = (Map<String, Object>)idToken.getOtherClaims().get("roles-custom");
        assertThat(roleMappings.keySet(), containsInAnyOrder("realm", clientId));
        List<String> realmRoleMappings = (List<String>) roleMappings.get("realm");
        List<String> testAppScopeMappings = (List<String>) roleMappings.get(clientId);
        assertRolesString(realmRoleMappings,
          "pref.admin",                     // from direct assignment to /roleRichGroup/level2group
          "pref.user",  // from parent group of /roleRichGroup/level2group, i.e. from /roleRichGroup
          "pref.customer-user-premium"
        );
        assertRolesString(testAppScopeMappings,
          "test-app-allowed-by-scope",      // from direct assignment to roleRichUser, present as scope allows it
          "test-app-disallowed-by-scope",   // from direct assignment to /roleRichGroup/level2group, present as scope allows it
          "customer-admin-composite-role",  // from the other application
          "customer-admin"
        );

        // Revert
        deleteMappers(protocolMappers);
    }

    @Test
    public void testSingleValuedRoleMapping() throws Exception {
        String clientId = "test-app-scope";
        ProtocolMapperRepresentation realmMapper = ProtocolMapperUtil.createUserRealmRoleMappingMapper("pref.", "Realm roles mapper", "roles-custom.realm", true, true, true,false);
        ProtocolMapperRepresentation clientMapper = ProtocolMapperUtil.createUserClientRoleMappingMapper(null, null, "Client roles mapper", "roles-custom.test-app-scope", true, true, true, false);

        ProtocolMappersResource protocolMappers = ApiUtil.findClientResourceByClientId(adminClient.realm("test"), clientId).getProtocolMappers();
        protocolMappers.createMapper(Arrays.asList(realmMapper, clientMapper));

        // Login user
        ClientManager.realm(adminClient.realm("test")).clientId(clientId).directAccessGrant(true);
        oauth.client(clientId, "password");
        AccessTokenResponse response = browserLogin("rich.roles@redhat.com", "password");
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        // Verify attribute is filled
        Map<String, Object> roleMappings = (Map<String, Object>)idToken.getOtherClaims().get("roles-custom");
        assertThat(roleMappings.keySet(), containsInAnyOrder("realm", clientId));
        String realmRoleMappings = (String) roleMappings.get("realm");
        String testAppScopeMappings = (String) roleMappings.get(clientId);
        assertSingleValuedRolesString(realmRoleMappings,
                "pref.admin",                     // from direct assignment to /roleRichGroup/level2group
                "pref.user",  // from parent group of /roleRichGroup/level2group, i.e. from /roleRichGroup
                "pref.customer-user-premium"
        );
        assertSingleValuedRolesString(testAppScopeMappings,
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

        final ProtocolMapperRepresentation realmMapper = ProtocolMapperUtil.createUserRealmRoleMappingMapper("pref.", "Realm roles mapper", "roles-custom.realm", true, true, true);
        final ProtocolMapperRepresentation clientMapper = ProtocolMapperUtil.createUserClientRoleMappingMapper(diffClient, null, "Client roles mapper", "roles-custom.test-app", true, true, true);

        try (ClientAttributeUpdater cau = ClientAttributeUpdater.forClient(adminClient, realmName, clientId).setDirectAccessGrantsEnabled(true);
             ProtocolMappersUpdater protocolMappers = new ProtocolMappersUpdater(cau.getResource().getProtocolMappers())) {

            protocolMappers.add(realmMapper, clientMapper).update();

            // Login user
            oauth.client(clientId, "password");
            AccessTokenResponse response = browserLogin("rich.roles@redhat.com", "password");
            IDToken idToken = oauth.verifyIDToken(response.getIdToken());

            // Verify attribute is filled
            Map<String, Object> roleMappings = (Map<String, Object>) idToken.getOtherClaims().get("roles-custom");
            assertNotNull(roleMappings);
            assertThat(roleMappings.keySet(), containsInAnyOrder("realm", diffClient));
            List<String> realmRoleMappings = (List<String>) roleMappings.get("realm");
            List<String> testAppScopeMappings = (List<String>) roleMappings.get(diffClient);
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
        protocolMappers.createMapper(createClaimMapper("group-value", "group-value", "group-value", "String", true, true, true,false, false)).close();

        try {
            // test it
            AccessTokenResponse response = browserLogin("test-user@localhost", "password");

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
        protocolMappers.createMapper(createClaimMapper("group-value", "group-value", "group-value", "String", true, true, true, true, false)).close();

        try {
            // test it
            AccessTokenResponse response = browserLogin("test-user@localhost", "password");

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
        protocolMappers.createMapper(createClaimMapper("group-value", "group-value", "group-value", "String", true, true, true, true, true)).close();

        try {
            // test it
            AccessTokenResponse response = browserLogin("test-user@localhost", "password");

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
        protocolMappers.createMapper(createClaimMapper("group-value", "group-value", "group-value", "String", true, true, true, false, false)).close();

        try {
            // test it
            AccessTokenResponse response = browserLogin("test-user@localhost", "password");

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
        protocolMappers.createMapper(createClaimMapper("group-value", "group-value", "group-value", "String", true, true, true, true, false)).close();

        try {
            // test it
            AccessTokenResponse response = browserLogin("test-user@localhost", "password");

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
        protocolMappers.createMapper(createClaimMapper("group-value", "group-value", "group-value", "String", true, true, true, true, true)).close();

        try {
            // test it
            AccessTokenResponse response = browserLogin("test-user@localhost", "password");

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
        protocolMappers.createMapper(createClaimMapper("group-value", "group-value", "group-value", "String", true, true, true, false, false)).close();

        try {
            // test it
            AccessTokenResponse response = browserLogin("test-user@localhost", "password");

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
        protocolMappers.createMapper(createClaimMapper("group-value", "group-value", "group-value", "String", true, true, true, true, false)).close();

        try {
            // test it
            AccessTokenResponse response = browserLogin("test-user@localhost", "password");

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
        protocolMappers.createMapper(createClaimMapper("group-value", "group-value", "group-value", "String", true, true, true, true, true)).close();

        try {
            // test it
            AccessTokenResponse response = browserLogin("test-user@localhost", "password");

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

    // KEYCLOAK-17252 -- Test the scenario where:
    // - one group is a subgroup of another
    // - only the parent group has values for the 'group-value' attribute
    // - a user is a member of the subgroup
    // - the 'single value' attribute 'group-value' should not be aggregated
    @Test
    public void testGroupAttributeTwoGroupHierarchyNoMultivalueNoAggregateFromParent() throws Exception {
        // get the user
        UserResource userResource = findUserByUsernameId(adminClient.realm("test"), "test-user@localhost");
        // create two groups with two values (one is the same value)
        GroupRepresentation group1 = new GroupRepresentation();
        group1.setName("group1");
        group1.setAttributes(new HashMap<>());
        group1.getAttributes().put("group-value", Arrays.asList("value1", "value2"));
        adminClient.realm("test").groups().add(group1);
        group1 = adminClient.realm("test").getGroupByPath("/group1");
        GroupRepresentation group2 = new GroupRepresentation();
        group2.setName("group2");
        group2.setAttributes(new HashMap<>());
        adminClient.realm("test").groups().add(group2);
        group2 = adminClient.realm("test").getGroupByPath("/group2");
        // make group2 a subgroup of group1 and make user join group2
        adminClient.realm("test").groups().group(group1.getId()).subGroup(group2);
        userResource.joinGroup(group2.getId());

        // create the attribute mapper
        ProtocolMappersResource protocolMappers = findClientResourceByClientId(adminClient.realm("test"), "test-app").getProtocolMappers();
        protocolMappers.createMapper(createClaimMapper("group-value", "group-value", "group-value", "String", true, true, true, false, false)).close();

        try {
            // test it
            AccessTokenResponse response = browserLogin("test-user@localhost", "password");

            IDToken idToken = oauth.verifyIDToken(response.getIdToken());
            assertNotNull(idToken.getOtherClaims());
            assertNotNull(idToken.getOtherClaims().get("group-value"));
            assertTrue(idToken.getOtherClaims().get("group-value") instanceof String);
            assertTrue("value1".equals(idToken.getOtherClaims().get("group-value"))
                    || "value2".equals(idToken.getOtherClaims().get("group-value")));
        } finally {
            // revert
            userResource.leaveGroup(group2.getId());
            adminClient.realm("test").groups().group(group2.getId()).remove();
            adminClient.realm("test").groups().group(group1.getId()).remove();
            deleteMappers(protocolMappers);
        }
    }

    // KEYCLOAK-17252 -- Test the scenario where:
    // - one group is a subgroup of another
    // - both groups have values for the 'group-value' attribute
    // - a user is a member of the subgroup
    // - the 'single value' attribute 'group-value' should not be aggregated
    @Test
    public void testGroupAttributeTwoGroupHierarchyNoMultivalueNoAggregateFromChild() throws Exception {
        // get the user
        UserResource userResource = findUserByUsernameId(adminClient.realm("test"), "test-user@localhost");
        // create two groups with two values (one is the same value)
        GroupRepresentation group1 = new GroupRepresentation();
        group1.setName("group1");
        group1.setAttributes(new HashMap<>());
        group1.getAttributes().put("group-value", Arrays.asList("value1", "value2"));
        adminClient.realm("test").groups().add(group1);
        group1 = adminClient.realm("test").getGroupByPath("/group1");
        GroupRepresentation group2 = new GroupRepresentation();
        group2.setName("group2");
        group2.setAttributes(new HashMap<>());
        group2.getAttributes().put("group-value", Arrays.asList("value3", "value4"));
        adminClient.realm("test").groups().add(group2);
        group2 = adminClient.realm("test").getGroupByPath("/group2");
        // make group2 a subgroup of group1 and make user join group2
        adminClient.realm("test").groups().group(group1.getId()).subGroup(group2);
        userResource.joinGroup(group2.getId());

        // create the attribute mapper
        ProtocolMappersResource protocolMappers = findClientResourceByClientId(adminClient.realm("test"), "test-app").getProtocolMappers();
        protocolMappers.createMapper(createClaimMapper("group-value", "group-value", "group-value", "String", true, true, true, false, false)).close();

        try {
            // test it
            AccessTokenResponse response = browserLogin("test-user@localhost", "password");

            IDToken idToken = oauth.verifyIDToken(response.getIdToken());
            assertNotNull(idToken.getOtherClaims());
            assertNotNull(idToken.getOtherClaims().get("group-value"));
            assertTrue(idToken.getOtherClaims().get("group-value") instanceof String);
            assertTrue("value3".equals(idToken.getOtherClaims().get("group-value"))
                    || "value4".equals(idToken.getOtherClaims().get("group-value")));
        } finally {
            // revert
            userResource.leaveGroup(group2.getId());
            adminClient.realm("test").groups().group(group2.getId()).remove();
            adminClient.realm("test").groups().group(group1.getId()).remove();
            deleteMappers(protocolMappers);
        }
    }

    // KEYCLOAK-17252 -- Test the scenario where:
    // - one group is a subgroup of another
    // - both groups have values for the 'group-value' attribute
    // - a user is a member of the subgroup
    // - the multivalue attribute 'group-value' should not be aggregated
    @Test
    public void testGroupAttributeTwoGroupHierarchyMultiValueNoAggregate() throws Exception {
        // get the user
        UserResource userResource = findUserByUsernameId(adminClient.realm("test"), "test-user@localhost");
        // create two groups with two values (one is the same value)
        GroupRepresentation group1 = new GroupRepresentation();
        group1.setName("group1");
        group1.setAttributes(new HashMap<>());
        group1.getAttributes().put("group-value", Arrays.asList("value1", "value2"));
        adminClient.realm("test").groups().add(group1);
        group1 = adminClient.realm("test").getGroupByPath("/group1");
        GroupRepresentation group2 = new GroupRepresentation();
        group2.setName("group2");
        group2.setAttributes(new HashMap<>());
        group2.getAttributes().put("group-value", Arrays.asList("value2", "value3"));
        adminClient.realm("test").groups().add(group2);
        group2 = adminClient.realm("test").getGroupByPath("/group2");
        // make group2 a subgroup of group1 and make user join group2
        adminClient.realm("test").groups().group(group1.getId()).subGroup(group2);
        userResource.joinGroup(group2.getId());

        // create the attribute mapper
        ProtocolMappersResource protocolMappers = findClientResourceByClientId(adminClient.realm("test"), "test-app").getProtocolMappers();
        protocolMappers.createMapper(createClaimMapper("group-value", "group-value", "group-value", "String", true, true, true, true, false)).close();

        try {
            // test it
            AccessTokenResponse response = browserLogin("test-user@localhost", "password");

            IDToken idToken = oauth.verifyIDToken(response.getIdToken());
            assertNotNull(idToken.getOtherClaims());
            assertNotNull(idToken.getOtherClaims().get("group-value"));
            assertTrue(idToken.getOtherClaims().get("group-value") instanceof List);
            assertEquals(2, ((List) idToken.getOtherClaims().get("group-value")).size());
            assertTrue(((List) idToken.getOtherClaims().get("group-value")).contains("value2"));
            assertTrue(((List) idToken.getOtherClaims().get("group-value")).contains("value3"));
        } finally {
            // revert
            userResource.leaveGroup(group2.getId());
            adminClient.realm("test").groups().group(group2.getId()).remove();
            adminClient.realm("test").groups().group(group1.getId()).remove();
            deleteMappers(protocolMappers);
        }
    }

    // KEYCLOAK-17252 -- Test the scenario where:
    // - one group is a subgroup of another
    // - both groups have values for the 'group-value' attribute
    // - a user is a member of the subgroup
    // - the multivalue attribute 'group-value' should be aggregated
    @Test
    public void testGroupAttributeTwoGroupHierarchyMultiValueAggregate() throws Exception {
        // get the user
        UserResource userResource = findUserByUsernameId(adminClient.realm("test"), "test-user@localhost");
        UserRepresentation user = userResource.toRepresentation();
        user.setAttributes(new HashMap<>());
        user.getAttributes().put("group-value", Arrays.asList("user-value1", "user-value2"));
        userResource.update(user);
        // create two groups with two values (one is the same value)
        GroupRepresentation group1 = new GroupRepresentation();
        group1.setName("group1");
        group1.setAttributes(new HashMap<>());
        group1.getAttributes().put("group-value", Arrays.asList("value1", "value2"));
        adminClient.realm("test").groups().add(group1);
        group1 = adminClient.realm("test").getGroupByPath("/group1");
        GroupRepresentation group2 = new GroupRepresentation();
        group2.setName("group2");
        group2.setAttributes(new HashMap<>());
        group2.getAttributes().put("group-value", Arrays.asList("value2", "value3"));
        adminClient.realm("test").groups().add(group2);
        group2 = adminClient.realm("test").getGroupByPath("/group2");
        // make group2 a subgroup of group1 and make user join group2
        adminClient.realm("test").groups().group(group1.getId()).subGroup(group2);
        userResource.joinGroup(group2.getId());

        // create the attribute mapper
        ProtocolMappersResource protocolMappers = findClientResourceByClientId(adminClient.realm("test"), "test-app").getProtocolMappers();
        protocolMappers.createMapper(createClaimMapper("group-value", "group-value", "group-value", "String", true, true, true,true, true)).close();

        try {
            // test it
            AccessTokenResponse response = browserLogin("test-user@localhost", "password");

            IDToken idToken = oauth.verifyIDToken(response.getIdToken());
            assertNotNull(idToken.getOtherClaims());
            assertNotNull(idToken.getOtherClaims().get("group-value"));
            assertTrue(idToken.getOtherClaims().get("group-value") instanceof List);
            assertEquals(5, ((List) idToken.getOtherClaims().get("group-value")).size());
            assertTrue(((List) idToken.getOtherClaims().get("group-value")).contains("user-value1"));
            assertTrue(((List) idToken.getOtherClaims().get("group-value")).contains("user-value2"));
            assertTrue(((List) idToken.getOtherClaims().get("group-value")).contains("value1"));
            assertTrue(((List) idToken.getOtherClaims().get("group-value")).contains("value2"));
            assertTrue(((List) idToken.getOtherClaims().get("group-value")).contains("value3"));
        } finally {
            // revert
            user.getAttributes().remove("group-value");
            userResource.update(user);
            userResource.leaveGroup(group2.getId());
            adminClient.realm("test").groups().group(group2.getId()).remove();
            adminClient.realm("test").groups().group(group1.getId()).remove();
            deleteMappers(protocolMappers);
        }
    }

    private void checkRealmAccessInOtherClaims(Map<String, Object> otherClaims, String shouldExistRole, String shouldNotExistRole) {
        assertThat(otherClaims.get("realm_access"), CoreMatchers.instanceOf(Map.class));
        Map<?, ?> access = (Map<?, ?>) otherClaims.get("realm_access");
        assertThat(access.get("roles"), CoreMatchers.instanceOf(Collection.class));
        Collection<String> roles = (Collection<String>) access.get("roles");
        if (shouldExistRole != null) {
            assertThat(roles, hasItem(shouldExistRole));
        }
        if (shouldNotExistRole != null) {
            assertThat(roles, not(hasItem(shouldNotExistRole)));
        }
    }

    private void checkClientAccessInOtherClaims(Map<String, Object> otherClaims, String app, String shouldExistRole, String shouldNotExistRole) {
        assertThat(otherClaims.get("resource_access"), CoreMatchers.instanceOf(Map.class));
        Map<?, ?> access = (Map<?, ?>) otherClaims.get("resource_access");
        assertThat(access.get(app), CoreMatchers.instanceOf(Map.class));
        access = (Map<?, ?>) access.get(app);
        assertThat(access.get("roles"), CoreMatchers.instanceOf(Collection.class));
        Collection<String> roles = (Collection<String>) access.get("roles");
        if (shouldExistRole != null) {
            assertThat(roles, hasItem(shouldExistRole));
        }
        if (shouldNotExistRole != null) {
            assertThat(roles, not(hasItem(shouldNotExistRole)));
        }
    }

    private Map<String, String> modifyScopeRolesMapperToBeIncludedInAll(ClientScopeResource rolesScope, ProtocolMapperRepresentation mapper) {
        Map<String, String> config = new HashMap<>(mapper.getConfig());
        mapper.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "true");
        mapper.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        mapper.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        rolesScope.getProtocolMappers().update(mapper.getId(), mapper);
        return config;
    }

    @Test
    public void testHardcodeRoleAll() throws Exception {
        RealmResource testRealm = adminClient.realm("test");
        ClientResource app = findClientResourceByClientId(testRealm, "test-app");
        // create two hardcoded realm mappers for realm and client
        String hardcodedRoleRealmMapperId, hardcodedRoleClientMapperId;
        try (Response resp = app.getProtocolMappers().createMapper(createHardcodedRole("hardcoded-realm", "hardcoded"))) {
            hardcodedRoleRealmMapperId = ApiUtil.getCreatedId(resp);
        }
        try (Response resp = app.getProtocolMappers().createMapper(createHardcodedRole("hardcoded-app", "test-app.hardcoded"))) {
            hardcodedRoleClientMapperId = ApiUtil.getCreatedId(resp);
        }
        // modify the default role mappers to be included in access, ID and user-info
        ClientScopeResource rolesScope = ApiUtil.findClientScopeByName(testRealm, OIDCLoginProtocolFactory.ROLES_SCOPE);
        ProtocolMapperRepresentation realmRolesMapper = ApiUtil.findProtocolMapperByName(rolesScope, OIDCLoginProtocolFactory.REALM_ROLES);
        Map<String, String> configRealmRoles = modifyScopeRolesMapperToBeIncludedInAll(rolesScope, realmRolesMapper);
        ProtocolMapperRepresentation clientRolesMapper = ApiUtil.findProtocolMapperByName(rolesScope, OIDCLoginProtocolFactory.CLIENT_ROLES);
        Map<String, String> configClientRoles = modifyScopeRolesMapperToBeIncludedInAll(rolesScope, clientRolesMapper);

        // check that the hardcoded mappers are in the three responses
        try {
            AccessTokenResponse response = browserLogin("test-user@localhost", "password");

            // check hardcoded roles in access token
            AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
            assertThat(accessToken.getRealmAccess().getRoles(), hasItem("hardcoded"));
            assertNotNull(accessToken.getResourceAccess("test-app"));
            assertThat(accessToken.getResourceAccess("test-app").getRoles(), hasItem("hardcoded"));

            // in ID token
            IDToken idToken = oauth.verifyIDToken(response.getIdToken());
            checkRealmAccessInOtherClaims(idToken.getOtherClaims(), "hardcoded", null);
            checkClientAccessInOtherClaims(idToken.getOtherClaims(), "test-app", "hardcoded", null);

            // in the user info
            Client client = AdminClientUtil.createResteasyClient();
            try {
                Response userInfoResponse = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, response.getAccessToken());
                UserInfo userInfo = userInfoResponse.readEntity(UserInfo.class);
                assertEquals("test-user@localhost", userInfo.getPreferredUsername());
                checkRealmAccessInOtherClaims(userInfo.getOtherClaims(), "hardcoded", null);
                checkClientAccessInOtherClaims(userInfo.getOtherClaims(), "test-app", "hardcoded", null);
            } finally {
                client.close();
            }
        } finally {
            // reset the roles client scopes
            app.getProtocolMappers().delete(hardcodedRoleRealmMapperId);
            app.getProtocolMappers().delete(hardcodedRoleClientMapperId);
            realmRolesMapper.setConfig(configRealmRoles);
            rolesScope.getProtocolMappers().update(realmRolesMapper.getId(), realmRolesMapper);
            clientRolesMapper.setConfig(configClientRoles);
            rolesScope.getProtocolMappers().update(clientRolesMapper.getId(), clientRolesMapper);
        }
    }

    @Test
    public void testRoleNameMapperAll() throws Exception {
        RealmResource testRealm = adminClient.realm("test");
        ClientResource app = findClientResourceByClientId(testRealm, "test-app");
        // create two role name mappers for realm and client
        String realmRoleNameMapperId, clientRoleNameMapperId;
        try (Response resp = app.getProtocolMappers().createMapper(createRoleNameMapper("rename-realm-role", "user", "realm-user"))) {
            realmRoleNameMapperId = ApiUtil.getCreatedId(resp);
        }
        try (Response resp = app.getProtocolMappers().createMapper(createRoleNameMapper("rename-app-role", "test-app.customer-user", "test-app.test-app-user"))) {
            clientRoleNameMapperId = ApiUtil.getCreatedId(resp);
        }
        // modify the default role mappers to be included in access, ID and user-info
        ClientScopeResource rolesScope = ApiUtil.findClientScopeByName(testRealm, OIDCLoginProtocolFactory.ROLES_SCOPE);
        ProtocolMapperRepresentation realmRolesMapper = ApiUtil.findProtocolMapperByName(rolesScope, OIDCLoginProtocolFactory.REALM_ROLES);
        Map<String, String> configRealmRoles = modifyScopeRolesMapperToBeIncludedInAll(rolesScope, realmRolesMapper);
        ProtocolMapperRepresentation clientRolesMapper = ApiUtil.findProtocolMapperByName(rolesScope, OIDCLoginProtocolFactory.CLIENT_ROLES);
        Map<String, String> configClientRoles = modifyScopeRolesMapperToBeIncludedInAll(rolesScope, clientRolesMapper);

        // check that the role mappers are executed in the three responses
        try {
            AccessTokenResponse response = browserLogin("test-user@localhost", "password");

            // check mapped roles are in access token and not the original ones
            AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
            assertThat(accessToken.getRealmAccess().getRoles(), hasItem("realm-user"));
            assertThat(accessToken.getRealmAccess().getRoles(), not(hasItem("user")));
            assertNotNull(accessToken.getResourceAccess("test-app"));
            assertThat(accessToken.getResourceAccess("test-app").getRoles(), hasItem("test-app-user"));
            assertThat(accessToken.getResourceAccess("test-app").getRoles(), not(hasItem("customer-user")));

            // same in ID token
            IDToken idToken = oauth.verifyIDToken(response.getIdToken());
            checkRealmAccessInOtherClaims(idToken.getOtherClaims(), "realm-user", "user");
            checkClientAccessInOtherClaims(idToken.getOtherClaims(), "test-app", "test-app-user", "customer-user");

            // same in user info
            Client client = AdminClientUtil.createResteasyClient();
            try {
                Response userInfoResponse = UserInfoClientUtil.executeUserInfoRequest_getMethod(client, response.getAccessToken());
                UserInfo userInfo = userInfoResponse.readEntity(UserInfo.class);
                assertEquals("test-user@localhost", userInfo.getPreferredUsername());
                checkRealmAccessInOtherClaims(userInfo.getOtherClaims(), "realm-user", "user");
                checkClientAccessInOtherClaims(userInfo.getOtherClaims(), "test-app", "test-app-user", "customer-user");
            } finally {
                client.close();
            }
        } finally {
            // reset the roles client scopes
            app.getProtocolMappers().delete(realmRoleNameMapperId);
            app.getProtocolMappers().delete(clientRoleNameMapperId);
            realmRolesMapper.setConfig(configRealmRoles);
            rolesScope.getProtocolMappers().update(realmRolesMapper.getId(), realmRolesMapper);
            clientRolesMapper.setConfig(configClientRoles);
            rolesScope.getProtocolMappers().update(clientRolesMapper.getId(), clientRolesMapper);
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
        ProtocolMapperRepresentation protocolMapperRepresentation = createHardcodedClaim("dynamic-scope-hardcoded-mapper", "hardcoded-foo", "hardcoded-bar", "String", true, true, true);
        scopeRep.setProtocolMappers(Collections.singletonList(protocolMapperRepresentation));

        try (Response resp = adminClient.realm("test").clientScopes().create(scopeRep)) {
            assertEquals(201, resp.getStatus());
            String clientScopeId = ApiUtil.getCreatedId(resp);
            getCleanup().addClientScopeId(clientScopeId);
            clientResource.addOptionalClientScope(clientScopeId);
        }

        oauth.scope("openid dyn-scope-with-mapper:value");
        AccessTokenResponse response = browserLogin("test-user@localhost", "password");
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

    @Test
    public void testStaticScopeUsingDynamicScopeFormatWithDedicatedMappers() {
        RealmResource realm = adminClient.realm("test");
        ClientResource clientResource = findClientResourceByClientId(realm, "test-app");
        ClientRepresentation client = clientResource.toRepresentation();

        // make sure the name of the client maps to the prefix of the dynamic scope name
        client.setName("test");
        clientResource.update(client);

        String expectedScopeName = "test:create-mapper";
        createClientScope(realm, clientResource, expectedScopeName, "from-mapper", "value", false);

        // creates a dedicated mapper to the client
        ProtocolMappersResource dedicatedClientMappers = clientResource.getProtocolMappers();
        dedicatedClientMappers.createMapper(createHardCodedMapper("from-dedicated-mapper", "from-dedicated-mapper", "value")).close();

        // request the test:create-mapper scope so that mappers are included
        oauth.scope("openid " + expectedScopeName);
        AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user@localhost", "password");
        assertTrue(response.getScope().contains(expectedScopeName));

        IDToken idToken = oauth.verifyIDToken(response.getIdToken());
        assertNotNull(idToken.getOtherClaims());
        assertNotNull(idToken.getOtherClaims().get("from-mapper"));
        // claim mapped by client scope mapper
        assertEquals("value", idToken.getOtherClaims().get("from-mapper"));
        assertNotNull(idToken.getOtherClaims().get("from-dedicated-mapper"));
        // claim mapped by dedicated client mapper
        assertEquals("value", idToken.getOtherClaims().get("from-dedicated-mapper"));

        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertTrue(accessToken.getScope().contains(expectedScopeName));
        assertNotNull(accessToken.getOtherClaims());
        assertNotNull(accessToken.getOtherClaims().get("from-mapper"));
        // claim mapped by client scope mapper
        assertEquals("value", accessToken.getOtherClaims().get("from-mapper"));
        assertNotNull(accessToken.getOtherClaims().get("from-dedicated-mapper"));
        // claim mapped by dedicated client mapper
        assertEquals("value", accessToken.getOtherClaims().get("from-dedicated-mapper"));
    }

    @Test
    public void testStaticScopeUsingDynamicScopeFormatPrefixedWithScopeAsDefaultScope() {
        RealmResource realm = adminClient.realm("test");
        ClientResource clientResource = findClientResourceByClientId(realm, "test-app");
        ClientRepresentation client = clientResource.toRepresentation();

        // make sure the name of the client maps to the prefix of the dynamic scope name
        client.setName("test");
        clientResource.update(client);

        // creates a client scope using the dynamic scope format and add it to the client as default scope
        createClientScope(realm, clientResource, "test", "from-scope-mapper", "value", true);
        createClientScope(realm, clientResource, "test:create", "from-dynamic-scope", "value", true);

        oauth.scope("openid test:create");
        AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user@localhost", "password");
        assertTrue(response.getScope().contains("test:create"));

        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertTrue(accessToken.getScope().contains("test:create"));
        assertNotNull(accessToken.getOtherClaims());
        assertNotNull(accessToken.getOtherClaims().get("from-dynamic-scope"));
        assertEquals("value", accessToken.getOtherClaims().get("from-dynamic-scope"));
        assertNotNull(accessToken.getOtherClaims().get("from-scope-mapper"));
        assertEquals("value", accessToken.getOtherClaims().get("from-scope-mapper"));
    }

    @Test
    public void testStaticScopeUsingDynamicScopeFormatPrefixedWithScopeAsOptionalScope() {
        RealmResource realm = adminClient.realm("test");
        ClientResource clientResource = findClientResourceByClientId(realm, "test-app");
        ClientRepresentation client = clientResource.toRepresentation();

        // make sure the name of the client maps to the prefix of the dynamic scope name
        client.setName("test");
        clientResource.update(client);

        // creates a client scope using the dynamic scope format and add it to the client as optional scope
        createClientScope(realm, clientResource, "test", "from-scope-mapper", "value", false);
        createClientScope(realm, clientResource, "test:create", "from-dynamic-scope", "value", false);

        oauth.scope("openid test:create");
        AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user@localhost", "password");
        assertTrue(response.getScope().contains("test:create"));

        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertTrue(accessToken.getScope().contains("test:create"));
        assertNotNull(accessToken.getOtherClaims());
        assertNotNull(accessToken.getOtherClaims().get("from-dynamic-scope"));
        // claim mapped by client scope mapper
        assertEquals("value", accessToken.getOtherClaims().get("from-dynamic-scope"));
    }

    private void createClientScope(RealmResource realm, ClientResource clientResource, String name, String claim, String value, boolean isDefault) {
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName(name);
        scopeRep.setProtocol("openid-connect");
        scopeRep.setProtocolMappers(Collections.singletonList(createHardCodedMapper(name + "from-scope-mapper", claim, value)));
        try (Response resp1 = realm.clientScopes().create(scopeRep)) {
            assertEquals(201, resp1.getStatus());
            String clientScopeId = ApiUtil.getCreatedId(resp1);
            getCleanup().addClientScopeId(clientScopeId);

            if (isDefault) {
                clientResource.addDefaultClientScope(clientScopeId);
            } else {
                clientResource.addOptionalClientScope(clientScopeId);
            }
        }
    }

    private void assertRoles(List<String> actualRoleList, String ...expectedRoles){
        Assert.assertNames(actualRoleList, expectedRoles);
    }

    private void assertRolesString(List<String> actualRoleString, String...expectedRoles) {
        assertThat(actualRoleString, containsInAnyOrder(expectedRoles));
    }

    private void assertSingleValuedRolesString(String actualRoleString, String... expectedRoles) {
        assertThat(actualRoleString, is(in(expectedRoles)));
    }

    private AccessTokenResponse browserLogin(String username, String password) {
        AuthorizationEndpointResponse authzEndpointResponse = oauth.doLogin(username, password);
        return oauth.doAccessTokenRequest(authzEndpointResponse.getCode());
    }

    public ProtocolMapperRepresentation createHardCodedMapper(String name, String claim, String value) {
        return createHardcodedClaim(name, claim, value, "String", true, true, true);
    }
}
