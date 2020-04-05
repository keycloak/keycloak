package org.keycloak.testsuite.adapter.page;

import org.jboss.arquillian.graphene.wait.ElementBuilder;
import org.keycloak.testsuite.page.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;

import javax.ws.rs.core.UriBuilder;
import org.keycloak.testsuite.util.JavascriptBrowser;

import java.util.concurrent.TimeUnit;

import static org.jboss.arquillian.graphene.Graphene.waitGui;
import static org.keycloak.testsuite.util.WaitUtils.pause;

/**
 * @author mhajas
 */
public class HawtioPage extends AbstractPage {

    public String getUrl() {
        if (Boolean.parseBoolean(System.getProperty("app.server.ssl.required"))) {
            return "https://localhost:" + System.getProperty("app.server.https.port", "8543") + "/hawtio";
        }
        return "http://localhost:" + System.getProperty("app.server.http.port", "8180") + "/hawtio";
    }

    @Override
    public UriBuilder createUriBuilder() {
        return UriBuilder.fromUri(getUrl());
    }

    @FindBy(xpath = "//a[@class='dropdown-toggle' and @data-original-title='Preferences and log out']")
    @JavascriptBrowser
    private WebElement dropDownMenu;

    @FindBy(xpath = "//a[@ng-click='logout()']")
    @JavascriptBrowser
    private WebElement logoutButton;

    @FindBy(xpath = "//input[@type='submit' and @value='Yes']")
    @JavascriptBrowser
    private WebElement modal;

    public void logout(WebDriver jsDriver) {
        log.debug("logging out");
        hawtioWaitUntil(dropDownMenu).is().clickable();
        dropDownMenu.click();

        // There is a tooltip shown which prevents logout button from clicking
        // So we need to move mouse away from dropDownMenu element
        new Actions(jsDriver).moveToElement(logoutButton).perform();
        pause(100); // Wait for tooltip to fade out

        hawtioWaitUntil(logoutButton).is().clickable();
        logoutButton.click();
        hawtioWaitUntil(modal).is().clickable();
        modal.click();
    }

    public ElementBuilder<Void> hawtioWaitUntil(WebElement element) {
        return waitGui().withTimeout(3, TimeUnit.MINUTES).until().element(element);
    }

    public ElementBuilder<Void> hawtioWaitUntil(By element) {
        return waitGui().withTimeout(3, TimeUnit.MINUTES).until().element(element);
    }
}
