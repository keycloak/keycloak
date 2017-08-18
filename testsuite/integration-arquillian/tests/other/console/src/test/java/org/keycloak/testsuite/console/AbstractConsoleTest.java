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
import org.junit.Before;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.auth.page.login.Login;
import org.keycloak.testsuite.console.page.AdminConsole;
import org.keycloak.testsuite.console.page.AdminConsoleRealm;
import org.keycloak.testsuite.console.page.AdminConsoleRealm.ConfigureMenu;
import org.keycloak.testsuite.console.page.AdminConsoleRealm.ManageMenu;
import org.keycloak.testsuite.console.page.fragment.AdminConsoleAlert;
import org.keycloak.testsuite.console.page.fragment.ModalDialog;
import org.openqa.selenium.support.FindBy;

import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWithLoginUrlOf;

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

    // TODO: Fix the tests so this workaround is not necessary
    @Override
    protected boolean isImportAfterEachMethod() {
        return true;
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
        assertTrue("Alert is not success", alert.isSuccess());
        alert.close();
    }

    public void assertAlertDanger() {
        assertTrue("Alert is not danger", alert.isDanger());
        alert.close();
    }

    public ConfigureMenu configure() {
        return adminConsoleRealmPage.configure();
    }

    public ManageMenu manage() {
        return adminConsoleRealmPage.manage();
    }

}
