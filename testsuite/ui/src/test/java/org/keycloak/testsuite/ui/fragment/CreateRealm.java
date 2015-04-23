/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.ui.fragment;

import org.jboss.arquillian.drone.api.annotation.Drone;
import static org.openqa.selenium.By.id;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author pmensik
 */
public class CreateRealm {
	
	@FindBy(css = ".btn-primary")
	private WebElement primaryButton;
	
	@Drone
	private WebDriver driver;
	
	public void importRealm(String filePath) {
		driver.findElement(id("import-file")).sendKeys(filePath);
		primaryButton.click();
	}
	
	public void createRealm(String name, boolean on) {
		driver.findElement(id("name")).sendKeys(name);
		primaryButton.click();
	}
	
	public void createRealm(String name) {
		createRealm(name, true);
	}
}
