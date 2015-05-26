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

package org.keycloak.testsuite.ui;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.runner.RunWith;
import org.keycloak.testsuite.ui.fragment.Navigation;
import org.keycloak.testsuite.ui.fragment.MenuPage;
import org.keycloak.testsuite.ui.page.LoginPage;
import org.keycloak.testsuite.ui.page.account.PasswordPage;
import static org.keycloak.testsuite.ui.util.Constants.ADMIN_PSSWD;

import static org.keycloak.testsuite.ui.util.URL.BASE_URL;
import org.openqa.selenium.WebDriver;

/**
 *
 * @author Petr Mensik
 */
@RunWith(Arquillian.class)
public abstract class AbstractTest {

    private static boolean firstAdminLogin = Boolean.parseBoolean(
            System.getProperty("firstAdminLogin", "true"));

    @Page
    protected LoginPage loginPage;

    @Page
    protected PasswordPage passwordPage;

    @Page
    protected MenuPage menuPage;

    @Page
    protected Navigation navigation;

    @Drone
    protected WebDriver driver;

    public void logOut() {
        menuPage.logOut();
    }

    public void loginAsAdmin() {
        driver.get(BASE_URL);
        loginPage.loginAsAdmin();
        if (firstAdminLogin) {
            passwordPage.confirmNewPassword(ADMIN_PSSWD);
            passwordPage.submit();
            firstAdminLogin = false;
        }
    }

}
