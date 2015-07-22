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
package org.keycloak.testsuite.admin.page;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.keycloak.testsuite.admin.util.Constants;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author Petr Mensik
 */
public class AbstractPage {

    @Drone
    protected WebDriver driver;
	
	@FindBy(css = ".btn-danger")
	protected WebElement dangerButton;
	
    @FindBy(css = ".btn-primary")
    protected WebElement primaryButton;

	@FindBy(css = ".ng-binding.btn.btn-danger")
	protected WebElement deleteConfirmationButton;
	
    public void goToPage(String page) {
        driver.get(String.format(page, Constants.CURRENT_REALM));
    }

}
