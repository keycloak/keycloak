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

package org.keycloak.testsuite.adapter.example.fuse;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.auth.page.AuthRealm.DEMO;
import static org.keycloak.testsuite.utils.io.IOUtil.loadRealm;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWithLoginUrlOf;

import java.io.File;
import java.util.List;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.adapter.AbstractExampleAdapterTest;
import org.keycloak.testsuite.adapter.page.fuse.AdminInterface;
import org.keycloak.testsuite.adapter.page.fuse.CustomerListing;
import org.keycloak.testsuite.adapter.page.fuse.CustomerPortalFuseExample;
import org.keycloak.testsuite.adapter.page.fuse.ProductPortalFuseExample;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.arquillian.containers.ContainerConstants;
import org.keycloak.testsuite.auth.page.account.Account;
import org.keycloak.testsuite.util.WaitUtils;
import org.hamcrest.Matchers;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer(ContainerConstants.APP_SERVER_FUSE63)
@AppServerContainer(ContainerConstants.APP_SERVER_FUSE7X)
public class FuseExampleAdapterTest extends AbstractExampleAdapterTest {

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
        RealmRepresentation fuseRealm = loadRealm(new File(TEST_APPS_HOME_DIR + "/fuse/demorealm.json"));
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
        assertThat(src, Matchers.allOf(
          containsString("Username: bburke@redhat.com"),
          containsString("Bill Burke"),
          containsString("Stian Thorgersen")
        ));

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
        WaitUtils.waitForPageToLoad();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);

        testRealmLoginPage.form().login("admin", "password");
        assertCurrentUrlStartsWith(adminInterface);
        assertThat(driver.getPageSource(), containsString("Hello admin!"));
        assertThat(driver.getPageSource(), containsString("This second sentence is returned from a Camel RestDSL endpoint"));

        customerListing.navigateTo();
        WaitUtils.waitForPageToLoad();
        customerListing.clickLogOut();
        WaitUtils.waitForPageToLoad();

        WaitUtils.pause(2500);
        customerPortal.navigateTo();//needed for phantomjs
        WaitUtils.waitForPageToLoad();
        customerPortal.clickAdminInterfaceLink();
        WaitUtils.waitForPageToLoad();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);

        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlStartsWith(adminInterface);
        assertThat(driver.getPageSource(), containsString("Status code is 403"));
    }

    @Test
    public void testProductPortal() {
        productPortal.navigateTo();
        WaitUtils.waitForPageToLoad();

        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);

        testRealmLoginPage.form().login("bburke@redhat.com", "password");
        assertCurrentUrlStartsWith(productPortal);

        assertThat(productPortal.getProduct1UnsecuredText(), containsString("401: Unauthorized"));
        assertThat(productPortal.getProduct1SecuredText(), containsString("Product received: id=1"));
        assertThat(productPortal.getProduct2SecuredText(), containsString("Product received: id=2"));

        productPortal.clickLogOutLink();
        WaitUtils.waitForPageToLoad();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
    }

}
