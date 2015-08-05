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

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.graphene.page.Page;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.console.page.AdminConsole;
import org.keycloak.testsuite.console.page.AdminConsoleRealm;
import org.keycloak.testsuite.console.page.AdminConsoleRealm.ConfigureMenu;
import org.keycloak.testsuite.console.page.AdminConsoleRealm.ManageMenu;
import org.keycloak.testsuite.console.page.fragment.FlashMessage;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import static org.keycloak.testsuite.util.LoginAssert.assertCurrentUrlStartsWithLoginUrlOf;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrlStartsWith;

/**
 *
 * @author Petr Mensik
 * @author tkyjovsk
 */
public abstract class AbstractConsoleTest extends AbstractAuthTest {

    @Page
    protected AdminConsole testAdminConsole;
    @Page
    protected AdminConsoleRealm testAdminConsoleRealm;

    @FindByJQuery(".alert")
    protected FlashMessage flashMessage;

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testAdminConsole.setAdminRealm(TEST);
        testAdminConsoleRealm.setConsoleRealm(TEST).setAdminRealm(TEST);
    }

    @Before
    public void beforeConsoleTest() {
        loginToMasterRealmConsoleAsAdmin();
    }

    public void loginAsTestAdmin() {
        testAdminConsole.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testAdminConsole);
        testLogin.login(testAdmin.getUsername(), PASSWORD);
        assertCurrentUrlStartsWith(testAdminConsole);
    }

    public void logoutFromTestRealm() {
        testAdminConsole.navigateTo();
        assertCurrentUrlStartsWith(testAdminConsole);
        menu.logOut();
        assertCurrentUrlStartsWithLoginUrlOf(testAdminConsole);
    }

    public ConfigureMenu configure() {
        return testAdminConsoleRealm.configure();
    }

    public ManageMenu manage() {
        return testAdminConsoleRealm.manage();
    }

    public void assertFlashMessageSuccess() {
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
    }

    public void assertFlashMessageDanger() {
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isDanger());
    }

    public void assertFlashMessageError() {
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isError());
    }

}
