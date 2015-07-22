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

package org.keycloak.testsuite.admin.page.settings;

import java.util.List;
import org.jboss.arquillian.graphene.findby.ByJQuery;
import org.keycloak.testsuite.admin.model.PasswordPolicy;
import org.keycloak.testsuite.admin.page.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;
/**
 *
 * @author Petr Mensik
 */
public class PasswordPolicyPage extends AbstractPage {
	
	@FindBy(tagName = "select")
	private Select addPolicySelect;
	
	@FindBy(css = "tr.ng-scope")
	private List<WebElement> allRows;
	
	public void addPolicy(PasswordPolicy policy, int value) {
		addPolicySelect.selectByVisibleText(policy.getName());
		setPolicyValue(policy, value);
		primaryButton.click();
	}
	
	public void removePolicy(PasswordPolicy policy) {
		int policyInputLocation = findPolicy(policy);
		allRows.get(policyInputLocation).findElements(By.tagName("button")).get(0).click();
		primaryButton.click();
	}
	
	public void editPolicy(PasswordPolicy policy, int value) {
		setPolicyValue(policy, value);
		primaryButton.click();
	}
	
	private void setPolicyValue(PasswordPolicy policy, int value) {
		int policyInputLocation = findPolicy(policy);
		allRows.get(policyInputLocation).findElement(By.tagName("input")).sendKeys(String.valueOf(value));
	}
	
	private int findPolicy(PasswordPolicy policy) {
		for (int i = 0; i < allRows.size(); i++) {
			String policyName = allRows.get(i).findElement(ByJQuery.selector("td:eq(0)")).getText();
			if(policyName.equals(policy.getName()))
				return i;
		}
		return 0;
	}
}
