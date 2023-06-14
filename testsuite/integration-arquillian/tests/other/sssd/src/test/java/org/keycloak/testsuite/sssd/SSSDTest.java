package org.keycloak.testsuite.sssd;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.OAuthClient;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

/**
 * <p>The class needs a SSSD working environment with a set of users created.
 * The users to test are provided by the <em>sssd.properties</em> properties
 * file. Currently the users are the following:</p>
 *
 * <pre>
 * kinit admin
 * ipa group-add --desc='test group' testgroup
 * ipa user-add emily --first=Emily --last=Jones --email=emily@jones.com  --password (emily123)
 * ipa group-add-member testgroup --users=emily
 * ipa user-add bart --first=bart --last=bart --email= --password (bart123)
 * ipa user-add david --first=david --last=david --password (david123)
 * ipa user-disable david
 * </pre>
 *
 * @author rmartinc
 */
public class SSSDTest extends AbstractTestRealmKeycloakTest {

    private static final Logger log = Logger.getLogger(SSSDTest.class);

    private static final String DISPLAY_NAME = "Test user federation";
    private static final String PROVIDER_NAME = "sssd";
    private static final String REALM_NAME = "test";

    private static final String sssdConfigPath = "sssd/sssd.properties";

    private static final String DISABLED_USER = "disabled";
    private static final String NO_EMAIL_USER = "noemail";
    private static final String ADMIN_USER = "admin";
    private static PropertiesConfiguration sssdConfig;

    @Page
    protected LoginPage loginPage;

    @Page
    protected AppPage appPage;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    private String SSSDFederationID;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @BeforeClass
    public static void loadSSSDConfiguration() throws ConfigurationException {
        log.info("Reading SSSD configuration from classpath from: " + sssdConfigPath);
        InputStream is = SSSDTest.class.getClassLoader().getResourceAsStream(sssdConfigPath);
        sssdConfig = new PropertiesConfiguration();
        sssdConfig.load(is);
        sssdConfig.setListDelimiter(',');
    }

    @Before
    public void createUserFederation() {
        ComponentRepresentation userFederation = new ComponentRepresentation();

        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        userFederation.setConfig(config);

        userFederation.setName(DISPLAY_NAME);
        userFederation.getConfig().putSingle("priority", "0");
        userFederation.setProviderType(UserStorageProvider.class.getName());
        userFederation.setProviderId(PROVIDER_NAME);

        try (Response response = adminClient.realm(REALM_NAME).components().add(userFederation)) {
            SSSDFederationID = ApiUtil.getCreatedId(response);
        }
    }

    private void testLoginFailure(String username, String password) {
        loginPage.open();
        loginPage.login(username, password);
        loginPage.assertCurrent();
        Assert.assertEquals("Invalid username or password.", loginPage.getInputError());
        events.expect(EventType.LOGIN_ERROR).user(Matchers.any(String.class)).error(Errors.INVALID_USER_CREDENTIALS).assertEvent();
    }

    private void testLoginSuccess(String username) {
        loginPage.open();
        loginPage.login(username, getPassword(username));
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        EventRepresentation loginEvent = events.expectLogin().user(Matchers.any(String.class))
                .detail(Details.USERNAME, username).assertEvent();
        OAuthClient.AccessTokenResponse tokenResponse = sendTokenRequestAndGetResponse(loginEvent);
        appPage.logout(tokenResponse.getIdToken());
        events.expectLogout(loginEvent.getSessionId()).user(loginEvent.getUserId()).assertEvent();
    }

    @Test
    public void testInvalidPassword() {
        String username = getUsername();
        log.debug("Testing invalid password for user " + username);

        testLoginFailure(username, "invalid-password");
    }

    @Test
    public void testDisabledUser() {
        String username = getUser(DISABLED_USER);
        Assume.assumeTrue("Ignoring test no disabled user configured", username != null);
        log.debug("Testing disabled user " + username);

        testLoginFailure(username, getPassword(username));
    }

    @Test
    public void testAdmin() {
        String username = getUser(ADMIN_USER);
        Assume.assumeTrue("Ignoring test no admin user configured", username != null);
        log.debug("Testing password for user " + username);
        testLoginSuccess(username);
    }

    @Test
    public void testExistingUserLogIn() {
        log.debug("Testing correct password");

        for (String username : getUsernames()) {
            testLoginSuccess(username);
            verifyUserGroups(username, getGroups(username));
        }
    }

    @Test
    public void testExistingUserWithNoEmailLogIn() {
        log.debug("Testing correct password, but no e-mail provided");
        testLoginSuccess(getUser(NO_EMAIL_USER));
    }

    @Test
    public void testDeleteSSSDFederationProvider() {
        log.debug("Testing correct password");

        String username = getUsername();
        testLoginSuccess(username);
        verifyUserGroups(username, getGroups(username));

        int componentsListSize = adminClient.realm(REALM_NAME).components().query().size();
        adminClient.realm(REALM_NAME).components().component(SSSDFederationID).remove();
        assertThat(adminClient.realm(REALM_NAME).components().query().size(), is(componentsListSize - 1));
    }


    @Test
    public void changeReadOnlyProfile() {

        String username = getUsername();

        testLoginSuccess(username);

        RealmResource realm = adminClient.realm(REALM_NAME);
        List<UserRepresentation> users = realm.users().search(username, true);
        Assert.assertEquals(1, users.size());
        UserRepresentation user = users.iterator().next();
        user.setLastName("changed");

        BadRequestException e = Assert.assertThrows(BadRequestException.class,
                () -> realm.users().get(users.iterator().next().getId()).update(user));
        ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
        Assert.assertEquals("User is read only!", error.getErrorMessage());
    }

    @Test
    public void changeReadOnlyPassword() {
        String username = getUsername();

        testLoginSuccess(username);

        RealmResource realm = adminClient.realm(REALM_NAME);
        List<UserRepresentation> users = realm.users().search(username, true);
        Assert.assertEquals(1, users.size());
        CredentialRepresentation newPassword = new CredentialRepresentation();
        newPassword.setType(CredentialRepresentation.PASSWORD);
        newPassword.setValue("new-password-123!");
        newPassword.setTemporary(false);

        BadRequestException e = Assert.assertThrows(BadRequestException.class,
                () -> realm.users().get(users.iterator().next().getId()).resetPassword(newPassword));
        OAuth2ErrorRepresentation error = e.getResponse().readEntity(OAuth2ErrorRepresentation.class);
        Assert.assertEquals("Can't reset password as account is read only", error.getError());
    }

    private void verifyUserGroups(String username, List<String> groups) {
        List<UserRepresentation> users = adminClient.realm(REALM_NAME).users().search(username, 0, 1);
        assertThat("There must be at least one user", users.size(), greaterThan(0));
        assertThat("Exactly our test user", users.get(0).getUsername(), is(username));
        List<GroupRepresentation> assignedGroups = adminClient.realm(REALM_NAME).users().get(users.get(0).getId()).groups();
        List<String> assignedGroupNames = assignedGroups.stream().map(GroupRepresentation::getName).collect(Collectors.toList());
        MatcherAssert.assertThat(assignedGroupNames, Matchers.hasItems(groups.toArray(new String[0])));
    }

    private String getUsername() {
        return sssdConfig.getStringArray("usernames")[0];
    }

    private String getUser(String type) {
        return sssdConfig.getString("user." + type);
    }

    private List<String> getUsernames() {
        return Arrays.asList(sssdConfig.getStringArray("usernames"));
    }

    private String getPassword(String username) {
        return sssdConfig.getString("user." + username + ".password");
    }

    private List<String> getGroups(String username) {
        return Arrays.asList(sssdConfig.getStringArray("user." + username + ".groups"));
    }
}
