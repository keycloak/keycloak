package org.keycloak.testsuite.broker;

import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testsuite.arquillian.SuiteContext;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 *
 * @author hmlnarik
 */
public class BrokerTestTools {

    public static String getAuthRoot(SuiteContext suiteContext) {
        return suiteContext.getAuthServerInfo().getContextRoot().toString();
    }

    public static IdentityProviderRepresentation createIdentityProvider(String alias, String providerId) {
        IdentityProviderRepresentation identityProviderRepresentation = new IdentityProviderRepresentation();

        identityProviderRepresentation.setAlias(alias);
        identityProviderRepresentation.setDisplayName(providerId);
        identityProviderRepresentation.setProviderId(providerId);
        identityProviderRepresentation.setEnabled(true);

        return identityProviderRepresentation;
    }

    public static void waitForPage(WebDriver driver, final String title) {
        WebDriverWait wait = new WebDriverWait(driver, 5);

        ExpectedCondition<Boolean> condition = (WebDriver input) -> input.getTitle().toLowerCase().contains(title);

        wait.until(condition);
    }

    public static void waitForElementEnabled(WebDriver driver, final String elementName) {
        WebDriverWait wait = new WebDriverWait(driver, 5);

        ExpectedCondition<Boolean> condition = (WebDriver input) -> {
            List<WebElement> elements = input.findElements(By.name(elementName));
            return (! elements.isEmpty()) && elements.get(0).isEnabled();
        };

        wait.until(condition);
    }

    public static String encodeUrl(String url) {
        String result;
        try {
            result = URLEncoder.encode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            result = url;
        }

        return result;
    }
}
