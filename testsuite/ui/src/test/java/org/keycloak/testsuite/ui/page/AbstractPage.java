/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.keycloak.testsuite.ui.page;

import java.util.List;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.keycloak.testsuite.ui.util.Constants;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author pmensik
 */
public class AbstractPage {

    @Drone
    protected WebDriver driver;
	
	@FindBy(css = ".btn-danger")
	protected WebElement dangerButton;
	
	//@FindByJQuery(".btn-primary:visible")
    @FindBy(css = ".btn-primary")
    protected WebElement primaryButton;
	
	    @FindBy(css = ".btn-primary")
    protected List<WebElement> primaryButtons;

	
	@FindBy(css = ".ng-binding.btn.btn-danger")
	protected WebElement deleteConfirmationButton;
	
    public void goToPage(String page) {
        driver.get(String.format(page, Constants.CURRENT_REALM));
    }

}
