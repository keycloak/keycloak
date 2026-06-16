package org.keycloak.testframework.oauth;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.protocol.oidc.utils.OIDCResponseMode;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.OAuthClientConfig;

import org.apache.http.impl.client.CloseableHttpClient;
import org.openqa.selenium.By;
import org.openqa.selenium.support.PageFactory;

/**
 * OAuth client to send OAuth request and handle callbacks
 */
public class OAuthClient extends AbstractOAuthClient<OAuthClient> {

    private final ManagedWebDriver managedWebDriver;
    private final ClientResource clientResource;

    public OAuthClient(String baseUrl, CloseableHttpClient httpClient, ManagedWebDriver managedWebDriver, ClientResource clientResource) {
        super(baseUrl, httpClient, managedWebDriver.driver());
        this.managedWebDriver = managedWebDriver;
        this.clientResource = clientResource;

        config = new OAuthClientConfig()
                .responseType(OAuth2Constants.CODE);
    }

    public OAuthClient(String baseUrl, CloseableHttpClient httpClient, ManagedWebDriver managedWebDriver) {
        this(baseUrl, httpClient, managedWebDriver, null);
    }

    @Override
    public void fillLoginForm(String username, String password) {
        LoginPage loginPage = new LoginPage(managedWebDriver);
        PageFactory.initElements(driver, loginPage);
        loginPage.fillLogin(username, password);
        loginPage.submit();
    }

    @Override
    public AuthorizationEndpointResponse parseLoginResponse() {
        if (config.getResponseMode() != null && config.getResponseMode().equals(OIDCResponseMode.FORM_POST.value())) {
            managedWebDriver.waiting().waitForOAuthCallback(webdriver1 -> webdriver1.findElement(By.id(OAuth2Constants.CODE)).isDisplayed() || webdriver1.findElement(By.id(OAuth2Constants.ERROR)).isDisplayed());
        } else if (config.getResponseMode() != null && config.getResponseMode().equals(OIDCResponseMode.FORM_POST_JWT.value())) {
            managedWebDriver.waiting().waitForOAuthCallback(webdriver1 -> webdriver1.findElement(By.id(OAuth2Constants.RESPONSE)).isDisplayed());
        } else {
            managedWebDriver.waiting().waitForOAuthCallback();
        }
        return super.parseLoginResponse();
    }

    public ClientRegistration clientRegistration() {
        return ClientRegistration.create().httpClient(httpClient().get()).url(baseUrl, config.getRealm()).build();
    }

    public ClientResource clientResource() {
        return clientResource;
    }

    public void close() {
        if (clientResource != null) {
            clientResource.remove();
        }
    }

}
