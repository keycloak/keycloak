/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
 *
 */
package org.keycloak.testsuite.oidc;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.util.UserBuilder;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.keycloak.common.Profile.Feature.DYNAMIC_SCOPES;


/**
 * Extend another tests class {@link OIDCScopeTest} in order to repeat all the tests but with DYNAMIC_SCOPES enabled
 * to make sure that retro compatibility is maintained when the feature is enabled.
 *
 * @author <a href="mailto:dgozalob@redhat.com">Daniel Gozalo</a>
 */
@EnableFeature(value = Profile.Feature.DYNAMIC_SCOPES, skipRestart = true)
public class OIDCDynamicScopeTest extends OIDCScopeTest {

    private static String userId = KeycloakModelUtils.generateId();

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        super.configureTestRealm(testRealm);
        UserRepresentation user = UserBuilder.create()
                .id(userId)
                .username("johnDynamic")
                .enabled(true)
                .email("johnDynamic@scopes.xyz")
                .firstName("John")
                .lastName("Dynamic")
                .password("password")
                .addRoles("dynamic-scope-role")
                .build();
        testRealm.getUsers().add(user);

        user = UserBuilder.create()
                .username("JohnNormal")
                .enabled(true)
                .password("password")
                .addRoles("role-1")
                .build();
        testRealm.getUsers().add(user);

        // Add sample realm roles
        RoleRepresentation dynamicScopeRole = new RoleRepresentation();
        dynamicScopeRole.setName("dynamic-scope-role");
        testRealm.getRoles().getRealm().add(dynamicScopeRole);
    }

    @Before
    public void assertDynamicScopesFeatureEnabled() {
        ProfileAssume.assumeFeatureEnabled(DYNAMIC_SCOPES);
    }

    @Override
    public void testBuiltinOptionalScopes() throws Exception {
        super.testBuiltinOptionalScopes();
    }

    @Override
    public void testRemoveScopes() throws Exception {
        super.testRemoveScopes();
    }

    @Override
    public void testClientScopesPermissions() {
        super.testClientScopesPermissions();
    }

    @Override
    public void testClientDisplayedOnConsentScreen() throws Exception {
        super.testClientDisplayedOnConsentScreen();
    }

    @Override
    public void testClientDisplayedOnConsentScreenWithEmptyConsentText() throws Exception {
        super.testClientDisplayedOnConsentScreenWithEmptyConsentText();
    }

    @Override
    public void testOptionalScopesWithConsentRequired() throws Exception {
        super.testOptionalScopesWithConsentRequired();
    }

    @Override
    public void testRefreshTokenWithConsentRequired() {
        super.testRefreshTokenWithConsentRequired();
    }

    @Override
    public void testTwoRefreshTokensWithDifferentScopes() {
        super.testTwoRefreshTokensWithDifferentScopes();
    }

    @Test
    public void testGetAccessTokenWithDynamicScope() {
        Response response = createDynamicScope("dynamic");
        String scopeId = ApiUtil.getCreatedId(response);
        getCleanup().addClientScopeId(scopeId);
        response.close();

        ClientResource testApp = ApiUtil.findClientByClientId(testRealm(), "test-app");
        ClientRepresentation testAppRep = testApp.toRepresentation();
        testApp.update(testAppRep);
        testApp.addOptionalClientScope(scopeId);

        oauth.scope("dynamic:scope");
        testLoginAndClientScopesPermissions("johnNormal", "dynamic:scope", "role-1");

        //cleanup
        testApp.removeOptionalClientScope(scopeId);
    }

    @Test
    public void testGetAccessTokenWithDynamicScopeWithPermittedRoleScope() {
        Response response = createDynamicScope("dynamic");
        String scopeId = ApiUtil.getCreatedId(response);
        getCleanup().addClientScopeId(scopeId);
        response.close();

        List<RoleRepresentation> dynamicScopeRoleList = testRealm().roles().list().stream()
                .filter(roleRepresentation -> "dynamic-scope-role".equalsIgnoreCase(roleRepresentation.getName()))
                .collect(Collectors.toList());

        testRealm().clientScopes().get(scopeId).getScopeMappings().realmLevel().add(dynamicScopeRoleList);

        ClientResource testApp = ApiUtil.findClientByClientId(testRealm(), "test-app");
        ClientRepresentation testAppRep = testApp.toRepresentation();
        testApp.update(testAppRep);
        testApp.addOptionalClientScope(scopeId);

        oauth.scope("dynamic:scope");
        testLoginAndClientScopesPermissions("johnDynamic", "dynamic:scope", "dynamic-scope-role");

        //cleanup
        testApp.removeOptionalClientScope(scopeId);
    }

    @Test
    public void testGetAccessTokenMissingRoleScopedDynamicScope() {
        Response response = createDynamicScope("dynamic");
        String scopeId = ApiUtil.getCreatedId(response);
        getCleanup().addClientScopeId(scopeId);
        response.close();

        List<RoleRepresentation> dynamicScopeRoleList = testRealm().roles().list().stream()
                .filter(roleRepresentation -> "dynamic-scope-role".equalsIgnoreCase(roleRepresentation.getName()))
                .collect(Collectors.toList());

        testRealm().clientScopes().get(scopeId).getScopeMappings().realmLevel().add(dynamicScopeRoleList);

        ClientResource testApp = ApiUtil.findClientByClientId(testRealm(), "test-app");
        ClientRepresentation testAppRep = testApp.toRepresentation();
        testApp.update(testAppRep);
        testApp.addOptionalClientScope(scopeId);

        oauth.scope("dynamic:scope");
        // almost the same test as before, but now with a user that doesn't have the Role scoped dynamic scope attached
        testLoginAndClientScopesPermissions("johnNormal", "", "role-1");

        //cleanup
        testApp.removeOptionalClientScope(scopeId);
    }


    private Response createDynamicScope(String scopeName) {
        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName(scopeName);
        clientScope.setAttributes(new HashMap<String, String>() {{
            put(ClientScopeModel.IS_DYNAMIC_SCOPE, "true");
            put(ClientScopeModel.DYNAMIC_SCOPE_REGEXP, String.format("%1s:*", scopeName));
        }});
        clientScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        return testRealm().clientScopes().create(clientScope);
    }

    /**
     * Copying the same method from {@link OIDCScopeTest} to avoid a change in that test class to affect this one
     *
     * @param username
     * @param expectedRoleScopes
     * @param expectedRoles
     */
    private void testLoginAndClientScopesPermissions(String username, String expectedRoleScopes, String... expectedRoles) {
        String userId = ApiUtil.findUserByUsername(testRealm(), username).getId();

        oauth.openLoginForm();
        oauth.doLogin(username, "password");
        EventRepresentation loginEvent = events.expectLogin()
                .user(userId)
                .assertEvent();

        Tokens tokens = sendTokenRequest(loginEvent, userId, "openid email profile " + expectedRoleScopes, "test-app");
        Assert.assertNames(tokens.accessToken.getRealmAccess().getRoles(), expectedRoles);

        oauth.doLogout(tokens.refreshToken, "password");
        events.expectLogout(tokens.idToken.getSessionState())
                .client("test-app")
                .user(userId)
                .removeDetail(Details.REDIRECT_URI).assertEvent();
    }


}
