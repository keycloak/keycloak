package org.keycloak.testsuite.adapter;

import java.io.File;
import java.util.List;
import org.jboss.arquillian.graphene.page.Page;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.account.page.Account;
import static org.keycloak.testsuite.util.RealmUtils.loadRealm;
import static org.keycloak.testsuite.adapter.AbstractExampleAdapterTest.EXAMPLES_HOME_DIR;
import org.keycloak.testsuite.adapter.page.fuse.AdminInterface;
import org.keycloak.testsuite.adapter.page.fuse.CustomerListing;
import org.keycloak.testsuite.adapter.page.fuse.CustomerPortalFuseExample;
import org.keycloak.testsuite.adapter.page.fuse.ProductPortalFuseExample;
import static org.keycloak.testsuite.console.page.Realm.DEMO;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.RealmAssert.assertCurrentUrlStartsWithLoginUrlOf;
import static org.keycloak.testsuite.util.SeleniumUtils.pause;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractFuseExampleAdapterTest extends AbstractExampleAdapterTest {

    @Page
    protected CustomerPortalFuseExample customerPortal;
    @Page
    protected CustomerListing customerListing;
    @Page
    protected AdminInterface adminInterface;

    @Page
    protected ProductPortalFuseExample productPortal;

    @Page
    protected Account account;

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation fureRealm = loadRealm(new File(EXAMPLES_HOME_DIR + "/fuse/testrealm.json"));
        testRealms.add(fureRealm);
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealm.setConsoleRealm(DEMO);

        account.setAccountRealm(DEMO);
    }

    // no Arquillian deployments - examples already installed by maven

    @Test
    public void testCustomerListingAndAccountManagement() {
        customerPortal.navigateTo();
        assertCurrentUrlStartsWith(customerPortal);

        customerPortal.clickCustomerListingLink();
        assertCurrentUrlStartsWithLoginUrlOf(testRealm);

        login.login("bburke@redhat.com", "password");
        assertCurrentUrlStartsWith(customerListing);

        String src = driver.getPageSource();
        assertTrue(src.contains("Username: bburke@redhat.com")
                && src.contains("Bill Burke")
                && src.contains("Stian Thorgersen")
        );

        // account mgmt
        customerListing.clickAccountManagement();

        assertCurrentUrlStartsWith(account);
        assertEquals(account.getUsername(), "bburke@redhat.com");

        driver.navigate().back();
        customerListing.clickLogOut();

        // assert user not logged in
        customerPortal.clickCustomerListingLink();
        assertCurrentUrlStartsWithLoginUrlOf(testRealm);

    }

    @Test
    public void testAdminInterface() {
        customerPortal.navigateTo();
        assertCurrentUrlStartsWith(customerPortal);

        customerPortal.clickAdminInterfaceLink();
        assertCurrentUrlStartsWithLoginUrlOf(testRealm);

        login.login("admin", "password");
        assertCurrentUrlStartsWith(adminInterface);
        assertTrue(driver.getPageSource().contains("Hello admin!"));

        customerListing.navigateTo();
        customerListing.clickLogOut();
        pause(500);
        assertCurrentUrlStartsWith(customerPortal);

        customerPortal.clickAdminInterfaceLink();
        assertCurrentUrlStartsWithLoginUrlOf(testRealm);

        login.login("bburke@redhat.com", "password");
        assertCurrentUrlStartsWith(adminInterface);
        assertTrue(driver.getPageSource().contains("Status code is 403"));
    }

    @Test
    public void testProductPortal() {
        productPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealm);

        login.login("bburke@redhat.com", "password");
        assertCurrentUrlStartsWith(productPortal);

        assertTrue(productPortal.getProduct1UnsecuredText().contains("401: Unauthorized"));
        assertTrue(productPortal.getProduct1SecuredText().contains("Product received: id=1"));
        assertTrue(productPortal.getProduct2SecuredText().contains("Product received: id=2"));

        productPortal.clickLogOutLink();
        assertCurrentUrlStartsWithLoginUrlOf(testRealm);
    }

}
