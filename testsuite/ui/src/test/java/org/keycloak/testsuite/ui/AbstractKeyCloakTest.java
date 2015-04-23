/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.ui;

import java.util.concurrent.TimeUnit;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Before;
import org.keycloak.testsuite.ui.page.AbstractPage;

/**
 *
 * @author pmensik
 * @param <P>
 */
public abstract class AbstractKeyCloakTest<P extends AbstractPage> extends AbstractTest {
	
    @Page
    protected P page;
	
	@Before
	public void before() {
        driver.manage().timeouts().setScriptTimeout(10, TimeUnit.SECONDS);
		driver.manage().window().maximize();
		loginAsAdmin();
		menuPage.switchRealm("master");
	}
	
	@After
	public void after() {
		logOut();
	}
}
