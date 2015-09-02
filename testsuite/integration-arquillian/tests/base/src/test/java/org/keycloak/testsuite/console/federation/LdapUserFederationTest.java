package org.keycloak.testsuite.console.federation;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.*;
import org.keycloak.models.LDAPConstants;

import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.federation.LdapUserProviderForm;
import org.keycloak.testsuite.console.page.federation.UserFederation;
import org.keycloak.testsuite.console.page.users.Users;
import org.keycloak.testsuite.util.LDAPTestConfiguration;

import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import static org.keycloak.testsuite.admin.Users.setPasswordFor;

/**
 * Created by fkiss.
 */
public class LdapUserFederationTest extends AbstractConsoleTest {

    @Page
    private LdapUserProviderForm ldapUserProviderForm;

    @Page
    private UserFederation userFederation;

    @Page
    private Users users;

    @Before
    public void beforeTestLdapUserFederation() {
        //configure().userFederation();
    }

    @Ignore
    @Test
    public void addAndConfigureProvider() {
        adminConsole.navigateTo();
        testRealmLogin.form().login(testRealmUser);

        String name = "ldapname";

        String LDAP_CONNECTION_PROPERTIES_LOCATION = "ldap/ldap-connection.properties";
        LDAPTestConfiguration ldapTestConfiguration = LDAPTestConfiguration.readConfiguration(LDAP_CONNECTION_PROPERTIES_LOCATION);

        UserRepresentation newUser = new UserRepresentation();
        String testUsername = "defaultrole tester";
        newUser.setUsername(testUsername);
        setPasswordFor(newUser, PASSWORD);

        Map<String,String> ldapConfig = ldapTestConfiguration.getLDAPConfig();

        //addLdapProviderTest
        configure().userFederation();
        userFederation.addProvider("ldap");
        ldapUserProviderForm.configureLdap(ldapConfig.get(LDAPConstants.LDAP_PROVIDER), ldapConfig.get(LDAPConstants.EDIT_MODE), ldapConfig.get(LDAPConstants.VENDOR), ldapConfig.get(LDAPConstants.CONNECTION_URL), ldapConfig.get(LDAPConstants.USERS_DN), ldapConfig.get(LDAPConstants.BIND_DN), ldapConfig.get(LDAPConstants.BIND_CREDENTIAL));
    }

    @Ignore
    @Test
    public void caseSensitiveSearch() {
        // This should fail for now due to case-sensitivity
        adminConsole.navigateTo();
        testRealmLogin.form().login("johnKeycloak", "Password1");
        assertTrue(flashMessage.getText(), flashMessage.isDanger());
    }
}