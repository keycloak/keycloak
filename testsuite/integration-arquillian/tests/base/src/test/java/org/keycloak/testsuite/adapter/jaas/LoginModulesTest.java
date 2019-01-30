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

package org.keycloak.testsuite.adapter.jaas;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.jaas.AbstractKeycloakLoginModule;
import org.keycloak.adapters.jaas.BearerTokenLoginModule;
import org.keycloak.adapters.jaas.DirectAccessGrantsLoginModule;
import org.keycloak.adapters.jaas.RolePrincipal;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.utils.io.IOUtil;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LoginModulesTest extends AbstractKeycloakTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(IOUtil.loadRealm("/adapter-test/demorealm.json"));
    }

    @Before
    public void generateAudienceClientScope() {
        if (ApiUtil.findClientScopeByName(adminClient.realm("demo"), "customer-db-audience-required") != null) {
            return;
        }

        // Generate audience client scope
        Response resp = adminClient.realm("demo").clientScopes().generateAudienceClientScope("customer-db-audience-required");
        String clientScopeId = ApiUtil.getCreatedId(resp);
        resp.close();
        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm("demo"), "customer-portal");
        client.addOptionalClientScope(clientScopeId);
    }


    @Test
    public void testDirectAccessGrantLoginModuleLoginFailed() throws Exception {
        LoginContext loginContext = new LoginContext("does-not-matter", null,
                createJaasCallbackHandler("bburke@redhat.com", "bad-password"),
                createJaasConfigurationForDirectGrant(null));

        try {
            loginContext.login();
            Assert.fail("Not expected to successfully login");
        } catch (LoginException le) {
            // Ignore
        }
    }


    @Test
    public void testDirectAccessGrantLoginModuleLoginSuccess() throws Exception {
        oauth.realm("demo");

        LoginContext loginContext = directGrantLogin(null);
        Subject subject = loginContext.getSubject();

        // Assert principals in subject
        KeycloakPrincipal principal = subject.getPrincipals(KeycloakPrincipal.class).iterator().next();
        Assert.assertEquals("bburke@redhat.com", principal.getKeycloakSecurityContext().getToken().getPreferredUsername());
        assertToken(principal.getKeycloakSecurityContext().getTokenString(), true);

        Set<RolePrincipal> roles = subject.getPrincipals(RolePrincipal.class);
        Assert.assertEquals(1, roles.size());
        Assert.assertEquals("user", roles.iterator().next().getName());

        // Logout and assert token not valid anymore
        loginContext.logout();
        assertToken(principal.getKeycloakSecurityContext().getTokenString(), false);
    }


    @Test
    public void testBearerLoginFailedLogin() throws Exception {
        oauth.realm("demo");

        LoginContext directGrantCtx = directGrantLogin(null);
        String accessToken = directGrantCtx.getSubject().getPrincipals(KeycloakPrincipal.class).iterator().next()
                .getKeycloakSecurityContext().getTokenString();

        LoginContext bearerCtx = new LoginContext("does-not-matter", null,
                createJaasCallbackHandler("doesn-not-matter", accessToken),
                createJaasConfigurationForBearer());

        // Login should fail due insufficient audience in the token
        try {
            bearerCtx.login();
            Assert.fail("Not expected to successfully login");
        } catch (LoginException le) {
            // Ignore
        }

        directGrantCtx.logout();
    }


    @Test
    public void testBearerLoginSuccess() throws Exception {
        oauth.realm("demo");

        LoginContext directGrantCtx = directGrantLogin("customer-db-audience-required");
        String accessToken = directGrantCtx.getSubject().getPrincipals(KeycloakPrincipal.class).iterator().next()
                .getKeycloakSecurityContext().getTokenString();

        LoginContext bearerCtx = new LoginContext("does-not-matter", null,
                createJaasCallbackHandler("doesn-not-matter", accessToken),
                createJaasConfigurationForBearer());

        // Login should be successful
        bearerCtx.login();

        // Assert subject
        Subject subject = bearerCtx.getSubject();

        KeycloakPrincipal principal = subject.getPrincipals(KeycloakPrincipal.class).iterator().next();
        Assert.assertEquals("bburke@redhat.com", principal.getKeycloakSecurityContext().getToken().getPreferredUsername());
        assertToken(principal.getKeycloakSecurityContext().getTokenString(), true);

        Set<RolePrincipal> roles = subject.getPrincipals(RolePrincipal.class);
        Assert.assertEquals(1, roles.size());
        Assert.assertEquals("user", roles.iterator().next().getName());

        // Logout
        bearerCtx.logout();
        directGrantCtx.logout();
    }


    private LoginContext directGrantLogin(String scope) throws LoginException {
        LoginContext loginContext = new LoginContext("does-not-matter", null,
                createJaasCallbackHandler("bburke@redhat.com", "password"),
                createJaasConfigurationForDirectGrant(scope));

        loginContext.login();

        return loginContext;
    }


    private void assertToken(String accessToken, boolean expectActive) throws IOException {
        String introspectionResponse = oauth.introspectAccessTokenWithClientCredential("customer-portal", "password", accessToken);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(introspectionResponse);
        Assert.assertEquals(expectActive, jsonNode.get("active").asBoolean());
    }


    private CallbackHandler createJaasCallbackHandler(final String principal, final String password) {
        return new CallbackHandler() {

            @Override
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                for (Callback callback : callbacks) {
                    if (callback instanceof NameCallback) {
                        NameCallback nameCallback = (NameCallback) callback;
                        nameCallback.setName(principal);
                    } else if (callback instanceof PasswordCallback) {
                        PasswordCallback passwordCallback = (PasswordCallback) callback;
                        passwordCallback.setPassword(password.toCharArray());
                    } else {
                        throw new UnsupportedCallbackException(callback, "Unsupported callback: " + callback.getClass().getCanonicalName());
                    }
                }
            }
        };
    }


    private Configuration createJaasConfigurationForDirectGrant(String scope) {
        return new Configuration() {

            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                Map<String, Object> options = new HashMap<>();
                options.put(AbstractKeycloakLoginModule.KEYCLOAK_CONFIG_FILE_OPTION, "classpath:adapter-test/customer-portal/WEB-INF/keycloak.json");
                if (scope != null) {
                    options.put(DirectAccessGrantsLoginModule.SCOPE_OPTION, scope);
                }

                AppConfigurationEntry LMConfiguration = new AppConfigurationEntry(DirectAccessGrantsLoginModule.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options);
                return new AppConfigurationEntry[] { LMConfiguration };
            }
        };
    }


    private Configuration createJaasConfigurationForBearer() {
        return new Configuration() {

            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                Map<String, Object> options = new HashMap<>();
                options.put(AbstractKeycloakLoginModule.KEYCLOAK_CONFIG_FILE_OPTION, "classpath:adapter-test/customer-db-audience-required/WEB-INF/keycloak.json");

                AppConfigurationEntry LMConfiguration = new AppConfigurationEntry(BearerTokenLoginModule.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options);
                return new AppConfigurationEntry[] { LMConfiguration };
            }
        };
    }
}
