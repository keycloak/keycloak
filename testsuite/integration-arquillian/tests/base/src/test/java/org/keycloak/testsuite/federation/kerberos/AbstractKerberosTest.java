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
import static org.keycloak.testsuite.admin.ApiUtil.findClientByClientId;

import java.net.URI;
import java.security.Principal;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.security.sasl.Sasl;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.ietf.jgss.GSSCredential;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.HttpClientBuilder;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.common.util.KerberosSerializationUtils;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.events.Details;
import org.keycloak.federation.kerberos.CommonKerberosConfig;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.mappers.UserSessionNoteMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.pages.AccountPasswordPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.OAuthClient;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractKerberosTest extends AbstractAuthTest {

    protected KeycloakSPNegoSchemeFactory spnegoSchemeFactory;

    protected ResteasyClient client;

    @Page
    protected LoginPage loginPage;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AccountPasswordPage changePasswordPage;

    protected abstract CommonKerberosConfig getKerberosConfig();

    protected abstract ComponentRepresentation getUserStorageConfiguration();

    protected abstract void setKrb5ConfPath();

    protected abstract boolean isStartEmbeddedLdapServer();

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realmRep = loadJson(getClass().getResourceAsStream("/kerberos/kerberosrealm.json"), RealmRepresentation.class);
        testRealms.add(realmRep);
    }


    @Before
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();

        testRealmPage.setAuthRealm(AuthRealm.TEST);
        changePasswordPage.realm(AuthRealm.TEST);

        setKrb5ConfPath();

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


    @Test
    public void spnegoNotAvailableTest() throws Exception {
        initHttpClient(false);

        String kcLoginPageLocation = oauth.getLoginFormUrl();

        Response response = client.target(kcLoginPageLocation).request().get();
        Assert.assertEquals(401, response.getStatus());
        Assert.assertEquals(KerberosConstants.NEGOTIATE, response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE));
        String responseText = response.readEntity(String.class);
        response.close();
    }


    protected OAuthClient.AccessTokenResponse spnegoLoginTestImpl() throws Exception {
        Response spnegoResponse = spnegoLogin("hnelson", "secret");
        Assert.assertEquals(302, spnegoResponse.getStatus());

        List<UserRepresentation> users = testRealmResource().users().search("hnelson", 0, 1);
        String userId = users.get(0).getId();
        events.expectLogin()
                .client("kerberos-app")
                .user(userId)
                .detail(Details.USERNAME, "hnelson")
                .assertEvent();

        String codeUrl = spnegoResponse.getLocation().toString();

        return assertAuthenticationSuccess(codeUrl);
    }


    protected abstract boolean isCaseSensitiveLogin();

    // KEYCLOAK-2102
    @Test
    public void spnegoCaseInsensitiveTest() throws Exception {
        Response spnegoResponse = spnegoLogin(isCaseSensitiveLogin() ? "MyDuke" : "myduke", "theduke");
        Assert.assertEquals(302, spnegoResponse.getStatus());
        List<UserRepresentation> users = testRealmResource().users().search("myduke", 0, 1);
        String userId = users.get(0).getId();
        events.expectLogin()
                .client("kerberos-app")
                .user(userId)
                .detail(Details.USERNAME, "myduke")
                .assertEvent();

        String codeUrl = spnegoResponse.getLocation().toString();

        assertAuthenticationSuccess(codeUrl);
    }

    @Test
    public void usernamePasswordLoginTest() throws Exception {
        // Change editMode to READ_ONLY
        updateProviderEditMode(UserStorageProvider.EditMode.READ_ONLY);

        // Login with username/password from kerberos
        changePasswordPage.open();
        loginPage.assertCurrent();
        loginPage.login("jduke", "theduke");
        changePasswordPage.assertCurrent();

        // Bad existing password
        changePasswordPage.changePassword("theduke-invalid", "newPass", "newPass");
        Assert.assertTrue(driver.getPageSource().contains("Invalid existing password."));

        // Change password is not possible as editMode is READ_ONLY
        changePasswordPage.changePassword("theduke", "newPass", "newPass");
        Assert.assertTrue(
                driver.getPageSource().contains("You can't update your password as your account is read only"));

        // Change editMode to UNSYNCED
        updateProviderEditMode(UserStorageProvider.EditMode.UNSYNCED);

        // Successfully change password now
        changePasswordPage.changePassword("theduke", "newPass", "newPass");
        Assert.assertTrue(driver.getPageSource().contains("Your password has been updated."));
        changePasswordPage.logout();

        // Login with old password doesn't work, but with new password works
        loginPage.login("jduke", "theduke");
        loginPage.assertCurrent();
        loginPage.login("jduke", "newPass");
        changePasswordPage.assertCurrent();
        changePasswordPage.logout();

        // Assert SPNEGO login still with the old password as mode is unsynced
        events.clear();
        Response spnegoResponse = spnegoLogin("jduke", "theduke");
        Assert.assertEquals(302, spnegoResponse.getStatus());
        List<UserRepresentation> users = testRealmResource().users().search("jduke", 0, 1);
        String userId = users.get(0).getId();
        events.expectLogin()
                .client("kerberos-app")
                .user(userId)
                .detail(Details.USERNAME, "jduke")
                .assertEvent();

        String codeUrl = spnegoResponse.getLocation().toString();

        assertAuthenticationSuccess(codeUrl);
    }


    @Test
    public void credentialDelegationTest() throws Exception {
    	Assume.assumeTrue("Ignoring test as the embedded server is not started", isStartEmbeddedLdapServer());
    	// Add kerberos delegation credential mapper
        ProtocolMapperModel protocolMapper = UserSessionNoteMapper.createClaimMapper(KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME,
                KerberosConstants.GSS_DELEGATION_CREDENTIAL,
                KerberosConstants.GSS_DELEGATION_CREDENTIAL, "String",
                true, KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME,
                true, false);
        ProtocolMapperRepresentation protocolMapperRep = ModelToRepresentation.toRepresentation(protocolMapper);
        ClientResource clientResource = findClientByClientId(testRealmResource(), "kerberos-app");
        Response response = clientResource.getProtocolMappers().createMapper(protocolMapperRep);
        String protocolMapperId = ApiUtil.getCreatedId(response);
        response.close();

        // SPNEGO login
        OAuthClient.AccessTokenResponse tokenResponse = spnegoLoginTestImpl();

        // Assert kerberos ticket in the accessToken can be re-used to authenticate against other 3rd party kerberos service (ApacheDS Server in this case)
        String accessToken = tokenResponse.getAccessToken();
        AccessToken token = oauth.verifyToken(accessToken);

        String serializedGssCredential = (String) token.getOtherClaims().get(KerberosConstants.GSS_DELEGATION_CREDENTIAL);
        Assert.assertNotNull(serializedGssCredential);
        GSSCredential gssCredential = KerberosSerializationUtils.deserializeCredential(serializedGssCredential);
        String ldapResponse = invokeLdap(gssCredential, token.getPreferredUsername());
        Assert.assertEquals("Horatio Nelson", ldapResponse);

        // Logout
        oauth.openLogout();

        // Remove protocolMapper
        clientResource.getProtocolMappers().delete(protocolMapperId);

        // Login and assert delegated credential not anymore
        tokenResponse = spnegoLoginTestImpl();
        accessToken = tokenResponse.getAccessToken();
        token = oauth.verifyToken(accessToken);
        Assert.assertFalse(token.getOtherClaims().containsKey(KerberosConstants.GSS_DELEGATION_CREDENTIAL));

        events.clear();
    }

    private String invokeLdap(GSSCredential gssCredential, String username) throws NamingException {
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
            if (uri.contains("login-actions/required-action")) {
                response = client.target(uri).request().get();
            }
        }
        return response;

    }


    protected void initHttpClient(boolean useSpnego) {
        if (client != null) {
            cleanupApacheHttpClient();
        }

        DefaultHttpClient httpClient = (DefaultHttpClient) new HttpClientBuilder().build();
        httpClient.getAuthSchemes().register(AuthPolicy.SPNEGO, spnegoSchemeFactory);

        if (useSpnego) {
            Credentials fake = new Credentials() {

                public String getPassword() {
                    return null;
                }

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
        List<ComponentRepresentation> reps = testRealmResource().components().query("test", UserStorageProvider.class.getName());
        Assert.assertEquals(1, reps.size());
        ComponentRepresentation kerberosProvider = reps.get(0);
        kerberosProvider.getConfig().putSingle(LDAPConstants.EDIT_MODE, editMode.toString());
        testRealmResource().components().component(kerberosProvider.getId()).update(kerberosProvider);
    }

    public RealmResource testRealmResource() {
        return adminClient.realm("test");
    }


    // TODO: Use LDAPTestUtils.toComponentConfig once it's migrated to new testsuite
    public static MultivaluedHashMap<String, String> toComponentConfig(Map<String, String> ldapConfig) {
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        for (Map.Entry<String, String> entry : ldapConfig.entrySet()) {
            config.add(entry.getKey(), entry.getValue());

        }
        return config;
    }

}
