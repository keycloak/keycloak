package org.keycloak.testsuite.console.federation;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserFederationProviderRepresentation;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.federation.CreateKerberosUserProvider;

import static org.junit.Assert.assertEquals;

/**
 * @author pdrozd
 */
public class KerberosUserFederationTest extends AbstractConsoleTest {

	private static final String UNSYNCED = "UNSYNCED";

	private static final String READ_ONLY = "READ_ONLY";

	@Page
	private CreateKerberosUserProvider createKerberosUserProvider;

	@Test
	public void configureKerberosProvider() {
		createKerberosUserProvider.navigateTo();
		createKerberosUserProvider.form().setConsoleDisplayNameInput("kerberos");
		createKerberosUserProvider.form().setKerberosRealmInput("KEYCLOAK.ORG");
		createKerberosUserProvider.form().setServerPrincipalInput("HTTP/localhost@KEYCLOAK.ORG");
		createKerberosUserProvider.form().setKeyTabInput("http.keytab");
		createKerberosUserProvider.form().setDebugEnabled(true);
		createKerberosUserProvider.form().setAllowPasswordAuthentication(true);
		createKerberosUserProvider.form().selectEditMode(READ_ONLY);
		createKerberosUserProvider.form().setUpdateProfileFirstLogin(true);
		createKerberosUserProvider.form().save();
		assertAlertSuccess();

        ComponentRepresentation ufpr = testRealmResource().components()
                .query(null, "org.keycloak.storage.UserStorageProvider").get(0);
		assertKerberosSetings(ufpr, "KEYCLOAK.ORG", "HTTP/localhost@KEYCLOAK.ORG", "http.keytab", "true", "true", "true");
	}

	@Test
	public void invalidSettingsTest() {
		createKerberosUserProvider.navigateTo();
		createKerberosUserProvider.form().setConsoleDisplayNameInput("kerberos");
		createKerberosUserProvider.form().setServerPrincipalInput("HTTP/localhost@KEYCLOAK.ORG");
		createKerberosUserProvider.form().setKeyTabInput("http.keytab");
		createKerberosUserProvider.form().setDebugEnabled(true);
		createKerberosUserProvider.form().setAllowPasswordAuthentication(true);
		createKerberosUserProvider.form().selectEditMode(UNSYNCED);
		createKerberosUserProvider.form().setUpdateProfileFirstLogin(true);
		createKerberosUserProvider.form().save();
		assertAlertDanger();
		createKerberosUserProvider.form().setServerPrincipalInput("");
		createKerberosUserProvider.form().setKerberosRealmInput("KEYCLOAK.ORG");;
		createKerberosUserProvider.form().save();
		assertAlertDanger();
		createKerberosUserProvider.form().setServerPrincipalInput("HTTP/localhost@KEYCLOAK.ORG");;
		createKerberosUserProvider.form().setKeyTabInput("");
		createKerberosUserProvider.form().save();
		assertAlertDanger();		
		createKerberosUserProvider.form().setKeyTabInput("http.keytab");;
		createKerberosUserProvider.form().save();
		assertAlertSuccess();
	}

	private void assertKerberosSetings(ComponentRepresentation ufpr, String kerberosRealm, String serverPrincipal, String keyTab, String debug, String useKerberosForPasswordAuthentication, String updateProfileFirstLogin) {
		assertEquals(kerberosRealm, ufpr.getConfig().get("kerberosRealm").get(0));
		assertEquals(serverPrincipal, ufpr.getConfig().get("serverPrincipal").get(0));
		assertEquals(keyTab, ufpr.getConfig().get("keyTab").get(0));
		assertEquals(debug, ufpr.getConfig().get("debug").get(0));
		assertEquals(useKerberosForPasswordAuthentication, ufpr.getConfig().get("allowPasswordAuthentication").get(0));
		assertEquals(updateProfileFirstLogin, ufpr.getConfig().get("updateProfileFirstLogin").get(0));
	}
}