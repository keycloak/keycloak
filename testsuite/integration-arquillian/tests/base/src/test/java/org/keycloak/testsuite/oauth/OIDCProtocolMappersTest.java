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
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.util.UriUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AddressMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AddressClaimSet;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.ProtocolMapperUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
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

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OIDCProtocolMappersTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);


    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
    }

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
        protocolMappers.delete(mapper.getId());

        mapper = ProtocolMapperUtil.getMapperByNameAndProtocol(protocolMappers, OIDCLoginProtocol.LOGIN_PROTOCOL, "Client roles mapper");
        protocolMappers.delete(mapper.getId());
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realm);
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

            List<String> departments = Arrays.asList("finance", "development");
            user.getAttributes().put("departments", departments);
            userResource.update(user);

            ClientResource app = findClientResourceByClientId(adminClient.realm("test"), "test-app");

            ProtocolMapperRepresentation mapper = createAddressMapper(true, true);
            mapper.getConfig().put(AddressMapper.getModelPropertyName(AddressClaimSet.REGION), "region_some");
            mapper.getConfig().put(AddressMapper.getModelPropertyName(AddressClaimSet.COUNTRY), "country_some");
            mapper.getConfig().remove(AddressMapper.getModelPropertyName(AddressClaimSet.POSTAL_CODE)); // Even if we remove protocolMapper config property, it should still default to postal_code
            app.getProtocolMappers().createMapper(mapper).close();

            ProtocolMapperRepresentation hard = createHardcodedClaim("hard", "hard", "coded", "String", false, null, true, true);
            app.getProtocolMappers().createMapper(hard).close();
            app.getProtocolMappers().createMapper(createHardcodedClaim("hard-nested", "nested.hard", "coded-nested", "String", false, null, true, true)).close();
            app.getProtocolMappers().createMapper(createClaimMapper("custom phone", "phone", "home_phone", "String", true, "", true, true, true)).close();
            app.getProtocolMappers().createMapper(createClaimMapper("nested phone", "phone", "home.phone", "String", true, "", true, true, true)).close();
            app.getProtocolMappers().createMapper(createClaimMapper("departments", "departments", "department", "String", true, "", true, true, true)).close();
            app.getProtocolMappers().createMapper(createClaimMapper("firstDepartment", "departments", "firstDepartment", "String", true, "", true, true, false)).close();
            app.getProtocolMappers().createMapper(createHardcodedRole("hard-realm", "hardcoded")).close();
            app.getProtocolMappers().createMapper(createHardcodedRole("hard-app", "app.hardcoded")).close();
            app.getProtocolMappers().createMapper(createRoleNameMapper("rename-app-role", "test-app.customer-user", "realm-user")).close();
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
            Assert.assertFalse(accessToken.getResourceAccess("test-app").getRoles().contains("customer-user"));
            assertTrue(accessToken.getResourceAccess("app").getRoles().contains("hardcoded"));

            oauth.openLogout();
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
                        || model.getName().equals("departments")
                        || model.getName().equals("firstDepartment")
                        || model.getName().equals("nested phone")
                        || model.getName().equals("rename-app-role")
                        || model.getName().equals("hard-realm")
                        || model.getName().equals("hard-app")
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

            oauth.openLogout();
        }


        events.clear();
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
            app.getProtocolMappers().createMapper(createClaimMapper("empty", "empty", "empty", "String", true, "", true, true, false)).close();
            app.getProtocolMappers().createMapper(createClaimMapper("null", "null", "null", "String", true, "", true, true, false)).close();
        }

        {
            OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password");

            IDToken idToken = oauth.verifyIDToken(response.getIdToken());
            Object empty = idToken.getOtherClaims().get("empty");
            assertThat((empty == null ? null : (String) empty), isEmptyString());
            Object nulll = idToken.getOtherClaims().get("null");
            assertNull(nulll);

            AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
            oauth.openLogout();
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

            oauth.openLogout();
        }
        events.clear();
    }



    @Test
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

    @Test
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
        Assert.assertThat(roleMappings.keySet(), containsInAnyOrder("realm", clientId));
        String realmRoleMappings = (String) roleMappings.get("realm");
        String testAppAuthzMappings = (String) roleMappings.get(clientId);
        assertRolesString(realmRoleMappings,
          "pref.admin",                     // from direct assignment to /roleRichGroup/level2group
          "pref.user",                      // from parent group of /roleRichGroup/level2group, i.e. from /roleRichGroup
          "pref.customer-user-premium",     // from client role customer-admin-composite-role - realm role for test-app
          "pref.realm-composite-role",      // from parent group of /roleRichGroup/level2group, i.e. from /roleRichGroup
          "pref.sample-realm-role"          // from realm role realm-composite-role
        );
        assertRolesString(testAppAuthzMappings);  // There is no client role defined for test-app-authz

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
          "pref.user"                       // from parent group of /roleRichGroup/level2group, i.e. from /roleRichGroup
        );
        assertRolesString(testAppScopeMappings,
          "test-app-allowed-by-scope"       // from direct assignment to roleRichUser, present as scope allows it
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
          "pref.user"                       // from parent group of /roleRichGroup/level2group, i.e. from /roleRichGroup
        );
        assertRolesString(testAppScopeMappings,
          "test-app-allowed-by-scope",      // from direct assignment to roleRichUser, present as scope allows it
          "customer-admin-composite-role"   // from direct assignment to /roleRichGroup/level2group, present as scope allows it
        );

        // Revert
        deleteMappers(protocolMappers);
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
        rep.setConsentRequired(true);
        rep.setConsentText("Test Consent Text");
        return rep;
    }

    private OAuthClient.AccessTokenResponse browserLogin(String clientSecret, String username, String password) {
        OAuthClient.AuthorizationEndpointResponse authzEndpointResponse = oauth.doLogin(username, password);
        return oauth.doAccessTokenRequest(authzEndpointResponse.getCode(), clientSecret);
    }

}
