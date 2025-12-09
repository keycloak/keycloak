package org.keycloak.testframework.ui.page;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class OAuthGrantPage extends AbstractLoginPage {

    // Locale-resolved built-in client scope consents
    public static final String PROFILE_CONSENT_TEXT = "User profile";
    public static final String EMAIL_CONSENT_TEXT = "Email address";
    public static final String ADDRESS_CONSENT_TEXT = "Address";
    public static final String PHONE_CONSENT_TEXT = "Phone number";
    public static final String OFFLINE_ACCESS_CONSENT_TEXT = "Offline Access";
    public static final String ROLES_CONSENT_TEXT = "User roles";

    @FindBy(css = "[name=\"accept\"]")
    private WebElement acceptButton;
    @FindBy(css = "[name=\"cancel\"]")
    private WebElement cancelButton;

    public OAuthGrantPage(ManagedWebDriver driver) {
        super(driver);
    }

    public void accept(){
        acceptButton.click();
    }

    public void cancel(){
        cancelButton.click();
    }

    public List<String> getDisplayedGrants() {
        List<String> table = new ArrayList<>();
        WebElement divKcOauth = driver.findElement(By.id("kc-oauth"));
        for (WebElement li : divKcOauth.findElements(By.tagName("li"))) {
            WebElement span = li.findElement(By.tagName("span"));
            table.add(span.getText());
        }
        return table;
    }

    public void assertGrants(String... expectedGrants) {
        List<String> displayed = getDisplayedGrants();
        List<String> expected = Arrays.asList(expectedGrants);
        Assertions.assertTrue(displayed.containsAll(expected) && expected.containsAll(displayed),
                "Not matched grants. Displayed grants: " + displayed + ", expected grants: " + expected);
    }

    @Override
    public String getExpectedPageId() {
        return "login-login-oauth-grant";
    }

}
