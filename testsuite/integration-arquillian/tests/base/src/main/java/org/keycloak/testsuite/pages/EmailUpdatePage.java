/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.pages;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.keycloak.testsuite.util.UIUtils.getTextFromElement;
import static org.keycloak.testsuite.util.UIUtils.isElementVisible;

public class EmailUpdatePage extends AbstractPage {

	@FindBy(id = "email")
	private WebElement emailInput;

	@FindBy(id = "kc-submit")
	private WebElement submitButton;

	@FindBy(id = "kc-cancel")
	private WebElement cancelAIAButton;

	@FindBy(id = "input-error-email")
	private WebElement emailError;

	public void changeEmail(String newEmail) {
		emailInput.clear();
		emailInput.sendKeys(newEmail);

		submitButton.click();
	}

	public void cancel() {
		cancelAIAButton.click();
	}

	@Override
	public boolean isCurrent() {
		return PageUtils.getPageTitle(driver).equals("Update email");
	}

	public String getEmailError() {
		try {
			return getTextFromElement(emailError);
		} catch (NoSuchElementException e) {
			return null;
		}
	}

	public boolean isCancelDisplayed() {
		return isElementVisible(cancelAIAButton);
	}
}
