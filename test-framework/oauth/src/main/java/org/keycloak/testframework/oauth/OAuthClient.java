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

    public static String SERVER_ROOT;
    public static String AUTH_SERVER_ROOT;
    public static String APP_ROOT;
    public static String APP_AUTH_ROOT;

    static {
        updateURLs(defaultServerRoot());
    }

    private final ManagedWebDriver managedWebDriver;
    private final ClientResource clientResource;

    public OAuthClient(String baseUrl, CloseableHttpClient httpClient, ManagedWebDriver managedWebDriver, ClientResource clientResource) {
        super(baseUrl, httpClient, managedWebDriver.driver());
        this.managedWebDriver = managedWebDriver;
        this.clientResource = clientResource;

        init();
    }

    public OAuthClient(String baseUrl, CloseableHttpClient httpClient, ManagedWebDriver managedWebDriver) {
        this(baseUrl, httpClient, managedWebDriver, null);
    }

    private static String defaultServerRoot() {
        boolean ssl = Boolean.parseBoolean(System.getProperty("auth.server.ssl.required", "true"));
        String scheme = ssl ? "https" : "http";
        String host = System.getProperty("auth.server.host", "localhost");
        String port = ssl ? System.getProperty("auth.server.https.port", "8543") : System.getProperty("auth.server.http.port", "8180");
        return removeDefaultPorts(String.format("%s://%s:%s", scheme, host, port));
    }

    private static String removeDefaultPorts(String url) {
        return url != null
                ? url.replaceFirst("(.*)(:80)(\\/.*)?$", "$1$3").replaceFirst("(.*)(:443)(\\/.*)?$", "$1$3")
                : null;
    }

    public static void updateURLs(String serverRoot) {
        SERVER_ROOT = removeDefaultPorts(serverRoot);
        AUTH_SERVER_ROOT = SERVER_ROOT + "/auth";
        updateAppRootRealm("master");
    }

    public static void updateAppRootRealm(String realm) {
        APP_ROOT = AUTH_SERVER_ROOT + "/realms/" + realm + "/app";
        APP_AUTH_ROOT = APP_ROOT + "/auth";
    }

    public static void resetAppRootRealm() {
        updateAppRootRealm("master");
    }

    public void init() {
        config = new OAuthClientConfig()
                .realm("test")
                .client("test-app", "password")
                .redirectUri(APP_ROOT + "/auth")
                .postLogoutRedirectUri(APP_ROOT + "/auth")
                .responseType(OAuth2Constants.CODE);
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

    public org.openqa.selenium.WebDriver getDriver() {
        return managedWebDriver.driver();
    }

    public void close() {
        if (clientResource != null) {
            clientResource.remove();
        }
    }

}
