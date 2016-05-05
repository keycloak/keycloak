package org.keycloak.testsuite.adapter.page;

import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;
import org.keycloak.util.JsonSerialization;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.io.IOException;
import java.net.URL;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>.
 */
public class OfflineToken extends AbstractPageWithInjectedUrl {

    public static final String DEPLOYMENT_NAME = "offline-client";

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    @Override
    public URL getInjectedUrl() {
        return url;
    }

    @FindBy(id = "accessToken")
    private WebElement accessToken;

    @FindBy(id = "refreshToken")
    private WebElement refreshToken;

    @FindBy(id = "prettyToken")
    private WebElement prettyToken;


    public AccessToken getAccessToken() {
        try {
            return JsonSerialization.readValue(accessToken.getText(), AccessToken.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public RefreshToken getRefreshToken() {
        try {
            return JsonSerialization.readValue(refreshToken.getText(), RefreshToken.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}