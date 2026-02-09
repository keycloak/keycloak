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

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
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

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authentication.authenticators.browser.SpnegoAuthenticatorFactory;
import org.keycloak.common.constants.KerberosConstants;
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
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.KerberosRule;
import org.keycloak.testsuite.util.KerberosUtils;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.ietf.jgss.GSSCredential;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;

import static org.keycloak.testsuite.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 * Contains just helper methods. No test methods.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractKerberosTest extends AbstractAuthTest {

    protected KeycloakSPNegoSchemeFactory spnegoSchemeFactory;

    protected ResteasyClient client;

    @Page
    protected LoginPage loginPage;

    @Page
    protected AppPage appPage;

    @Rule
    public AssertEvents events = new AssertEvents(this);

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

    @BeforeClass
    public static void checkKerberosSupportedByAuthServer() {
        KerberosUtils.assumeKerberosSupportExpected();
    }

    @Before
    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();

        testRealmPage.setAuthRealm(TEST);

        getKerberosRule().setKrb5ConfPath(testingClient.testing());

        spnegoSchemeFactory = new KeycloakSPNegoSchemeFactory(getKerberosConfig());
        initHttpClient(true);
        removeAllUsers();

        oauth.client("kerberos-app", "password");

        ComponentRepresentation rep = getUserStorageConfiguration();
        Response resp = testRealmResource().components().add(rep);
        getCleanup().addComponentId(ApiUtil.getCreatedId(resp));
        resp.close();
    }

    @After
    @Override
    public void afterAbstractKeycloakTest() throws Exception {
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


    protected AccessTokenResponse assertSuccessfulSpnegoLogin(String loginUsername, String expectedUsername, String password) throws Exception {
        return assertSuccessfulSpnegoLogin("kerberos-app", loginUsername, expectedUsername, password);
    }

    protected AccessTokenResponse assertSuccessfulSpnegoLogin(String clientId, String loginUsername, String expectedUsername, String password) throws Exception {
        events.clear();
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

        AccessTokenResponse tokenResponse = assertAuthenticationSuccess(codeUrl);

        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        Assert.assertEquals(userId, token.getSubject());
        Assert.assertEquals(expectedUsername, token.getPreferredUsername());

        return tokenResponse;
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
        String kcLoginPageLocation = oauth.loginForm().state("spnegoLogin").build();

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

        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        if (useSpnego) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("none", "none"));

            httpClientBuilder.setDefaultAuthSchemeRegistry(RegistryBuilder.<AuthSchemeProvider>create()
                            .register(AuthSchemes.SPNEGO, spnegoSchemeFactory).build())
                    .setDefaultRequestConfig(RequestConfig.copy(RequestConfig.DEFAULT)
                            .setTargetPreferredAuthSchemes(Collections.singletonList(AuthSchemes.SPNEGO)).build())
                    .setDefaultCredentialsProvider(credentialsProvider);
        }
        HttpClient httpClient = httpClientBuilder.build();

        ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine(httpClient);
        client = ((ResteasyClientBuilder) ResteasyClientBuilder.newBuilder()).httpEngine(engine).build();
    }


    protected void removeAllUsers() {
        RealmResource realm = testRealmResource();
        List<UserRepresentation> users = realm.users().search("", 0, -1);
        for (UserRepresentation user : users) {
            if (!user.getUsername().equals(AssertEvents.DEFAULT_USERNAME)) {
                realm.users().get(user.getId()).remove();
            }
        }
        Assert.assertEquals(1, realm.users().search("", 0, -1).size());
    }



    protected UserRepresentation assertUser(String expectedUsername, String expectedEmail, String expectedFirstname,
                                            String expectedLastname, String expectedKerberosPrincipal, boolean updateProfileActionExpected) {
        try {
            UserRepresentation user = ApiUtil.findUserByUsername(testRealmResource(), expectedUsername);
            Assert.assertNotNull(user);
            Assert.assertEquals(expectedEmail, user.getEmail());
            Assert.assertEquals(expectedFirstname, user.getFirstName());
            Assert.assertEquals(expectedLastname, user.getLastName());

            if (expectedKerberosPrincipal == null) {
                Assert.assertNull(user.getAttributes().get(KerberosConstants.KERBEROS_PRINCIPAL));
            } else {
                Assert.assertEquals(expectedKerberosPrincipal, user.getAttributes().get(KerberosConstants.KERBEROS_PRINCIPAL).get(0));
            }

            if (updateProfileActionExpected) {
                Assert.assertEquals(UserModel.RequiredAction.UPDATE_PROFILE.toString(),
                        user.getRequiredActions().iterator().next());
            } else {
                Assert.assertTrue(user.getRequiredActions().isEmpty());
            }
            return user;
        } finally {
        }
    }

    protected void assertUserStorageProvider(UserRepresentation user, String providerName) {
        if (user.getFederationLink() == null) Assert.fail("Federation link on user " + user.getUsername() + " was null");
        ComponentRepresentation rep = testRealmResource().components().component(user.getFederationLink()).toRepresentation();
        Assert.assertEquals(providerName, rep.getName());
    }


    protected AccessTokenResponse assertAuthenticationSuccess(String codeUrl) throws Exception {
        List<NameValuePair> pairs = URLEncodedUtils.parse(new URI(codeUrl), StandardCharsets.UTF_8);
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
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
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
        String parentId = testRealmResource().toRepresentation().getId();
        List<ComponentRepresentation> reps = testRealmResource().components().query(parentId, UserStorageProvider.class.getName());
        Assert.assertEquals(1, reps.size());
        ComponentRepresentation kerberosProvider = reps.get(0);

        updater.accept(kerberosProvider);

        testRealmResource().components().component(kerberosProvider.getId()).update(kerberosProvider);
    }


    protected AuthenticationExecutionModel.Requirement updateKerberosAuthExecutionRequirement(AuthenticationExecutionModel.Requirement requirement) {
        return updateKerberosAuthExecutionRequirement(requirement, testRealmResource());
    }

    public static AuthenticationExecutionModel.Requirement updateKerberosAuthExecutionRequirement(AuthenticationExecutionModel.Requirement requirement, RealmResource realmResource) {
        Optional<AuthenticationExecutionInfoRepresentation> kerberosAuthExecutionOpt = realmResource
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

        realmResource
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
