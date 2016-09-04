package org.keycloak.testsuite.console.federation;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.federation.CreateKerberosUserProvider;
import org.keycloak.testsuite.console.page.federation.UserFederation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class UserFederationTest extends AbstractConsoleTest {
    @Page
    private UserFederation userFederationPage;

    @Page
    private CreateKerberosUserProvider createKerberosUserProviderPage;

    @Before
    public void beforeUserFederationTest() {
        userFederationPage.navigateTo();
    }

    @Test
    public void availableProviders() {
        assertTrue(userFederationPage.hasProvider("ldap"));
        assertTrue(userFederationPage.hasProvider("kerberos"));
    }

    @Test
    public void addRemoveFederation() {
        final String federationName = "KerberosProvider";

        userFederationPage.addFederation("kerberos");
        assertTrue(createKerberosUserProviderPage.isCurrent());

        createKerberosUserProviderPage.form().setConsoleDisplayNameInput(federationName);
        createKerberosUserProviderPage.form().setKerberosRealmInput("KEYCLOAK.ORG");
        createKerberosUserProviderPage.form().setServerPrincipalInput("HTTP/localhost@KEYCLOAK.ORG");
        createKerberosUserProviderPage.form().setKeyTabInput("http.keytab");
        createKerberosUserProviderPage.form().save();
        assertAlertSuccess();
        userFederationPage.navigateTo();

        assertEquals(1, userFederationPage.table().getFederationsCount());

        userFederationPage.table().editFederation(federationName);
        assertEquals(federationName, createKerberosUserProviderPage.form().getConsoleDisplayNameInput());
        userFederationPage.navigateTo();

        userFederationPage.table().removeFederation(federationName);
        assertEquals(0, userFederationPage.table().getFederationsCount());
    }
}
