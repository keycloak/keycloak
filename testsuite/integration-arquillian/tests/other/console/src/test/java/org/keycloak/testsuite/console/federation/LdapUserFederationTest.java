package org.keycloak.testsuite.console.federation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Properties;

import org.apache.commons.configuration.ConfigurationException;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserFederationProviderRepresentation;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.federation.CreateLdapUserProvider;
import org.keycloak.util.ldap.LDAPEmbeddedServer;
import org.openqa.selenium.WebElement;

/**
 * @author fkiss, pdrozd
 */
public class LdapUserFederationTest extends AbstractConsoleTest {

    private static final String UNSYNCED = "UNSYNCED";

    private static final String READ_ONLY = "READ_ONLY";

    private static final String RED_HAT_DIRECTORY_SERVER = "Red Hat Directory Server";

    private static final String WRITABLE = "WRITABLE";

    private static final String ACTIVE_DIRECTORY = "Active Directory";

    @Page
    private CreateLdapUserProvider createLdapUserProvider;

    @Test
    public void configureAdProvider() {
        createLdapUserProvider.navigateTo();
        createLdapUserProvider.form().selectVendor(ACTIVE_DIRECTORY);
        createLdapUserProvider.form().setConsoleDisplayNameInput("ldap");
        createLdapUserProvider.form().selectEditMode(WRITABLE);
        createLdapUserProvider.form().setLdapConnectionUrlInput("ldap://localhost:389");
        createLdapUserProvider.form().setLdapBindDnInput("KEYCLOAK/Administrator");
        createLdapUserProvider.form().setLdapUserDnInput("ou=People,dc=keycloak,dc=org");
        createLdapUserProvider.form().setLdapBindCredentialInput("secret");
//        createLdapUserProvider.form().setAccountAfterPasswordUpdateEnabled(false);
        // enable kerberos
        createLdapUserProvider.form().setAllowKerberosAuthEnabled(true);
        createLdapUserProvider.form().setKerberosRealmInput("KEYCLOAK.ORG");
        createLdapUserProvider.form().setServerPrincipalInput("HTTP/localhost@KEYCLOAK.ORG");
        createLdapUserProvider.form().setKeyTabInput("http.keytab");
        createLdapUserProvider.form().setDebugEnabled(true);
        createLdapUserProvider.form().save();
        assertAlertSuccess();

        RealmRepresentation realm = testRealmResource().toRepresentation();
        UserFederationProviderRepresentation ufpr = realm.getUserFederationProviders().get(0);
        assertLdapProviderSetting(ufpr, "ldap", 0, WRITABLE, "false", "ad", "1", "true", "true", "false");
        assertLdapBasicMapping(ufpr, "cn", "cn", "objectGUID", "person, organizationalPerson, user",
                "ou=People,dc=keycloak,dc=org");
        assertLdapSyncSetings(ufpr, "1000", 0, 0);
        assertLdapKerberosSetings(ufpr, "KEYCLOAK.ORG", "HTTP/localhost@KEYCLOAK.ORG", "http.keytab", "true", "false");
    }

    @Test
    public void configureRhdsProvider() {
        createLdapUserProvider.navigateTo();
        createLdapUserProvider.form().selectVendor(RED_HAT_DIRECTORY_SERVER);
        createLdapUserProvider.form().setConsoleDisplayNameInput("ldap");
        createLdapUserProvider.form().selectEditMode(READ_ONLY);
        createLdapUserProvider.form().setLdapConnectionUrlInput("ldap://localhost:389");
        createLdapUserProvider.form().setLdapBindDnInput("uid=admin,ou=system");
        createLdapUserProvider.form().setLdapUserDnInput("ou=People,dc=keycloak,dc=org");
        createLdapUserProvider.form().setLdapBindCredentialInput("secret");
        createLdapUserProvider.form().save();
        assertAlertSuccess();

        RealmRepresentation realm = testRealmResource().toRepresentation();
        UserFederationProviderRepresentation ufpr = realm.getUserFederationProviders().get(0);
        assertLdapProviderSetting(ufpr, "ldap", 0, READ_ONLY, "false", "rhds", "1", "true", "true", "true");
        assertLdapBasicMapping(ufpr, "uid", "uid", "nsuniqueid", "inetOrgPerson, organizationalPerson",
                "ou=People,dc=keycloak,dc=org");
        assertLdapSyncSetings(ufpr, "1000", 0, 0);
    }

    @Test
    public void invalidSettingsTest() {
        createLdapUserProvider.navigateTo();
        createLdapUserProvider.form().selectVendor(ACTIVE_DIRECTORY);
        createLdapUserProvider.form().setConsoleDisplayNameInput("ldap");
        createLdapUserProvider.form().selectEditMode(UNSYNCED);
        createLdapUserProvider.form().setLdapBindDnInput("uid=admin,ou=system");
        createLdapUserProvider.form().setLdapUserDnInput("ou=People,dc=keycloak,dc=org");
        createLdapUserProvider.form().setLdapBindCredentialInput("secret");
        createLdapUserProvider.form().save();
        assertAlertDanger();
        createLdapUserProvider.form().setLdapUserDnInput("");
        createLdapUserProvider.form().setLdapConnectionUrlInput("ldap://localhost:389");
        createLdapUserProvider.form().save();
        assertAlertDanger();
        createLdapUserProvider.form().setLdapUserDnInput("ou=People,dc=keycloak,dc=org");
        createLdapUserProvider.form().setLdapBindDnInput("");
        createLdapUserProvider.form().save();
        assertAlertDanger();
        createLdapUserProvider.form().setLdapBindDnInput("uid=admin,ou=system");
        createLdapUserProvider.form().setLdapBindCredentialInput("");
        createLdapUserProvider.form().save();
        assertAlertDanger();
        createLdapUserProvider.form().setLdapBindCredentialInput("secret");

        createLdapUserProvider.form().setCustomUserSearchFilter("foo");
        createLdapUserProvider.form().save();
        assertAlertDanger();
        createLdapUserProvider.form().setCustomUserSearchFilter("");
        createLdapUserProvider.form().save();
        assertAlertSuccess();

        // Try updating invalid Custom LDAP Filter
        createLdapUserProvider.form().setCustomUserSearchFilter("(foo=bar");
        createLdapUserProvider.form().save();
        assertAlertDanger();
        createLdapUserProvider.form().setCustomUserSearchFilter("foo=bar)");
        createLdapUserProvider.form().save();
        assertAlertDanger();
        createLdapUserProvider.form().setCustomUserSearchFilter("(foo=bar)");
        createLdapUserProvider.form().save();
        assertAlertSuccess();

    }

    @Test
    public void testConnection() throws Exception {
        createLdapUserProvider.navigateTo();
        createLdapUserProvider.form().selectVendor(1);
        createLdapUserProvider.form().setConsoleDisplayNameInput("ldap");
        createLdapUserProvider.form().selectEditMode(WRITABLE);
        createLdapUserProvider.form().setLdapConnectionUrlInput("ldap://localhost:10389");
        createLdapUserProvider.form().setLdapBindDnInput("uid=admin,ou=system");
        createLdapUserProvider.form().setLdapUserDnInput("ou=People,dc=keycloak,dc=org");
        createLdapUserProvider.form().setLdapBindCredentialInput("secret");
//        createLdapUserProvider.form().setAccountAfterPasswordUpdateEnabled(true);
        createLdapUserProvider.form().save();
        assertAlertSuccess();
        LDAPEmbeddedServer ldapServer = null;
        try {
            ldapServer = startEmbeddedLdapServer();
            createLdapUserProvider.form().testConnection();
            assertAlertSuccess();
            createLdapUserProvider.form().testAuthentication();
            assertAlertSuccess();
            createLdapUserProvider.form().synchronizeAllUsers();
            assertAlertSuccess();
            createLdapUserProvider.form().setLdapBindCredentialInput("secret1");
            createLdapUserProvider.form().testAuthentication();
            assertAlertDanger();
        } finally {
            if (ldapServer != null) {
                ldapServer.stop();
            }
        }
    }

    @Test
    public void checkVendors() throws ConfigurationException {
        createLdapUserProvider.navigateTo();

        List<String> vendorsExpected = (List<String>) (List<?>) getConstantsProperties().getList("ldap-vendors");
        List<String> vendorsActual = createLdapUserProvider.form().getVendors();

        int vendorsExpectedSize = vendorsExpected.size();
        int vendorsActualSize = vendorsActual.size();
        assertTrue("Expected vendors count: " + vendorsExpectedSize + "; actual count: " + vendorsActualSize,
                vendorsExpectedSize == vendorsActualSize);

        assertTrue("Vendors list doesn't match", vendorsExpected.containsAll(vendorsActual));
    }

    private void assertLdapProviderSetting(UserFederationProviderRepresentation ufpr, String name, int priority,
            String editMode, String syncRegistrations, String vendor, String searchScope, String connectionPooling,
            String pagination, String enableAccountAfterPasswordUpdate) {
        assertEquals(name, ufpr.getDisplayName());
        assertEquals(priority, ufpr.getPriority());
        assertEquals(editMode, ufpr.getConfig().get("editMode"));
        assertEquals(syncRegistrations, ufpr.getConfig().get("syncRegistrations"));
        assertEquals(vendor, ufpr.getConfig().get("vendor"));
        assertEquals(searchScope, ufpr.getConfig().get("searchScope"));
        assertEquals(connectionPooling, ufpr.getConfig().get("connectionPooling"));
        assertEquals(pagination, ufpr.getConfig().get("pagination"));
//        assertEquals(enableAccountAfterPasswordUpdate, ufpr.getConfig().get("userAccountControlsAfterPasswordUpdate"));
    }

    private void assertLdapBasicMapping(UserFederationProviderRepresentation ufpr, String usernameLdapAttribute,
            String rdnLdapAttr, String uuidLdapAttr, String userObjectClasses, String userDN) {
        assertEquals(usernameLdapAttribute, ufpr.getConfig().get("usernameLDAPAttribute"));
        assertEquals(rdnLdapAttr, ufpr.getConfig().get("rdnLDAPAttribute"));
        assertEquals(uuidLdapAttr, ufpr.getConfig().get("uuidLDAPAttribute"));
        assertEquals(userObjectClasses, ufpr.getConfig().get("userObjectClasses"));
        assertEquals(userDN, ufpr.getConfig().get("usersDn"));
    }

    private void assertLdapKerberosSetings(UserFederationProviderRepresentation ufpr, String kerberosRealm,
            String serverPrincipal, String keyTab, String debug, String useKerberosForPasswordAuthentication) {
        assertEquals(kerberosRealm, ufpr.getConfig().get("kerberosRealm"));
        assertEquals(serverPrincipal, ufpr.getConfig().get("serverPrincipal"));
        assertEquals(keyTab, ufpr.getConfig().get("keyTab"));
        assertEquals(debug, ufpr.getConfig().get("debug"));
        assertEquals(useKerberosForPasswordAuthentication,
                ufpr.getConfig().get("useKerberosForPasswordAuthentication"));
    }

    private void assertLdapSyncSetings(UserFederationProviderRepresentation ufpr, String batchSize,
            int periodicFullSync, int periodicChangedUsersSync) {
        assertEquals(batchSize, ufpr.getConfig().get("batchSizeForSync"));
        assertEquals(periodicFullSync, ufpr.getFullSyncPeriod());
        assertEquals(periodicChangedUsersSync, ufpr.getChangedSyncPeriod());
    }

    private LDAPEmbeddedServer startEmbeddedLdapServer() throws Exception {
        Properties defaultProperties = new Properties();
        defaultProperties.setProperty(LDAPEmbeddedServer.PROPERTY_DSF, LDAPEmbeddedServer.DSF_INMEMORY);
        defaultProperties.setProperty(LDAPEmbeddedServer.PROPERTY_LDIF_FILE, "classpath:ldap/users.ldif");
        LDAPEmbeddedServer ldapEmbeddedServer = new LDAPEmbeddedServer(defaultProperties);
        ldapEmbeddedServer.init();
        ldapEmbeddedServer.start();
        return ldapEmbeddedServer;
    }
}
