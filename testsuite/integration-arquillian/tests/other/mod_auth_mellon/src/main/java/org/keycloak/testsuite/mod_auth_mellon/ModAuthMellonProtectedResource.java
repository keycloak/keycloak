package org.keycloak.testsuite.mod_auth_mellon;

import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author mhajas
 */
public class ModAuthMellonProtectedResource extends AbstractPageWithInjectedUrl {

    @FindBy(linkText = "logout")
    private WebElement logoutButton;

    @Override
    public URL getInjectedUrl() {
        try {
            return new URL(System.getProperty("apache.mod_auth_mellon.url", "http://localhost:8380") + "/auth");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void logout() {
        logoutButton.click();
    }
}
