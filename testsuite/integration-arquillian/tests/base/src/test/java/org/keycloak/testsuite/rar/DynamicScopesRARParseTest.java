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
package org.keycloak.testsuite.rar;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.AuthorizationDetailsJSONRepresentation;
import org.keycloak.rar.AuthorizationRequestSource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:dgozalob@redhat.com">Daniel Gozalo</a>
 */
@EnableFeature(value = Profile.Feature.DYNAMIC_SCOPES, skipRestart = true)
public class DynamicScopesRARParseTest extends AbstractRARParserTest {

    @Test
    public void generatedAuthorizationRequestsShouldMatchDefaultScopes() {
        ClientResource testApp = ApiUtil.findClientByClientId(testRealm(), "test-app");
        List<ClientScopeRepresentation> defScopes = testApp.getDefaultClientScopes();
        oauth.openLoginForm();
        oauth.scope("openid");
        oauth.doLogin("rar-test", "password");
        events.expectLogin()
                .user(userId)
                .assertEvent();
        AuthorizationRequestContextHolder contextHolder = fetchAuthorizationRequestContextHolder(userId);
        List<AuthorizationRequestContextHolder.AuthorizationRequestHolder> authorizationRequestHolders = contextHolder.getAuthorizationRequestHolders().stream()
                .filter(authorizationRequestHolder -> authorizationRequestHolder.getSource().equals(AuthorizationRequestSource.SCOPE))
                .collect(Collectors.toList());

        assertEquals(defScopes.size(), authorizationRequestHolders.size());

        assertEquals(defScopes.stream().map(ClientScopeRepresentation::getName).collect(Collectors.toSet()),
                authorizationRequestHolders.stream().map(authorizationRequestHolder -> authorizationRequestHolder.getAuthorizationDetails().getScopeNameFromCustomData())
                        .collect(Collectors.toSet()));

        Assert.assertTrue(authorizationRequestHolders.stream()
                .map(AuthorizationRequestContextHolder.AuthorizationRequestHolder::getAuthorizationDetails)
                .allMatch(rep -> rep.getType().equalsIgnoreCase(AuthorizationDetailsJSONRepresentation.STATIC_SCOPE_RAR_TYPE)));
    }

    @Test
    public void generatedAuthorizationRequestsShouldMatchRequestedAndDefaultScopes() {
        Response response = createScope("static-scope", false);
        String scopeId = ApiUtil.getCreatedId(response);
        getCleanup().addClientScopeId(scopeId);
        response.close();

        ClientResource testApp = ApiUtil.findClientByClientId(testRealm(), "test-app");
        ClientRepresentation testAppRep = testApp.toRepresentation();
        testApp.update(testAppRep);
        testApp.addDefaultClientScope(scopeId);

        List<ClientScopeRepresentation> defScopes = testApp.getDefaultClientScopes();
        oauth.openLoginForm();
        oauth.scope("openid static-scope");
        oauth.doLogin("rar-test", "password");
        events.expectLogin()
                .user(userId)
                .assertEvent();

        AuthorizationRequestContextHolder contextHolder = fetchAuthorizationRequestContextHolder(userId);
        List<AuthorizationRequestContextHolder.AuthorizationRequestHolder> authorizationRequestHolders = contextHolder.getAuthorizationRequestHolders().stream()
                .filter(authorizationRequestHolder -> authorizationRequestHolder.getSource().equals(AuthorizationRequestSource.SCOPE))
                .collect(Collectors.toList());

        assertEquals(defScopes.size(), authorizationRequestHolders.size());

        assertEquals(defScopes.stream().map(ClientScopeRepresentation::getName).collect(Collectors.toSet()),
                authorizationRequestHolders.stream().map(authorizationRequestHolder -> authorizationRequestHolder.getAuthorizationDetails().getScopeNameFromCustomData())
                        .collect(Collectors.toSet()));

        Assert.assertTrue(authorizationRequestHolders.stream()
                .map(AuthorizationRequestContextHolder.AuthorizationRequestHolder::getAuthorizationDetails)
                .allMatch(rep -> rep.getType().equalsIgnoreCase(AuthorizationDetailsJSONRepresentation.STATIC_SCOPE_RAR_TYPE)));

        testApp.removeOptionalClientScope(scopeId);
    }

    @Test
    public void generatedAuthorizationRequestsShouldMatchRequestedDynamicAndDefaultScopes() {
        Response response = createScope("dynamic-scope", true);
        String scopeId = ApiUtil.getCreatedId(response);
        getCleanup().addClientScopeId(scopeId);
        response.close();

        ClientResource testApp = ApiUtil.findClientByClientId(testRealm(), "test-app");
        ClientRepresentation testAppRep = testApp.toRepresentation();
        testApp.update(testAppRep);
        testApp.addOptionalClientScope(scopeId);

        List<ClientScopeRepresentation> defScopes = testApp.getDefaultClientScopes();
        oauth.openLoginForm();
        oauth.scope("openid dynamic-scope:param");
        oauth.doLogin("rar-test", "password");
        events.expectLogin()
                .user(userId)
                .assertEvent();

        AuthorizationRequestContextHolder contextHolder = fetchAuthorizationRequestContextHolder(userId);
        List<AuthorizationRequestContextHolder.AuthorizationRequestHolder> authorizationRequestHolders = contextHolder.getAuthorizationRequestHolders().stream()
                .filter(authorizationRequestHolder -> authorizationRequestHolder.getSource().equals(AuthorizationRequestSource.SCOPE))
                .collect(Collectors.toList());

        assertEquals(defScopes.size(), authorizationRequestHolders.size() - 1);

        Assert.assertFalse(authorizationRequestHolders.stream()
                .map(AuthorizationRequestContextHolder.AuthorizationRequestHolder::getAuthorizationDetails)
                .allMatch(rep -> rep.getType().equalsIgnoreCase(AuthorizationDetailsJSONRepresentation.STATIC_SCOPE_RAR_TYPE)));

        Optional<AuthorizationRequestContextHolder.AuthorizationRequestHolder> authorizationRequestContextHolderOpt = authorizationRequestHolders.stream()
                .filter(authorizationRequestHolder -> authorizationRequestHolder.getAuthorizationDetails().getType().equalsIgnoreCase(AuthorizationDetailsJSONRepresentation.DYNAMIC_SCOPE_RAR_TYPE))
                .findAny();

        Assert.assertTrue(authorizationRequestContextHolderOpt.isPresent());
        AuthorizationRequestContextHolder.AuthorizationRequestHolder authorizationRequestHolder = authorizationRequestContextHolderOpt.get();
        Assert.assertTrue(authorizationRequestHolder.getAuthorizationDetails().getScopeNameFromCustomData().equalsIgnoreCase("dynamic-scope:param"));
        Assert.assertTrue(authorizationRequestHolder.getAuthorizationDetails().getCustomData().get("scope_parameter").equals("param"));

        testApp.removeOptionalClientScope(scopeId);
    }

    private Response createScope(String scopeName, boolean dynamic) {
        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName(scopeName);
        if (dynamic) {
            clientScope.setAttributes(new HashMap<String, String>() {{
                put(ClientScopeModel.IS_DYNAMIC_SCOPE, "true");
                put(ClientScopeModel.DYNAMIC_SCOPE_REGEXP, String.format("%1s:*", scopeName));
            }});
        }
        clientScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        return testRealm().clientScopes().create(clientScope);
    }
}
