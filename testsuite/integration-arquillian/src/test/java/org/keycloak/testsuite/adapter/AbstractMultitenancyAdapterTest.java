package org.keycloak.testsuite.adapter;

import java.net.URL;
import java.util.List;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.keycloak.representations.idm.RealmRepresentation;
import static org.keycloak.testsuite.TestRealms.loadRealm;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrlStartsWith;
import org.keycloak.testsuite.page.adapter.MultiTenant;
import org.keycloak.testsuite.servlet.adapter.MultiTenantResolver;
import org.keycloak.testsuite.servlet.adapter.MultiTenantServlet;

public abstract class AbstractMultitenancyAdapterTest extends AbstractAdapterTest {

    public static final String ADAPTER_TENANT1 = "tenant1";
    public static final String ADAPTER_TENANT2 = "tenant2";
    
    @Page
    private MultiTenant multiTenant;

    @Deployment(name = MultiTenant.DEPLOYMENT_NAME)
    protected static WebArchive multiTenant() {
        String name = MultiTenant.DEPLOYMENT_NAME;
        String webInfPath = "/adapter-test/" + name + "/WEB-INF/";

        URL keycloakJSON1 = AbstractMultitenancyAdapterTest.class.getResource(webInfPath + "tenant1-keycloak.json");
        URL keycloakJSON2 = AbstractMultitenancyAdapterTest.class.getResource(webInfPath + "tenant2-keycloak.json");
        URL webXML = AbstractMultitenancyAdapterTest.class.getResource(webInfPath + "web.xml");

        WebArchive deployment = ShrinkWrap.create(WebArchive.class, name + ".war")
                .addClasses(MultiTenantServlet.class, MultiTenantResolver.class)
                .addAsWebInfResource(webXML, "web.xml")
                .addAsWebInfResource(keycloakJSON1, "tenant1-keycloak.json")
                .addAsWebInfResource(keycloakJSON2, "tenant2-keycloak.json");

        return deployment;
    }

    @Override
    public void loadAdapterTestRealmsInto(List<RealmRepresentation> testRealms) {
        testRealms.add(loadRealm("/adapter-test/tenant1-realm.json"));
        testRealms.add(loadRealm("/adapter-test/tenant2-realm.json"));
    }

//    Multi-tenancy tests.
    /**
     * Simplest scenario: one user, one realm. The user is not logged in at any
     * other realm
     *
     * @throws Exception
     */
    @Test
    public void testTenantsLoggingOut() throws Exception {
        doTenantRequests(ADAPTER_TENANT1, true);
        doTenantRequests(ADAPTER_TENANT2, true);
    }

    /**
     * This tests the adapter's ability to deal with multiple sessions from the
     * same user, one for each realm. It should not mixup and return a session
     * from tenant1 to tenant2
     *
     * @throws Exception
     */
    @Test
    public void testTenantsWithoutLoggingOut() throws Exception {
        doTenantRequests(ADAPTER_TENANT1, true);
        doTenantRequests(ADAPTER_TENANT2, true);

        doTenantRequests(ADAPTER_TENANT1, false);
        doTenantRequests(ADAPTER_TENANT2, true);
    }

    /**
     * This test simulates an user that is not logged in yet, and tris to login
     * into tenant1 using an account from tenant2. On this scenario, the user
     * should be shown the login page again.
     *
     * @throws Exception
     */
    @Test
    public void testUnauthorizedAccessNotLoggedIn() throws Exception {
        multiTenant.navigateToRealm(ADAPTER_TENANT1);
        assertCurrentUrlStartsWith(authServer);

        loginPage.login("user-tenant2", "user-tenant2");
        assertCurrentUrlStartsWith(authServer);
    }

    /**
     * This test simulates an user which is already logged in into tenant1 and
     * tries to access a resource on tenant2. On this scenario, the user should
     * be shown the login page again.
     *
     * @throws Exception
     */
    @Test
    public void testUnauthorizedAccessLoggedIn() throws Exception {
        doTenantRequests(ADAPTER_TENANT1, false);

        multiTenant.navigateToRealm(ADAPTER_TENANT2);
        assertCurrentUrlStartsWith(authServer);
    }

    private void doTenantRequests(String tenant, boolean logout) {
        String tenantLoginUrl = OIDCLoginProtocolService.authUrl(authServer.getUriBuilder()).build(tenant).toString();
        System.out.println("tenantLoginUrl " + tenantLoginUrl);

        multiTenant.navigateToRealm(tenant);
        System.out.println("Current url: " + driver.getCurrentUrl());
        
        assertTrue(driver.getCurrentUrl().startsWith(tenantLoginUrl));
        loginPage.login("bburke@redhat.com", "password");
        System.out.println("Current url: " + driver.getCurrentUrl());

        assertEquals(multiTenant.getTenantRealmUrl(tenant), driver.getCurrentUrl());

        String pageSource = driver.getPageSource();
        System.out.println(pageSource);

        assertTrue(pageSource.contains("Username: bburke@redhat.com"));
        assertTrue(pageSource.contains("Realm: " + tenant));

        if (logout) {
            driver.manage().deleteAllCookies();
        }
    }

}
