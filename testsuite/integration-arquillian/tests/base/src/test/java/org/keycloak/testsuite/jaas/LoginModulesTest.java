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

package org.keycloak.testsuite.jaas;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URI;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
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
import static org.keycloak.testsuite.arquillian.AuthServerTestEnricher.AUTH_SERVER_SSL_REQUIRED;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.utils.io.IOUtil;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class LoginModulesTest extends AbstractKeycloakTest {

    public static final URI DIRECT_GRANT_CONFIG;
    public static final URI BEARER_CONFIG;

    private static final File DIRECT_GRANT_CONFIG_FILE;
    private static final File BEARER_CONFIG_FILE;

    static {
        try {
            DIRECT_GRANT_CONFIG = MethodHandles.lookup().lookupClass().getResource("/adapter-test/customer-portal/WEB-INF/keycloak.json").toURI();
            BEARER_CONFIG = MethodHandles.lookup().lookupClass().getResource("/adapter-test/customer-db-audience-required/WEB-INF/keycloak.json").toURI();

            DIRECT_GRANT_CONFIG_FILE = File.createTempFile("LoginModulesTest", "testDirectAccessGrantLoginModuleLoginFailed");
            BEARER_CONFIG_FILE = File.createTempFile("LoginModulesTest", "testBearerLoginFailedLogin");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(IOUtil.loadRealm("/adapter-test/demorealm.json"));
    }

    private static void enabled() {
        Assume.assumeTrue(AUTH_SERVER_SSL_REQUIRED);
    }

    @BeforeClass
    public static void createTemporaryFiles() throws Exception {
        enabled();

        copyContentAndReplaceAuthServerAddress(new File(DIRECT_GRANT_CONFIG), DIRECT_GRANT_CONFIG_FILE);
        copyContentAndReplaceAuthServerAddress(new File(BEARER_CONFIG), BEARER_CONFIG_FILE);
    }

    @AfterClass
    public static void removeTemporaryFiles() {
        DIRECT_GRANT_CONFIG_FILE.deleteOnExit();
        BEARER_CONFIG_FILE.deleteOnExit();
    }

    private static void copyContentAndReplaceAuthServerAddress(File input, File output) throws IOException {
        try (InputStream inputStream = httpsAwareConfigurationStream(new FileInputStream(input))) {
            try (FileOutputStream outputStream = new FileOutputStream(output)) {
                byte[] buffer = new byte[inputStream.available()];
                inputStream.read(buffer);
                outputStream.write(buffer);
            }
        }
    }

    @Before
    public void generateAudienceClientScope() {
        if (ApiUtil.findClientScopeByName(adminClient.realm("demo"), "customer-db-audience-required") != null) {
            return;
        }

        // Generate audience client scope
        String clientScopeId = testingClient.testing().generateAudienceClientScope("demo", "customer-db-audience-required");

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
                options.put(AbstractKeycloakLoginModule.KEYCLOAK_CONFIG_FILE_OPTION, DIRECT_GRANT_CONFIG_FILE.getAbsolutePath());
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
                options.put(AbstractKeycloakLoginModule.KEYCLOAK_CONFIG_FILE_OPTION, BEARER_CONFIG_FILE.getAbsolutePath());

                AppConfigurationEntry LMConfiguration = new AppConfigurationEntry(BearerTokenLoginModule.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options);
                return new AppConfigurationEntry[] { LMConfiguration };
            }
        };
    }
}
