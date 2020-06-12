package org.keycloak.testsuite.console.federation;

import org.apache.commons.configuration.ConfigurationException;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.models.LDAPConstants;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.federation.CreateLdapUserProvider;
import org.keycloak.util.ldap.LDAPEmbeddedServer;

import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
        createLdapUserProvider.form().uncollapseKerberosIntegrationHeader();
        createLdapUserProvider.form().setAllowKerberosAuthEnabled(true);
        createLdapUserProvider.form().setKerberosRealmInput("KEYCLOAK.ORG");
        createLdapUserProvider.form().setServerPrincipalInput("HTTP/localhost@KEYCLOAK.ORG");
        createLdapUserProvider.form().setKeyTabInput("http.keytab");
        createLdapUserProvider.form().setDebugEnabled(true);
        createLdapUserProvider.form().save();
        assertAlertSuccess();

        ComponentRepresentation ufpr = testRealmResource().components()
                .query(null, "org.keycloak.storage.UserStorageProvider").get(0);

        assertLdapProviderSetting(ufpr, "ldap", "0", WRITABLE, "false", "ad", "1", "true", "true", "false");
        assertLdapBasicMapping(ufpr, "cn", "cn", "objectGUID", "person, organizationalPerson, user",
                "ou=People,dc=keycloak,dc=org");
        assertLdapSyncSetings(ufpr, "1000", "-1", "-1");
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

        ComponentRepresentation ufpr = testRealmResource().components()
                .query(null, "org.keycloak.storage.UserStorageProvider").get(0);

        assertLdapProviderSetting(ufpr, "ldap", "0", READ_ONLY, "false", "rhds", "1", "true", "true", "true");
        assertLdapBasicMapping(ufpr, "uid", "uid", "nsuniqueid", "inetOrgPerson, organizationalPerson",
                "ou=People,dc=keycloak,dc=org");
        assertLdapSyncSetings(ufpr, "1000", "-1", "-1");
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

    @Test
    public void configureConnectionPooling() {
        createLdapUserProvider.navigateTo();
        createLdapUserProvider.form().selectVendor(ACTIVE_DIRECTORY);
        createLdapUserProvider.form().setConsoleDisplayNameInput("ldap");
        createLdapUserProvider.form().selectEditMode(WRITABLE);
        createLdapUserProvider.form().setLdapConnectionUrlInput("ldap://localhost:389");
        createLdapUserProvider.form().setLdapBindDnInput("KEYCLOAK/Administrator");
        createLdapUserProvider.form().setLdapUserDnInput("ou=People,dc=keycloak,dc=org");
        createLdapUserProvider.form().setLdapBindCredentialInput("secret");

        createLdapUserProvider.form().connectionPoolingSettings();
        createLdapUserProvider.form().setConnectionPoolingAuthentication("none");
        createLdapUserProvider.form().setConnectionPoolingDebug("fine");
        createLdapUserProvider.form().setConnectionPoolingInitSize("10");
        createLdapUserProvider.form().setConnectionPoolingMaxSize("12");
        createLdapUserProvider.form().setConnectionPoolingPrefSize("11");
        createLdapUserProvider.form().setConnectionPoolingProtocol("ssl");
        createLdapUserProvider.form().setConnectionPoolingTimeout("500");

        createLdapUserProvider.form().save();
        assertAlertSuccess();

        ComponentRepresentation ufpr = testRealmResource().components()
                .query(null, "org.keycloak.storage.UserStorageProvider").get(0);

        assertLdapConnectionPoolSettings(ufpr, "none","fine","10","12","11","ssl","500");
   }

    private void assertLdapProviderSetting(ComponentRepresentation ufpr, String name, String priority,
            String editMode, String syncRegistrations, String vendor, String searchScope, String connectionPooling,
            String pagination, String enableAccountAfterPasswordUpdate) {
        assertEquals(name, ufpr.getName());
        assertEquals(priority, ufpr.getConfig().get("priority").get(0));
        assertEquals(editMode, ufpr.getConfig().get("editMode").get(0));
        assertEquals(syncRegistrations, ufpr.getConfig().get("syncRegistrations").get(0));
        assertEquals(vendor, ufpr.getConfig().get("vendor").get(0));
        assertEquals(searchScope, ufpr.getConfig().get("searchScope").get(0));
        assertEquals(connectionPooling, ufpr.getConfig().get("connectionPooling").get(0));
        assertEquals(pagination, ufpr.getConfig().get("pagination").get(0));
//        assertEquals(enableAccountAfterPasswordUpdate, ufpr.getConfig().get("userAccountControlsAfterPasswordUpdate"));
    }

    private void assertLdapBasicMapping(ComponentRepresentation ufpr, String usernameLdapAttribute,
            String rdnLdapAttr, String uuidLdapAttr, String userObjectClasses, String userDN) {
        assertEquals(usernameLdapAttribute, ufpr.getConfig().get("usernameLDAPAttribute").get(0));
        assertEquals(rdnLdapAttr, ufpr.getConfig().get("rdnLDAPAttribute").get(0));
        assertEquals(uuidLdapAttr, ufpr.getConfig().get("uuidLDAPAttribute").get(0));
        assertEquals(userObjectClasses, ufpr.getConfig().get("userObjectClasses").get(0));
        assertEquals(userDN, ufpr.getConfig().get("usersDn").get(0));
    }

    private void assertLdapKerberosSetings(ComponentRepresentation ufpr, String kerberosRealm,
            String serverPrincipal, String keyTab, String debug, String useKerberosForPasswordAuthentication) {
        assertEquals(kerberosRealm, ufpr.getConfig().get("kerberosRealm").get(0));
        assertEquals(serverPrincipal, ufpr.getConfig().get("serverPrincipal").get(0));
        assertEquals(keyTab, ufpr.getConfig().get("keyTab").get(0));
        assertEquals(debug, ufpr.getConfig().get("debug").get(0));
        assertEquals(useKerberosForPasswordAuthentication,
                ufpr.getConfig().get("useKerberosForPasswordAuthentication").get(0));
    }

    private void assertLdapSyncSetings(ComponentRepresentation ufpr, String batchSize,
            String periodicFullSync, String periodicChangedUsersSync) {
        assertEquals(batchSize, ufpr.getConfig().get("batchSizeForSync").get(0));
        assertEquals(periodicFullSync, ufpr.getConfig().get("fullSyncPeriod").get(0));
        assertEquals(periodicChangedUsersSync, ufpr.getConfig().get("changedSyncPeriod").get(0));
    }

    private void assertLdapConnectionPoolSettings(ComponentRepresentation ufpr, String authentication, String debug,
            String initsize, String maxsize, String prefsize, String protocol, String timeout) {
        assertEquals(authentication, ufpr.getConfig().get(LDAPConstants.CONNECTION_POOLING_AUTHENTICATION).get(0));
        assertEquals(debug, ufpr.getConfig().get(LDAPConstants.CONNECTION_POOLING_DEBUG).get(0));
        assertEquals(initsize, ufpr.getConfig().get(LDAPConstants.CONNECTION_POOLING_INITSIZE).get(0));
        assertEquals(maxsize, ufpr.getConfig().get(LDAPConstants.CONNECTION_POOLING_MAXSIZE).get(0));
        assertEquals(prefsize, ufpr.getConfig().get(LDAPConstants.CONNECTION_POOLING_PREFSIZE).get(0));
        assertEquals(protocol, ufpr.getConfig().get(LDAPConstants.CONNECTION_POOLING_PROTOCOL).get(0));
        assertEquals(timeout, ufpr.getConfig().get(LDAPConstants.CONNECTION_POOLING_TIMEOUT).get(0));
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
