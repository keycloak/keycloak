/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.ui.page.settings;

import java.util.List;
import org.jboss.arquillian.graphene.findby.ByJQuery;
import org.keycloak.testsuite.ui.model.PasswordPolicy;
import org.keycloak.testsuite.ui.page.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;
/**
 *
 * @author pmensik
 */
public class CredentialsPage extends AbstractPage {
	
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
		allRows.get(policyInputLocation).findElements(By.tagName("i")).get(0).click();
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
