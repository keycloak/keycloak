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

package org.keycloak.testsuite.broker;

import java.util.List;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.Retry;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AccountPasswordPage;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.IdpConfirmLinkPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.UpdateAccountInformationPage;
import org.openqa.selenium.TimeoutException;

import static org.keycloak.testsuite.broker.BrokerTestTools.encodeUrl;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

/**
 * No test methods there. Just some useful common functionality
 */
public abstract class AbstractBaseBrokerTest extends AbstractKeycloakTest {

    protected static final String ATTRIBUTE_VALUE = "attribute.value";

    @Page
    protected AccountUpdateProfilePage accountUpdateProfilePage;

    // TODO: Rename this to loginPage
    @Page
    protected LoginPage accountLoginPage;

    @Page
    protected UpdateAccountInformationPage updateAccountInformationPage;

    @Page
    protected AccountPasswordPage accountPasswordPage;

    @Page
    protected ErrorPage errorPage;

    @Page
    protected IdpConfirmLinkPage idpConfirmLinkPage;

    protected BrokerConfiguration bc = getBrokerConfiguration();

    protected String userId;

    /**
     * Returns a broker configuration. Return value should not change between calls.
     * @return
     */
    protected abstract BrokerConfiguration getBrokerConfiguration();


    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation providerRealm = bc.createProviderRealm();
        RealmRepresentation consumerRealm = bc.createConsumerRealm();

        testRealms.add(providerRealm);
        testRealms.add(consumerRealm);
    }


    @After
    public void cleanupUsers() {
        RealmResource providerRealm = adminClient.realm(bc.providerRealmName());
        UserRepresentation userRep = ApiUtil.findUserByUsername(providerRealm, bc.getUserLogin());
        if (userRep != null) {
            providerRealm.users().get(userRep.getId()).remove();
        }

        RealmResource childRealm = adminClient.realm(bc.consumerRealmName());
        userRep = ApiUtil.findUserByUsername(childRealm, bc.getUserLogin());
        if (userRep != null) {
            childRealm.users().get(userRep.getId()).remove();
        }
    }


    protected void logInAsUserInIDP() {
        driver.navigate().to(getAccountUrl(bc.consumerRealmName()));

        log.debug("Clicking social " + bc.getIDPAlias());
        accountLoginPage.clickSocial(bc.getIDPAlias());

        waitForPage(driver, "log in to");

        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));

        log.debug("Logging in");
        accountLoginPage.login(bc.getUserLogin(), bc.getUserPassword());
    }


    /** Logs in the IDP and updates account information */
    protected void logInAsUserInIDPForFirstTime() {
        logInAsUserInIDP();

        waitForPage(driver, "update account information");

        Assert.assertTrue(updateAccountInformationPage.isCurrent());
        Assert.assertTrue("We must be on correct realm right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));

        log.debug("Updating info on updateAccount page");
        updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail(), "Firstname", "Lastname");
    }


    protected String getAccountUrl(String realmName) {
        return BrokerTestTools.getAuthRoot(suiteContext) + "/auth/realms/" + realmName + "/account";
    }


    protected String getAccountPasswordUrl(String realmName) {
        return BrokerTestTools.getAuthRoot(suiteContext) + "/auth/realms/" + realmName + "/account/password";
    }


    protected void logoutFromRealm(String realm) {
        driver.navigate().to(BrokerTestTools.getAuthRoot(suiteContext)
                + "/auth/realms/" + realm
                + "/protocol/" + "openid-connect"
                + "/logout?redirect_uri=" + encodeUrl(getAccountUrl(realm)));

        try {
            Retry.execute(() -> {
                try {
                    waitForPage(driver, "log in to " + realm);
                } catch (TimeoutException ex) {
                    driver.navigate().refresh();
                    log.debug("[Retriable] Timed out waiting for login page");
                    throw ex;
                }
            }, 10, 100);
        } catch (TimeoutException e) {
            log.debug(driver.getTitle());
            log.debug(driver.getPageSource());
            Assert.fail("Timeout while waiting for login page");
        }
    }


    protected void assertLoggedInAccountManagement() {
        Assert.assertTrue(accountUpdateProfilePage.isCurrent());
        Assert.assertEquals(accountUpdateProfilePage.getUsername(), bc.getUserLogin());
        Assert.assertEquals(accountUpdateProfilePage.getEmail(), bc.getUserEmail());
    }


    protected void assertErrorPage(String expectedError) {
        errorPage.assertCurrent();
        Assert.assertEquals(expectedError, errorPage.getError());
    }


}
