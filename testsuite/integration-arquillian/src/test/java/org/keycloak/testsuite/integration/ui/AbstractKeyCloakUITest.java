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

package org.keycloak.testsuite.integration.ui;

import org.keycloak.testsuite.integration.AbstractTest;
import java.util.concurrent.TimeUnit;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Before;
import org.keycloak.testsuite.integration.ui.fragment.MenuPage;
import org.keycloak.testsuite.integration.ui.fragment.Navigation;
import org.keycloak.testsuite.integration.ui.page.AbstractPage;
import org.keycloak.testsuite.integration.ui.page.LoginPage;
import org.keycloak.testsuite.integration.ui.page.account.PasswordPage;
import static org.keycloak.testsuite.integration.ui.util.Constants.ADMIN_PSSWD;
import static org.keycloak.testsuite.integration.ui.util.URL.BASE_URL;

/**
 *
 * @author Petr Mensik
 * @param <P>
 */
public abstract class AbstractKeyCloakUITest<P extends AbstractPage> extends AbstractTest {

	private static boolean firstAdminLogin = Boolean.parseBoolean(
            System.getProperty("firstAdminLogin", "true"));
	
    @Page
    protected P page;
	
	@Page
    protected LoginPage loginPage;

    @Page
    protected PasswordPage passwordPage;

    @Page
    protected MenuPage menuPage;

    @Page
    protected Navigation navigation;
	
	@Before
	public void before() {
        driver.manage().timeouts().setScriptTimeout(10, TimeUnit.SECONDS);
		driver.manage().window().maximize();
		loginAsAdmin();
	}
	
	@After
	public void after() {
		logOut();
	}
	
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
 