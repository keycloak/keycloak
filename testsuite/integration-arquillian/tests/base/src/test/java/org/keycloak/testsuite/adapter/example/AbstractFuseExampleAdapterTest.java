/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.adapter.example;

import org.keycloak.testsuite.adapter.AbstractExampleAdapterTest;
import java.io.File;
import java.util.List;
import org.jboss.arquillian.graphene.page.Page;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.auth.page.account.Account;
import static org.keycloak.testsuite.util.IOUtil.loadRealm;
import static org.keycloak.testsuite.adapter.AbstractExampleAdapterTest.EXAMPLES_HOME_DIR;
import org.keycloak.testsuite.adapter.page.fuse.AdminInterface;
import org.keycloak.testsuite.adapter.page.fuse.CustomerListing;
import org.keycloak.testsuite.adapter.page.fuse.CustomerPortalFuseExample;
import org.keycloak.testsuite.adapter.page.fuse.ProductPortalFuseExample;
import static org.keycloak.testsuite.auth.page.AuthRealm.DEMO;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWithLoginUrlOf;
import static org.keycloak.testsuite.util.WaitUtils.pause;

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
    protected Account testRealmAccount;

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation fuseRealm = loadRealm(new File(EXAMPLES_HOME_DIR + "/fuse/testrealm.json"));
        testRealms.add(fuseRealm);
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmPage.setAuthRealm(DEMO);
        testRealmLoginPage.setAuthRealm(DEMO);
        testRealmAccount.setAuthRealm(DEMO);
    }

    // no Arquillian deployments - examples already installed by maven

    @Test
    public void testCustomerListingAndAccountManagement() {
        customerPortal.navigateTo();
        assertCurrentUrlStartsWith(customerPortal);

        customerPortal.clickCustomerListingLink();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);

        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlStartsWith(customerListing);

        String src = driver.getPageSource();
        assertTrue(src.contains("Username: bburke@redhat.com")
                && src.contains("Bill Burke")
                && src.contains("Stian Thorgersen")
        );

        // account mgmt
        customerListing.clickAccountManagement();

        assertCurrentUrlStartsWith(testRealmAccount);
        assertEquals(testRealmAccount.getUsername(), "bburke@redhat.com");

        driver.navigate().back();
        customerListing.clickLogOut();

        // assert user not logged in
        customerPortal.clickCustomerListingLink();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);

    }

    @Test
    public void testAdminInterface() {
        customerPortal.navigateTo();
        assertCurrentUrlStartsWith(customerPortal);

        customerPortal.clickAdminInterfaceLink();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);

        testRealmLoginPage.form().login("admin", "password");
        assertCurrentUrlStartsWith(adminInterface);
        assertTrue(driver.getPageSource().contains("Hello admin!"));

        customerListing.navigateTo();
        customerListing.clickLogOut();
        pause(500);
        assertCurrentUrlStartsWith(customerPortal);

        customerPortal.navigateTo();//needed for phantomjs
        customerPortal.clickAdminInterfaceLink();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);

        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlStartsWith(adminInterface);
        assertTrue(driver.getPageSource().contains("Status code is 403"));
    }

    @Test
    public void testProductPortal() {
        productPortal.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);

        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlStartsWith(productPortal);

        assertTrue(productPortal.getProduct1UnsecuredText().contains("401: Unauthorized"));
        assertTrue(productPortal.getProduct1SecuredText().contains("Product received: id=1"));
        assertTrue(productPortal.getProduct2SecuredText().contains("Product received: id=2"));

        productPortal.clickLogOutLink();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
    }

}
