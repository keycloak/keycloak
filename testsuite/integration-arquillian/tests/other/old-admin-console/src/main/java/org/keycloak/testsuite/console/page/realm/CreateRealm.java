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

package org.keycloak.testsuite.console.page.realm;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.openqa.selenium.By.id;

/**
 *
 * @author Petr Mensik
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
