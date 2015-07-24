package org.keycloak.testsuite.adapter.page;

import java.net.URL;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class JSConsoleExample extends AbstractPageWithInjectedUrl {

    public static final String DEPLOYMENT_NAME = "js-console-example";
    public static final String CLIENT_ID = "js-console";

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    @Override
    public URL getInjectedUrl() {
        return url;
    }

    @FindBy(xpath = "//button[text() = 'Login']")
    private WebElement logInButton;
    @FindBy(xpath = "//button[text() = 'Logout']")
    private WebElement logOutButton;
    @FindBy(xpath = "//button[text() = 'Refresh Token']")
    private WebElement refreshTokenButton;
    @FindBy(xpath = "//button[contains(text(),'Refresh Token (if <30s')]")
    private WebElement refreshTokenIfUnder30sButton;
    @FindBy(xpath = "//button[text() = 'Get Profile']")
    private WebElement getProfileButton;

    @FindBy(xpath = "//button[text() = 'Show Token']")
    private WebElement showTokenButton;
    @FindBy(xpath = "//button[text() = 'Show Refresh Token']")
    private WebElement showRefreshTokenButton;
    @FindBy(xpath = "//button[text() = 'Show ID Token']")
    private WebElement showIdTokenButton;
    @FindBy(xpath = "//button[text() = 'Show Expires']")
    private WebElement showExpiresButton;
    @FindBy(xpath = "//button[text() = 'Show Details']")
    private WebElement showDetailsButton;

    public void logIn() {
        logInButton.click();
    }

    public void logOut() {
        logOutButton.click();
    }

    public void refreshToken() {
        refreshTokenButton.click();
    }

    public void refreshTokenIfUnder30s() {
        refreshTokenIfUnder30sButton.click();
    }

    public void getProfile() {
        getProfileButton.click();
    }

}
