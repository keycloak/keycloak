/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.keycloak.testsuite.ui.util;

import static org.jboss.arquillian.graphene.Graphene.waitAjax;
import static org.jboss.arquillian.graphene.Graphene.waitGui;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 *
 * @author pmensik
 */
public final class SeleniumUtils {

    private SeleniumUtils() {
    }
    
    public static void waitAjaxForElement(By element) {
        waitAjax().until()
                .element(element)
                .is()
                .present();
    }
    
    public static void waitAjaxForElement(WebElement element) {
        waitAjax().until()
                .element(element)
                .is()
                .present();
    }

    public static void waitGuiForElement(By element, String message) {
		waitGui().until(message)
                .element(element)
                .is()
                .present();
	}
	
    public static void waitGuiForElement(By element) {
		waitGuiForElement(element, null);
	}
    
    public static void waitGuiForElement(WebElement element) {
		waitGuiForElement(element, null);
	}
	
	public static void waitGuiForElement(WebElement element, String message) {
        waitGui().until(message)
                .element(element)
                .is()
                .present();
    }
}
