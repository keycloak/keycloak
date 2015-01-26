/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.ui.fragment;

import org.jboss.arquillian.graphene.fragment.Root;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

/**
 *
 * @author pmensik
 */
public class OnOffSwitch {

	@Root
	private WebElement root;
	
	@ArquillianResource
	private Actions actions;
	
	public boolean isEnabled() {
		return root.findElement(By.tagName("input")).isSelected();
	}
	
	private void click() {
		actions.moveToElement(root.findElements(By.tagName("span")).get(0))
				.click().build().perform();
	}
	
	public void toggle() {
		click();
	}
	
	public void enable() {
		if(!isEnabled()) {
			click();
		}
	}
	
	public void disable() {
		if(isEnabled()) {
			click();
		}
	}
}
