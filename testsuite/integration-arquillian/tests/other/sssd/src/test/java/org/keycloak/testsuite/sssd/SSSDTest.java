package org.keycloak.testsuite.sssd;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.logging.Logger;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.common.constants.GenericConstants;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AccountPasswordPage;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.LDAPTestConfiguration;

public class SSSDTest extends AbstractKeycloakTest {

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
    protected LoginPage accountLoginPage;

    @Page
    protected AccountPasswordPage changePasswordPage;

    @Page
    protected AccountUpdateProfilePage profilePage;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    private String SSSDFederationID;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = new RealmRepresentation();

        realm.setRealm(REALM_NAME);
        realm.setEnabled(true);

        testRealms.add(realm);
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

        Response response = adminClient.realm(REALM_NAME).components().add(userFederation);
        SSSDFederationID = ApiUtil.getCreatedId(response);
        response.close();
    }

    @Test
    public void testInvalidPassword() {
        String username = getUsername();
        log.debug("Testing invalid password for user " + username);

        profilePage.open();
        Assert.assertEquals("Browser should be on login page now", "Log in to " + REALM_NAME, driver.getTitle());
        accountLoginPage.login(username, "invalid-password");
        Assert.assertEquals("Invalid username or password.", accountLoginPage.getError());
    }

    @Test
    public void testDisabledUser() {
        String username = getUser(DISABLED_USER);
        Assume.assumeTrue("Ignoring test no disabled user configured", username != null);
        log.debug("Testing disabled user " + username);

        profilePage.open();
        Assert.assertEquals("Browser should be on login page now", "Log in to " + REALM_NAME, driver.getTitle());
        accountLoginPage.login(username, getPassword(username));

        Assert.assertEquals("Invalid username or password.", accountLoginPage.getError());
    }

    @Test
    public void testAdmin() {
        String username = getUser(ADMIN_USER);
        Assume.assumeTrue("Ignoring test no admin user configured", username != null);
        log.debug("Testing password for user " + username);

        profilePage.open();
        Assert.assertEquals("Browser should be on login page now", "Log in to " + REALM_NAME, driver.getTitle());
        accountLoginPage.login(username, getPassword(username));
        Assert.assertTrue(profilePage.isCurrent());
    }

    @Test
    public void testExistingUserLogIn() {
        log.debug("Testing correct password");

        for (String username : getUsernames()) {
            profilePage.open();
            Assert.assertEquals("Browser should be on login page now", "Log in to " + REALM_NAME, driver.getTitle());
            accountLoginPage.login(username, getPassword(username));
            Assert.assertTrue(profilePage.isCurrent());
            verifyUserGroups(username, getGroups(username));
            profilePage.logout();
        }
    }

    @Test
    public void testExistingUserWithNoEmailLogIn() {
        log.debug("Testing correct password, but no e-mail provided");
        String username = getUser(NO_EMAIL_USER);
        profilePage.open();
        Assert.assertEquals("Browser should be on login page now", "Log in to " + REALM_NAME, driver.getTitle());
        accountLoginPage.login(username, getPassword(username));
        Assert.assertTrue(profilePage.isCurrent());
    }

    @Test
    public void testDeleteSSSDFederationProvider() {
        log.debug("Testing correct password");

        profilePage.open();
        String username = getUsername();
        Assert.assertEquals("Browser should be on login page now", "Log in to " + REALM_NAME, driver.getTitle());
        accountLoginPage.login(username, getPassword(username));
        Assert.assertTrue(profilePage.isCurrent());
        verifyUserGroups(username, getGroups(username));

        int componentsListSize = adminClient.realm(REALM_NAME).components().query().size();
        adminClient.realm(REALM_NAME).components().component(SSSDFederationID).remove();
        Assert.assertEquals(componentsListSize - 1, adminClient.realm(REALM_NAME).components().query().size());
    }


    @Test
    public void changeReadOnlyProfile() throws Exception {

        String username = getUsername();
        profilePage.open();
        accountLoginPage.login(username, getPassword(username));

        Assert.assertEquals(username, profilePage.getUsername());
        Assert.assertEquals(sssdConfig.getProperty("user." + username + ".firstname"), profilePage.getFirstName());
        Assert.assertEquals(sssdConfig.getProperty("user." + username + ".lastname"), profilePage.getLastName());
        Assert.assertEquals(sssdConfig.getProperty("user." + username + ".mail"), profilePage.getEmail());

        profilePage.updateProfile("New first", "New last", "new@email.com");

        Assert.assertEquals("You can't update your account as it is read-only.", profilePage.getError());
    }

    @Test
    public void changeReadOnlyPassword() {
        String username = getUsername();
        changePasswordPage.open();
        accountLoginPage.login(username, getPassword(username));

        changePasswordPage.changePassword(getPassword(username), "new-password", "new-password");
        Assert.assertEquals("You can't update your password as your account is read only.", profilePage.getError());
    }

    private void verifyUserGroups(String username, List<String> groups) {
        List<UserRepresentation> users = adminClient.realm(REALM_NAME).users().search(username, 0, 1);
        Assert.assertTrue("There must be at least one user", users.size() > 0);
        Assert.assertEquals("Exactly our test user", username, users.get(0).getUsername());
        List<GroupRepresentation> assignedGroups = adminClient.realm(REALM_NAME).users().get(users.get(0).getId()).groups();
        Assert.assertEquals("User must have exactly " + groups.size() + " groups", groups.size(), assignedGroups.size());

        for (GroupRepresentation group : assignedGroups) {
            Assert.assertTrue(groups.contains(group.getName()));
        }
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
