/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.ui.account2;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.models.utils.SessionTimeoutHelper;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.ui.account2.page.DeviceActivityPage;
import org.keycloak.testsuite.ui.account2.page.PersonalInfoPage;

import java.util.List;

import static org.keycloak.testsuite.util.UIUtils.refreshPageAndWaitForLoad;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWithLoginUrlOf;
import static org.keycloak.testsuite.util.WaitUtils.pause;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class SessionTest extends AbstractAccountTest {
    public static final int SSO_SESSION_IDLE_TIMEOUT = 1;
    public static final int ACCESS_TOKEN_LIFESPAN = 10;

    @Page
    private PersonalInfoPage personalInfoPage;

    @Page
    private DeviceActivityPage deviceActivityPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        super.addTestRealms(testRealms);
        RealmRepresentation realm = testRealms.get(0);

        // in seconds
        realm.setSsoSessionIdleTimeout(1);
        realm.setAccessTokenLifespan(10);
    }

    @Before
    public void beforeSessionTest() {
        personalInfoPage.navigateTo();
        loginToAccount();
    }

    @Test
    public void reactPageSsoTimeoutTest() {
        deviceActivityPage.navigateToUsingSidebar();
        deviceActivityPage.assertCurrent();
        personalInfoPage.navigateToUsingSidebar();
        personalInfoPage.assertCurrent();

        waitForSessionToExpire();
        deviceActivityPage.navigateToUsingSidebar();
        assertCurrentUrlStartsWithLoginUrlOf(accountWelcomeScreen);
    }

    @Test
    public void reactPageAsyncLogoutTest() {
        testRealmResource().logoutAll();
        deviceActivityPage.navigateToUsingSidebar();
        assertCurrentUrlStartsWithLoginUrlOf(accountWelcomeScreen);
    }

    @Test
    public void welcomeScreenSsoTimeoutTest() {
        accountWelcomeScreen.navigateTo();
        accountWelcomeScreen.header().assertLoginBtnVisible(false);
        accountWelcomeScreen.header().assertLogoutBtnVisible(true);

        waitForSessionToExpire();
        refreshPageAndWaitForLoad();
        accountWelcomeScreen.header().assertLoginBtnVisible(true);
        accountWelcomeScreen.header().assertLogoutBtnVisible(false);
    }

    @Test
    public void welcomeScreenAsyncLogoutTest() {
        accountWelcomeScreen.navigateTo();
        accountWelcomeScreen.header().assertLoginBtnVisible(false);
        accountWelcomeScreen.header().assertLogoutBtnVisible(true);

        testRealmResource().logoutAll();
        refreshPageAndWaitForLoad();
        accountWelcomeScreen.header().assertLoginBtnVisible(true);
        accountWelcomeScreen.header().assertLogoutBtnVisible(false);
    }

    private void waitForSessionToExpire() {
        // +3 to add some toleration
        log.info("Waiting for SSO session to expire");
        pause((SSO_SESSION_IDLE_TIMEOUT + SessionTimeoutHelper.IDLE_TIMEOUT_WINDOW_SECONDS + 3) * 1000);
    }
}
