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

package org.keycloak.testsuite.federation.kerberos;

import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

import java.net.URI;
import java.security.Principal;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.security.sasl.Sasl;
import javax.ws.rs.core.Response;

import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.ietf.jgss.GSSCredential;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.HttpClientBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authentication.authenticators.browser.SpnegoAuthenticatorFactory;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.events.Details;
import org.keycloak.federation.kerberos.CommonKerberosConfig;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.pages.AccountPasswordPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.KerberosRule;
import org.keycloak.testsuite.util.OAuthClient;

/**
 * Contains just helper methods. No test methods.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public abstract class AbstractKerberosTest extends AbstractAuthTest {

    protected KeycloakSPNegoSchemeFactory spnegoSchemeFactory;

    protected ResteasyClient client;

    @Page
    protected LoginPage loginPage;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AccountPasswordPage changePasswordPage;

    protected abstract KerberosRule getKerberosRule();

    protected abstract CommonKerberosConfig getKerberosConfig();

    protected abstract ComponentRepresentation getUserStorageConfiguration();


    protected ComponentRepresentation getUserStorageConfiguration(String providerName, String providerId) {
        Map<String,String> kerberosConfig = getKerberosRule().getConfig();
        MultivaluedHashMap<String, String> config = toComponentConfig(kerberosConfig);

        UserStorageProviderModel model = new UserStorageProviderModel();
        model.setLastSync(0);
        model.setChangedSyncPeriod(-1);
        model.setFullSyncPeriod(-1);
        model.setName(providerName);
        model.setPriority(0);
        model.setProviderId(providerId);
        model.setConfig(config);

        ComponentRepresentation rep = ModelToRepresentation.toRepresentationWithoutConfig(model);
        return rep;
    }


    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realmRep = loadJson(getClass().getResourceAsStream("/kerberos/kerberosrealm.json"), RealmRepresentation.class);
        testRealms.add(realmRep);
    }

    @Override
    public RealmResource testRealmResource() {
        return adminClient.realm("test");
    }


    @Before
    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();

        testRealmPage.setAuthRealm(AuthRealm.TEST);
        changePasswordPage.realm(AuthRealm.TEST);

        getKerberosRule().setKrb5ConfPath(testingClient.testing());

        spnegoSchemeFactory = new KeycloakSPNegoSchemeFactory(getKerberosConfig());
        initHttpClient(true);
        removeAllUsers();

        oauth.clientId("kerberos-app");

        ComponentRepresentation rep = getUserStorageConfiguration();
        Response resp = testRealmResource().components().add(rep);
        getCleanup().addComponentId(ApiUtil.getCreatedId(resp));
        resp.close();
    }

    @After
    @Override
    public void afterAbstractKeycloakTest() {
        cleanupApacheHttpClient();

        super.afterAbstractKeycloakTest();
    }

    private void cleanupApacheHttpClient() {
        client.close();
        client = null;
    }

//    @Test
//    public void sleepTest() throws Exception {
//        String kcLoginPageLocation = oauth.getLoginFormUrl();
//        Thread.sleep(10000000);
//    }


    protected AccessToken assertSuccessfulSpnegoLogin(String loginUsername, String expectedUsername, String password) throws Exception {
        return assertSuccessfulSpnegoLogin("kerberos-app", loginUsername, expectedUsername, password);
    }

    protected AccessToken assertSuccessfulSpnegoLogin(String clientId, String loginUsername, String expectedUsername, String password) throws Exception {
        oauth.clientId(clientId);
        Response spnegoResponse = spnegoLogin(loginUsername, password);
        Assert.assertEquals(302, spnegoResponse.getStatus());

        List<UserRepresentation> users = testRealmResource().users().search(expectedUsername, 0, 1);
        String userId = users.get(0).getId();
        events.expectLogin()
                .client(clientId)
                .user(userId)
                .detail(Details.USERNAME, expectedUsername)
                .assertEvent();

        String codeUrl = spnegoResponse.getLocation().toString();

        OAuthClient.AccessTokenResponse tokenResponse = assertAuthenticationSuccess(codeUrl);

        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        Assert.assertEquals(userId, token.getSubject());
        Assert.assertEquals(expectedUsername, token.getPreferredUsername());

        return token;
    }


    protected String invokeLdap(GSSCredential gssCredential, String username) throws NamingException {
        Hashtable env = new Hashtable(11);
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://localhost:10389");

        if (gssCredential != null) {
            env.put(Context.SECURITY_AUTHENTICATION, "GSSAPI");
            env.put(Sasl.CREDENTIALS, gssCredential);
        }

        DirContext ctx = new InitialDirContext(env);
        try {
            Attributes attrs = ctx.getAttributes("uid=" + username + ",ou=People,dc=keycloak,dc=org");
            String cn = (String) attrs.get("cn").get();
            String sn = (String) attrs.get("sn").get();
            return cn + " " + sn;
        } finally {
            ctx.close();
        }
    }


    protected Response spnegoLogin(String username, String password) {
        String kcLoginPageLocation = oauth.getLoginFormUrl();

        // Request for SPNEGO login sent with Resteasy client
        spnegoSchemeFactory.setCredentials(username, password);
        Response response = client.target(kcLoginPageLocation).request().get();
        if (response.getStatus() == 302) {
            if (response.getLocation() == null)
                return response;
            String uri = response.getLocation().toString();
            if (uri.contains("login-actions/required-action") || uri.contains("auth_session_id")) {
                response = client.target(uri).request().get();
            }
        }
        return response;

    }


    protected void initHttpClient(boolean useSpnego) {
        if (client != null) {
            cleanupApacheHttpClient();
        }
        
        DefaultHttpClient httpClient = (DefaultHttpClient) new HttpClientBuilder()
                .disableCookieCache(false)
                .build();

        httpClient.getAuthSchemes().register(AuthSchemes.SPNEGO, spnegoSchemeFactory);

        if (useSpnego) {
            Credentials fake = new Credentials() {

                @Override
                public String getPassword() {
                    return null;
                }

                @Override
                public Principal getUserPrincipal() {
                    return null;
                }

            };

            httpClient.getCredentialsProvider().setCredentials(
                    new AuthScope(null, -1, null),
                    fake);
        }
        ApacheHttpClient4Engine engine = new ApacheHttpClient4Engine(httpClient);
        client = new ResteasyClientBuilder().httpEngine(engine).build();
    }


    protected void removeAllUsers() {
        RealmResource realm = testRealmResource();
        List<UserRepresentation> users = realm.users().search("", 0, Integer.MAX_VALUE);
        for (UserRepresentation user : users) {
            if (!user.getUsername().equals(AssertEvents.DEFAULT_USERNAME)) {
                realm.users().get(user.getId()).remove();
            }
        }
        Assert.assertEquals(1, realm.users().search("", 0, Integer.MAX_VALUE).size());
    }


    protected void assertUser(String expectedUsername, String expectedEmail, String expectedFirstname,
                              String expectedLastname, boolean updateProfileActionExpected) {
        try {
            UserRepresentation user = ApiUtil.findUserByUsername(testRealmResource(), expectedUsername);
            Assert.assertNotNull(user);
            Assert.assertEquals(expectedEmail, user.getEmail());
            Assert.assertEquals(expectedFirstname, user.getFirstName());
            Assert.assertEquals(expectedLastname, user.getLastName());

            if (updateProfileActionExpected) {
                Assert.assertEquals(UserModel.RequiredAction.UPDATE_PROFILE.toString(),
                        user.getRequiredActions().iterator().next());
            } else {
                Assert.assertTrue(user.getRequiredActions().isEmpty());
            }
        } finally {
        }
    }


    protected OAuthClient.AccessTokenResponse assertAuthenticationSuccess(String codeUrl) throws Exception {
        List<NameValuePair> pairs = URLEncodedUtils.parse(new URI(codeUrl), "UTF-8");
        String code = null;
        String state = null;
        for (NameValuePair pair : pairs) {
            if (pair.getName().equals(OAuth2Constants.CODE)) {
                code = pair.getValue();
            } else if (pair.getName().equals(OAuth2Constants.STATE)) {
                state = pair.getValue();
            }
        }
        Assert.assertNotNull(code);
        Assert.assertNotNull(state);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        Assert.assertNotNull(response.getAccessToken());
        events.clear();
        return response;
    }


    protected void updateProviderEditMode(UserStorageProvider.EditMode editMode) {
        updateUserStorageProvider(kerberosProvider -> kerberosProvider.getConfig().putSingle(LDAPConstants.EDIT_MODE, editMode.toString()));
    }

    protected void updateProviderValidatePasswordPolicy(Boolean validatePasswordPolicy) {
        updateUserStorageProvider(kerberosProvider -> kerberosProvider.getConfig().putSingle(LDAPConstants.VALIDATE_PASSWORD_POLICY, validatePasswordPolicy.toString()));
    }


    /**
     * Update UserStorage provider (Kerberos provider or LDAP provider with Kerberos enabled) with specified updater and save it
     *
     */
    protected void updateUserStorageProvider(Consumer<ComponentRepresentation> updater) {
        List<ComponentRepresentation> reps = testRealmResource().components().query("test", UserStorageProvider.class.getName());
        Assert.assertEquals(1, reps.size());
        ComponentRepresentation kerberosProvider = reps.get(0);

        updater.accept(kerberosProvider);

        testRealmResource().components().component(kerberosProvider.getId()).update(kerberosProvider);
    }


    protected AuthenticationExecutionModel.Requirement updateKerberosAuthExecutionRequirement(AuthenticationExecutionModel.Requirement requirement) {
        Optional<AuthenticationExecutionInfoRepresentation> kerberosAuthExecutionOpt = testRealmResource()
                .flows()
                .getExecutions(DefaultAuthenticationFlows.BROWSER_FLOW)
                .stream()
                .filter(e -> e.getProviderId().equals(SpnegoAuthenticatorFactory.PROVIDER_ID))
                .findFirst();

        Assert.assertTrue(kerberosAuthExecutionOpt.isPresent());

        AuthenticationExecutionInfoRepresentation kerberosAuthExecution = kerberosAuthExecutionOpt.get();
        String oldRequirementStr = kerberosAuthExecution.getRequirement();
        AuthenticationExecutionModel.Requirement oldRequirement = AuthenticationExecutionModel.Requirement.valueOf(oldRequirementStr);
        kerberosAuthExecution.setRequirement(requirement.name());

        testRealmResource()
                .flows()
                .updateExecutions(DefaultAuthenticationFlows.BROWSER_FLOW, kerberosAuthExecution);

        return oldRequirement;
    }


    private static MultivaluedHashMap<String, String> toComponentConfig(Map<String, String> ldapConfig) {
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        for (Map.Entry<String, String> entry : ldapConfig.entrySet()) {
            config.add(entry.getKey(), entry.getValue());

        }
        return config;
    }

}
