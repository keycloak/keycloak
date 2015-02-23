package org.keycloak.testsuite.federation;

import java.security.Principal;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.HttpClientBuilder;
import org.keycloak.events.Details;
import org.keycloak.federation.kerberos.CommonKerberosConfig;
import org.keycloak.federation.kerberos.KerberosConfig;
import org.keycloak.models.KerberosConstants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.AccountPasswordPage;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractKerberosTest {

    protected KeycloakSPNegoSchemeFactory spnegoSchemeFactory;
    protected ResteasyClient client;

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected LoginPage loginPage;

    @WebResource
    protected AccountPasswordPage changePasswordPage;

    @WebResource
    protected AppPage appPage;

    protected abstract CommonKerberosConfig getKerberosConfig();
    protected abstract KeycloakRule getKeycloakRule();
    protected abstract AssertEvents getAssertEvents();

    @Before
    public void before() {
        CommonKerberosConfig kerberosConfig = getKerberosConfig();
        spnegoSchemeFactory = new KeycloakSPNegoSchemeFactory(kerberosConfig);
        initHttpClient(true);
        removeAllUsers();
    }

    @After
    public void after() {
        client.close();
        client = null;
    }


    @Test
    public void spnegoNotAvailableTest() throws Exception {
        initHttpClient(false);
        Response response = client.target(oauth.getLoginFormUrl()).request().get();
        Assert.assertEquals(401, response.getStatus());
        Assert.assertEquals(KerberosConstants.NEGOTIATE, response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE));
        String responseText = response.readEntity(String.class);
        responseText.contains("Log in to test");
        response.close();
    }


    protected void spnegoLoginTestImpl() throws Exception {
        KeycloakRule keycloakRule = getKeycloakRule();
        AssertEvents events = getAssertEvents();

        Response spnegoResponse = spnegoLogin("hnelson", "secret");
        Assert.assertEquals(302, spnegoResponse.getStatus());

        events.expectLogin()
                .user(keycloakRule.getUser("test", "hnelson").getId())
                .detail(Details.AUTH_METHOD, "spnego")
                .detail(Details.USERNAME, "hnelson")
                .assertEvent();

        String location = spnegoResponse.getLocation().toString();
        driver.navigate().to(location);
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        spnegoResponse.close();
    }


    @Test
    public void usernamePasswordLoginTest() throws Exception {
        KeycloakRule keycloakRule = getKeycloakRule();
        AssertEvents events = getAssertEvents();

        // Change editMode to READ_ONLY
        updateProviderEditMode(UserFederationProvider.EditMode.READ_ONLY);

        // Login with username/password from kerberos
        changePasswordPage.open();
        loginPage.assertCurrent();
        loginPage.login("jduke", "theduke");
        changePasswordPage.assertCurrent();

        // Change password is not possible as editMode is READ_ONLY
        changePasswordPage.changePassword("theduke", "newPass", "newPass");
        Assert.assertTrue(driver.getPageSource().contains("You can't update your password as your account is read only"));

        // Change editMode to UNSYNCED
        updateProviderEditMode(UserFederationProvider.EditMode.UNSYNCED);

        // Successfully change password now
        changePasswordPage.changePassword("theduke", "newPass", "newPass");
        Assert.assertTrue(driver.getPageSource().contains("Your password has been updated"));
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
        events.expectLogin()
                .user(keycloakRule.getUser("test", "jduke").getId())
                .detail(Details.AUTH_METHOD, "spnego")
                .detail(Details.USERNAME, "jduke")
                .assertEvent();
        spnegoResponse.close();
    }



    protected Response spnegoLogin(String username, String password) {
        spnegoSchemeFactory.setCredentials(username, password);
        return client.target(oauth.getLoginFormUrl()).request().get();
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
        KeycloakRule keycloakRule = getKeycloakRule();

        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmManager manager = new RealmManager(session);

            RealmModel appRealm = manager.getRealm("test");
            List<UserModel> users = session.userStorage().getUsers(appRealm);
            for (UserModel user : users) {
                if (!user.getUsername().equals(AssertEvents.DEFAULT_USERNAME)) {
                    session.userStorage().removeUser(appRealm, user);
                }
            }

            Assert.assertEquals(1, session.userStorage().getUsers(appRealm).size());
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }


    protected void assertUser(String expectedUsername, String expectedEmail, String expectedFirstname, String expectedLastname, boolean updateProfileActionExpected) {
        KeycloakRule keycloakRule = getKeycloakRule();

        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmManager manager = new RealmManager(session);
            RealmModel appRealm = manager.getRealm("test");

            UserModel user = session.users().getUserByUsername(expectedUsername, appRealm);
            Assert.assertNotNull(user);
            Assert.assertEquals(user.getEmail(), expectedEmail);
            Assert.assertEquals(user.getFirstName(), expectedFirstname);
            Assert.assertEquals(user.getLastName(), expectedLastname);

            if (updateProfileActionExpected) {
                Assert.assertEquals(UserModel.RequiredAction.UPDATE_PROFILE.toString(), user.getRequiredActions().iterator().next().name());
            } else {
                Assert.assertTrue(user.getRequiredActions().isEmpty());
            }
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }


    protected void updateProviderEditMode(UserFederationProvider.EditMode editMode) {
        KeycloakRule keycloakRule = getKeycloakRule();

        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmModel realm = session.realms().getRealm("test");
            UserFederationProviderModel kerberosProviderModel = realm.getUserFederationProviders().get(0);
            kerberosProviderModel.getConfig().put(LDAPConstants.EDIT_MODE, editMode.toString());
            realm.updateUserFederationProvider(kerberosProviderModel);
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }
}
