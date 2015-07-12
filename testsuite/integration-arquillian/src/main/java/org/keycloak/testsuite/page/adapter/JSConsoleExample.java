package org.keycloak.testsuite.page.adapter;

import java.net.URL;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class JSConsoleExample extends AbstractPageWithInjectedUrl {

    public static final String DEPLOYMENT_NAME = "js-console-example";

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    @Override
    public URL getInjectedUrl() {
        return url;
    }

    @FindBy(xpath = "//button[contains(@onclick,'keycloak.login()')]")
    private WebElement logInButton;
    @FindBy(xpath = "//button[contains(@onclick,'keycloak.logout()')]")
    private WebElement logOutButton;

    public void logIn() {
        logInButton.sendKeys(Keys.RETURN);
    }

    public void logOut() {
        logOutButton.sendKeys(Keys.RETURN);
    }

}
