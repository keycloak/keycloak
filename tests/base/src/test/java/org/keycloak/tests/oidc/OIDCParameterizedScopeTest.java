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
package org.keycloak.tests.oidc;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.admin.AdminApiUtil;

import org.junit.jupiter.api.Test;


/**
 * Extend another tests class {@link OIDCScopeTest} in order to repeat all the tests but with PARAMETERIZED_SCOPES enabled
 * to make sure that retro compatibility is maintained when the feature is enabled.
 *
 * @author <a href="mailto:dgozalob@redhat.com">Daniel Gozalo</a>
 */
@KeycloakIntegrationTest(config = OIDCParameterizedScopeTest.ServerConfig.class)
public class OIDCParameterizedScopeTest extends OIDCScopeTest {

    @Test
    public void testGetAccessTokenWithParameterizedScope() {
        clientConfiguration();
        Response response = createParameterizedScope();
        String scopeId = ApiUtil.getCreatedId(response);
        response.close();

        ClientResource testApp = AdminApiUtil.findClientByClientId(managedRealm.admin(), "test-app");
        ClientRepresentation testAppRep = testApp.toRepresentation();
        testApp.update(testAppRep);
        testApp.addOptionalClientScope(scopeId);

        oauth.scope("dynamic:scope");
        testLoginAndClientScopesPermissions("johnNormal", "dynamic:scope", "role-1");

        //cleanup
        testApp.removeOptionalClientScope(scopeId);
    }

    @Test
    public void testGetAccessTokenWithParameterizedScopeWithPermittedRoleScope() {
        clientConfiguration();
        Response response = createParameterizedScope();
        String scopeId = ApiUtil.getCreatedId(response);
        response.close();

        List<RoleRepresentation> parameterizedScopeRoleList = managedRealm.admin().roles().list().stream()
                .filter(roleRepresentation -> "dynamic-scope-role".equalsIgnoreCase(roleRepresentation.getName()))
                .collect(Collectors.toList());

        managedRealm.admin().clientScopes().get(scopeId).getScopeMappings().realmLevel().add(parameterizedScopeRoleList);

        ClientResource testApp = AdminApiUtil.findClientByClientId(managedRealm.admin(), "test-app");
        ClientRepresentation testAppRep = testApp.toRepresentation();
        testApp.update(testAppRep);
        testApp.addOptionalClientScope(scopeId);

        oauth.scope("dynamic:scope");
        testLoginAndClientScopesPermissions("johnDynamic", "dynamic:scope", "dynamic-scope-role");

        //cleanup
        testApp.removeOptionalClientScope(scopeId);
    }

    @Test
    public void testGetAccessTokenMissingRoleScopedParameterizedScope() {
        clientConfiguration();
        Response response = createParameterizedScope();
        String scopeId = ApiUtil.getCreatedId(response);
        response.close();

        List<RoleRepresentation> parameterizedScopeRoleList = managedRealm.admin().roles().list().stream()
                .filter(roleRepresentation -> "dynamic-scope-role".equalsIgnoreCase(roleRepresentation.getName()))
                .collect(Collectors.toList());

        managedRealm.admin().clientScopes().get(scopeId).getScopeMappings().realmLevel().add(parameterizedScopeRoleList);

        ClientResource testApp = AdminApiUtil.findClientByClientId(managedRealm.admin(), "test-app");
        ClientRepresentation testAppRep = testApp.toRepresentation();
        testApp.update(testAppRep);
        testApp.addOptionalClientScope(scopeId);

        oauth.scope("dynamic:scope");
        // almost the same test as before, but now with a user that doesn't have the Role scoped parameterized scope attached
        testLoginAndClientScopesPermissions("johnNormal", "", "role-1");

        //cleanup
        testApp.removeOptionalClientScope(scopeId);
    }

    private Response createParameterizedScope() {
        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("dynamic");
        clientScope.setAttributes(new HashMap<>() {{
            put(ClientScopeModel.IS_PARAMETERIZED_SCOPE, "true");
            put(ClientScopeModel.PARAMETERIZED_SCOPE_TYPE, "string");
        }});
        clientScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        return managedRealm.admin().clientScopes().create(clientScope);
    }

    public static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.PARAMETERIZED_SCOPES);
        }
    }
}
