/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.keycloak.testsuite.ui.fragment;

import org.jboss.arquillian.graphene.fragment.Root;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import org.openqa.selenium.support.ui.Select;

/**
 *
 * @author pmensik
 */
public class PickList {

    @Root
    private WebElement root;

    private Select firstSelect;
    private Select secondSelect;
    
    @FindBy(className = "kc-icon-arrow-right")
    private WebElement rightArrow;
    
    @FindBy(className = "kc-icon-arrow-left")
    private WebElement leftArrow;
    
    public void addItems(String... values) {
        for(String value : values) {
            firstSelect.selectByVisibleText(value);
        }
        rightArrow.click();
    }
    
    public void setFirstSelect(By locator) {
        firstSelect = new Select(root.findElement(locator));
    }
    
    public void setSecondSelect(By locator) {
        secondSelect = new Select(root.findElement(locator));
    }
    
}
