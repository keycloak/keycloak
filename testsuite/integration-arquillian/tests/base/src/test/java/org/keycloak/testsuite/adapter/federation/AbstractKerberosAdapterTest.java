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

package org.keycloak.testsuite.adapter.federation;

import static org.keycloak.testsuite.auth.page.AuthRealm.DEMO;
import static org.keycloak.testsuite.util.IOUtil.loadRealm;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.adapters.HttpClientBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.events.Details;
import org.keycloak.federation.kerberos.CommonKerberosConfig;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserFederationProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import org.keycloak.testsuite.adapter.page.KerberosPortal;
import org.keycloak.testsuite.adapter.servlet.KerberosCredDelegServlet;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.auth.page.account.ChangePassword;
import org.keycloak.testsuite.util.LDAPTestConfiguration;
import org.keycloak.util.ldap.KerberosEmbeddedServer;
import org.keycloak.util.ldap.LDAPEmbeddedServer;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractKerberosAdapterTest extends AbstractServletsAdapterTest {

    protected static LDAPTestConfiguration ldapTestConfiguration;  
    
    protected KeycloakSPNegoSchemeFactory spnegoSchemeFactory;
    
    protected ResteasyClient client;
        
    protected static LDAPEmbeddedServer ldapEmbeddedServer;

    @Rule
    public AssertEvents events = new AssertEvents(this);
    
    @Page
    protected ChangePassword changePasswordPage;
    
    @Page
    protected KerberosPortal kerberosPortal;
    
    protected abstract String getConnectionPropertiesLocation();

    protected abstract CommonKerberosConfig getKerberosConfig(UserFederationProviderModel model);
    
    @Deployment(name = KerberosPortal.DEPLOYMENT_NAME)
    protected static WebArchive kerberosPortal() {
        return servletDeployment(KerberosPortal.DEPLOYMENT_NAME, "keycloak.json", KerberosCredDelegServlet.class);
    }

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(loadRealm("/adapter-test/kerberosrealm.json"));
    }
    
    
    @Before
    public void before() throws Exception {   
        testRealmPage.setAuthRealm(AuthRealm.TEST);
        changePasswordPage.setAuthRealm(testRealmPage);
        // Global kerberos configuration
        ldapTestConfiguration = LDAPTestConfiguration.readConfiguration(getConnectionPropertiesLocation());
        String krb5ConfPath = LDAPTestConfiguration.getResource("test-krb5.conf");
        log.info("Krb5.conf file location is: " + krb5ConfPath);
        System.setProperty("java.security.krb5.conf", krb5ConfPath);
        if (ldapTestConfiguration.isStartEmbeddedLdapServer() && ldapEmbeddedServer == null) {
            ldapEmbeddedServer = createServer();
            ldapEmbeddedServer.init();
            ldapEmbeddedServer.start();
        }
        UserFederationProviderModel model = new UserFederationProviderModel();
        model.setConfig(ldapTestConfiguration.getLDAPConfig());
        spnegoSchemeFactory = new KeycloakSPNegoSchemeFactory(getKerberosConfig(model));
        initHttpClient(true);
        removeAllUsers();
    }

    @After
    public void after() {
        client.close();
        client = null;        
    }
    
    @AfterClass
    public static void afterClass() {
        try {
            if (ldapEmbeddedServer != null) {
                ldapEmbeddedServer.stop();
                ldapEmbeddedServer = null;
            }
            ldapTestConfiguration = null;
        } catch (Exception e) {
            throw new RuntimeException("Error tearDown Embedded LDAP server.", e);
        }
    }

    @Test
    public void spnegoNotAvailableTest() throws Exception {
        initHttpClient(false);

        String kcLoginPageLocation = client.target(kerberosPortal.getInjectedUrl().toString()).request().get().getLocation().toString();

        Response response = client.target(kcLoginPageLocation).request().get();
        Assert.assertEquals(401, response.getStatus());
        Assert.assertEquals(KerberosConstants.NEGOTIATE, response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE));
        String responseText = response.readEntity(String.class);
        responseText.contains("Log in to test");
        response.close();
    }

    protected void spnegoLoginTestImpl() throws Exception {
        Response spnegoResponse = spnegoLogin("hnelson", "secret");
        Assert.assertEquals(302, spnegoResponse.getStatus());
        
        List<UserRepresentation> users = testRealmResource().users().search("hnelson", 0, 1);
        String userId = users.get(0).getId();
        events.expectLogin()
                .client("kerberos-app")
                .user(userId)
                .detail(Details.REDIRECT_URI, kerberosPortal.toString())
                //.detail(Details.AUTH_METHOD, "spnego")
                .detail(Details.USERNAME, "hnelson")
                .assertEvent();

        String location = spnegoResponse.getLocation().toString();
        driver.navigate().to(location);

        String pageSource = driver.getPageSource();
        Assert.assertTrue(
                pageSource.contains("Kerberos Test") && pageSource.contains("Kerberos servlet secured content"));

        spnegoResponse.close();
        events.clear();
    }

    // KEYCLOAK-2102
    @Test
    public void spnegoCaseInsensitiveTest() throws Exception {
        Response spnegoResponse = spnegoLogin(ldapTestConfiguration.isCaseSensitiveLogin() ? "MyDuke" : "myduke", "theduke");
        Assert.assertEquals(302, spnegoResponse.getStatus());
        List<UserRepresentation> users = testRealmResource().users().search("myduke", 0, 1);
        String userId = users.get(0).getId();
        events.expectLogin()
                .client("kerberos-app")
                .user(userId)
                .detail(Details.REDIRECT_URI, kerberosPortal.toString())
                //.detail(Details.AUTH_METHOD, "spnego")
                .detail(Details.USERNAME, "myduke")
                .assertEvent();

        String location = spnegoResponse.getLocation().toString();
        driver.navigate().to(location);

        String pageSource = driver.getPageSource();
        Assert.assertTrue(
                pageSource.contains("Kerberos Test") && pageSource.contains("Kerberos servlet secured content"));

        spnegoResponse.close();
        events.clear();
    }

    @Test
    public void usernamePasswordLoginTest() throws Exception {
        // Change editMode to READ_ONLY
        updateProviderEditMode(UserFederationProvider.EditMode.READ_ONLY);

        // Login with username/password from kerberos
        changePasswordPage.navigateTo();
        testRealmLoginPage.isCurrent();
        testRealmLoginPage.form().login("jduke", "theduke");
        changePasswordPage.isCurrent();

        // Bad existing password
        changePasswordPage.changePasswords("theduke-invalid", "newPass", "newPass");
        Assert.assertTrue(driver.getPageSource().contains("Invalid existing password."));

        // Change password is not possible as editMode is READ_ONLY
        changePasswordPage.changePasswords("theduke", "newPass", "newPass");
        Assert.assertTrue(
                driver.getPageSource().contains("You can't update your password as your account is read only"));

        // Change editMode to UNSYNCED
        updateProviderEditMode(UserFederationProvider.EditMode.UNSYNCED);

        // Successfully change password now
        changePasswordPage.changePasswords("theduke", "newPass", "newPass");
        Assert.assertTrue(driver.getPageSource().contains("Your password has been updated."));
        changePasswordPage.logOut();        

        // Login with old password doesn't work, but with new password works
        testRealmLoginPage.form().login("jduke", "theduke");
        testRealmLoginPage.isCurrent();
        testRealmLoginPage.form().login("jduke", "newPass");
        changePasswordPage.isCurrent();
        changePasswordPage.logOut();
        
        // Assert SPNEGO login still with the old password as mode is unsynced
        events.clear();
        Response spnegoResponse = spnegoLogin("jduke", "theduke");
        Assert.assertEquals(302, spnegoResponse.getStatus());
        UserRepresentation user = ApiUtil.findUserByUsername(testRealmResource(), "jduke");
        events.expectLogin()
                .client("kerberos-app")
                .user(user != null ? user.getId() : null)
                .detail(Details.REDIRECT_URI, kerberosPortal.toString())
                //.detail(Details.AUTH_METHOD, "spnego")
                .detail(Details.USERNAME, "jduke")
                .assertEvent();
        spnegoResponse.close();
    }

    
    protected Response spnegoLogin(String username, String password) {
        kerberosPortal.navigateTo();
        Response res = client.target(kerberosPortal.getInjectedUrl().toString()).request().get();
        String kcLoginPageLocation = res.getLocation().toString();
        if (driver.manage().getCookieNamed("OAuth_Token_Request_State") != null) {
            kcLoginPageLocation = res.getLocation().toString().replaceFirst("state=.*&", "state=" + driver.manage().getCookieNamed("OAuth_Token_Request_State").getValue() + "&");
        }
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
            after();
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
    
    protected void updateProviderEditMode(UserFederationProvider.EditMode editMode) {
        RealmResource realm = testRealmResource();
        RealmRepresentation realmRepresentation = realm.toRepresentation();
        UserFederationProviderRepresentation kerberosProviderRepresentation = realmRepresentation
                .getUserFederationProviders().get(0);
        kerberosProviderRepresentation.getConfig().put(LDAPConstants.EDIT_MODE, editMode.toString());
        realm.update(realmRepresentation);
    }

    public RealmResource testRealmResource() {
        return adminClient.realm("test");
    }
    
    public Map<String, String> getConfig() {
        return ldapTestConfiguration.getLDAPConfig();
    }
    
    protected static LDAPEmbeddedServer createServer() {
        Properties defaultProperties = new Properties();
        defaultProperties.setProperty(LDAPEmbeddedServer.PROPERTY_DSF, LDAPEmbeddedServer.DSF_INMEMORY);
        defaultProperties.setProperty(LDAPEmbeddedServer.PROPERTY_LDIF_FILE, "classpath:kerberos/users-kerberos.ldif");
        return new KerberosEmbeddedServer(defaultProperties);
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmPage.setAuthRealm(AuthRealm.TEST);
    }
}
