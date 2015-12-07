package org.keycloak.testsuite.console.federation;

import static org.junit.Assert.assertEquals;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserFederationProviderRepresentation;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.federation.CreateKerberosUserProvider;

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
		RealmRepresentation realm = testRealmResource().toRepresentation();
		UserFederationProviderRepresentation ufpr = realm.getUserFederationProviders().get(0);
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

	private void assertKerberosSetings(UserFederationProviderRepresentation ufpr, String kerberosRealm, String serverPrincipal, String keyTab, String debug, String useKerberosForPasswordAuthentication, String updateProfileFirstLogin) {
		assertEquals(kerberosRealm, ufpr.getConfig().get("kerberosRealm"));
		assertEquals(serverPrincipal, ufpr.getConfig().get("serverPrincipal"));
		assertEquals(keyTab, ufpr.getConfig().get("keyTab"));
		assertEquals(debug, ufpr.getConfig().get("debug"));
		assertEquals(useKerberosForPasswordAuthentication, ufpr.getConfig().get("allowKerberosAuthentication"));
		assertEquals(updateProfileFirstLogin, ufpr.getConfig().get("updateProfileFirstLogin"));
	}
}