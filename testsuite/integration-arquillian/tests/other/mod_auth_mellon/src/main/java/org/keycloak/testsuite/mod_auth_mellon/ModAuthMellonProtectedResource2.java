package org.keycloak.testsuite.mod_auth_mellon;

import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author mhajas
 */
public class ModAuthMellonProtectedResource2 extends AbstractPageWithInjectedUrl {

    @FindBy(linkText = "logout")
    private WebElement logoutButton;

    @Override
    public URL getInjectedUrl() {
        try {
            return new URL(System.getProperty("apache.mod_auth_mellon2.url", "https://app-saml-127-0-0-1.nip.io:8843") + "/auth2");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void logout() {
        logoutButton.click();
    }
}
