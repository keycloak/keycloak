/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.console;

import org.jboss.arquillian.graphene.page.Page;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.console.page.AdminConsole;
import org.keycloak.testsuite.console.page.AdminConsoleRealm;
import org.keycloak.testsuite.console.page.AdminConsoleRealm.ConfigureMenu;
import org.keycloak.testsuite.console.page.AdminConsoleRealm.ManageMenu;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import org.keycloak.testsuite.auth.page.login.Login;
import org.keycloak.testsuite.console.page.fragment.AdminConsoleAlert;
import org.keycloak.testsuite.console.page.fragment.ModalDialog;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWithLoginUrlOf;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author Petr Mensik
 * @author tkyjovsk
 */
public abstract class AbstractConsoleTest extends AbstractAuthTest {

    @Page
    protected AdminConsole adminConsolePage;
    @Page
    protected AdminConsoleRealm adminConsoleRealmPage;

    @Page
    protected AdminConsole testRealmAdminConsolePage;

    @FindBy(xpath = "//div[@class='modal-dialog']")
    protected ModalDialog modalDialog;

    @FindBy(className = "alert")
    protected AdminConsoleAlert alert;

    protected boolean adminLoggedIn = false;

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmPage.setAuthRealm(TEST);
        testRealmAdminConsolePage.setAdminRealm(TEST);
    }

    @Before
    public void beforeConsoleTest() {
        createTestUserWithAdminClient();
        if (!testContext.isAdminLoggedIn()) {
            loginToMasterRealmAdminConsoleAs(adminUser);
            testContext.setAdminLoggedIn(true);
        } else {
//            adminConsoleRealmPage.navigateTo();
        }
    }

    public void loginToMasterRealmAdminConsoleAs(UserRepresentation user) {
        loginToAdminConsoleAs(adminConsolePage, loginPage, user);
    }

    public void logoutFromMasterRealmConsole() {
        logoutFromAdminConsole(adminConsolePage);
    }

    public void loginToTestRealmConsoleAs(UserRepresentation user) {
        loginToAdminConsoleAs(testRealmAdminConsolePage, testRealmLoginPage, user);
    }

    public void logoutFromTestRealmConsole() {
        logoutFromAdminConsole(testRealmAdminConsolePage);
    }

    public void loginToAdminConsoleAs(AdminConsole adminConsole, Login login, UserRepresentation user) {
        adminConsole.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(adminConsole);
        login.form().login(user);
        assertCurrentUrlStartsWith(adminConsole);
    }

    public void logoutFromAdminConsole(AdminConsole adminConsole) {
        adminConsole.navigateTo();
        assertCurrentUrlStartsWith(adminConsole);
        adminConsole.logOut();
        assertCurrentUrlStartsWithLoginUrlOf(adminConsole);
    }

    public void assertAlertSuccess() {
        assertTrue(alert.isSuccess());
        alert.close();
    }

    public void assertAlertDanger() {
        assertTrue(alert.isDanger());
        alert.close();
    }

    public ConfigureMenu configure() {
        return adminConsoleRealmPage.configure();
    }

    public ManageMenu manage() {
        return adminConsoleRealmPage.manage();
    }

}
