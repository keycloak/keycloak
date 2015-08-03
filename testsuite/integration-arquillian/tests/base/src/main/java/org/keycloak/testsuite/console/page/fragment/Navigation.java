package org.keycloak.testsuite.console.page.fragment;

import org.jboss.arquillian.drone.api.annotation.Drone;
import static org.jboss.arquillian.graphene.Graphene.waitModel;
import static org.keycloak.testsuite.util.SeleniumUtils.waitGuiForElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class Navigation {

    @Drone
    private WebDriver driver;

//    @FindBy(css = "div h1")
    @FindBy(xpath = "//div[./h1]")
    private WebElement currentHeader;

    @FindBy(css = "div > h1.ng-binding")
    private WebElement tabHeader;

    protected void clickAndWaitForHeader(WebElement element, String headerText) {
        waitGuiForElement(element);
        element.click();
        waitModel().until().element(currentHeader).text().contains(headerText);
    }

    protected void clickAndWaitForTabHeader(WebElement element, String headerText) {
        waitGuiForElement(element);
        element.click();
        waitModel().until().element(tabHeader).text().contains(headerText);
    }

    public static String capitalize(String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    public void waitForHeader() {
        waitModel().until().element(currentHeader).is().present();
    }

    public void clickAndWaitForHeader(WebElement element) {
        element.click();
//        waitForHeader();
    }

}
