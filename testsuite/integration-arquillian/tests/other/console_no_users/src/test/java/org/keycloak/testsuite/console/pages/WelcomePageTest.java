package org.keycloak.testsuite.console.pages;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.auth.page.WelcomePage;
import org.keycloak.testsuite.auth.page.login.OIDCLogin;

/**
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WelcomePageTest extends AbstractKeycloakTest {

    @Page
    private WelcomePage welcomePage;

    @Page
    protected OIDCLogin loginPage;
    
    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        // no operation
    }

    /*
     * Leave out client initialization and creation of a user account. We
     * don't need those.
     */
    @Before
    @Override
    public void beforeAbstractKeycloakTest() {
        setDefaultPageUriParameters();
        driverSettings();
    }

    @After
    @Override
    public void afterAbstractKeycloakTest() {
        // no need for this either
    }

    /**
     * Attempt to resolve the floating IP address. This is where EAP/WildFly
     * will be accessible. See "-Djboss.bind.address=0.0.0.0".
     * 
     * @return
     * @throws Exception 
     */
    private String getFloatingIpAddress() throws Exception {
        Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface ni : Collections.list(netInterfaces)) {
            Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
            for (InetAddress a : Collections.list(inetAddresses)) {
                if (!a.isLoopbackAddress() && a.isSiteLocalAddress()) {
                    return a.getHostAddress();
                }
            }
        }
        return null;
    }

    private URL getPublicServerUrl() throws Exception {
        String floatingIp = getFloatingIpAddress();
        if (floatingIp == null) {
            throw new RuntimeException("Could not determine floating IP address.");
        }
        return new URL("http", floatingIp, welcomePage.getInjectedUrl().getPort(), "");
    }
    
    @Test
    public void test_1_LocalAccessNoAdmin() throws Exception {
        welcomePage.navigateTo();
        Assert.assertFalse("Welcome page did not ask to create a new admin user.", welcomePage.isPasswordSet());
    }

    @Test
    public void test_2_RemoteAccessNoAdmin() throws Exception {
        driver.navigate().to(getPublicServerUrl());
        Assert.assertFalse("Welcome page did not ask to create a new admin user.", welcomePage.isPasswordSet());
    }

    @Test
    public void test_3_LocalAccessWithAdmin() throws Exception {
        welcomePage.navigateTo();
        welcomePage.setPassword("admin", "admin");
        Assert.assertTrue(driver.getPageSource().contains("User created"));

        welcomePage.navigateTo();
        Assert.assertTrue("Welcome page asked to set admin password.", welcomePage.isPasswordSet());
    }

    @Test
    public void test_4_RemoteAccessWithAdmin() throws Exception {
        driver.navigate().to(getPublicServerUrl());
        Assert.assertTrue("Welcome page asked to set admin password.", welcomePage.isPasswordSet());
    }

    @Test
    public void test_5_AccessCreatedAdminAccount() throws Exception {
        welcomePage.navigateToAdminConsole();
        loginPage.form().login("admin", "admin");
        Assert.assertFalse("Login with 'admin:admin' failed", 
                driver.getPageSource().contains("Invalid username or password."));
    }

}
