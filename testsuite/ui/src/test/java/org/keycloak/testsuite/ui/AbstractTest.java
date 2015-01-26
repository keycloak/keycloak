/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.keycloak.testsuite.ui;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.runner.RunWith;
import org.keycloak.testsuite.ui.fragment.Navigation;
import org.keycloak.testsuite.ui.fragment.UserMenu;
import org.keycloak.testsuite.ui.page.LoginPage;

import static org.keycloak.testsuite.ui.util.URL.BASE_URL;
import org.openqa.selenium.WebDriver;
/**
 *
 * @author pmensik
 */
@RunWith(Arquillian.class)
public class AbstractTest {

	@Page
	protected LoginPage loginPage;
	
	@Page
	protected UserMenu menuPage;
	
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
    }
    
}
