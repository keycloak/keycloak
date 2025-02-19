package org.keycloak.testframework.oauth;

import org.apache.http.impl.client.CloseableHttpClient;
import org.keycloak.OAuth2Constants;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;
import org.keycloak.testsuite.util.oauth.OAuthClientConfig;
import org.openqa.selenium.WebDriver;

public class OAuthClient extends AbstractOAuthClient<OAuthClient> {

    public OAuthClient(String baseUrl, CloseableHttpClient httpClient, WebDriver webDriver) {
        super(baseUrl, httpClient, webDriver);

        config = new OAuthClientConfig()
                .responseType(OAuth2Constants.CODE);
    }

    public void close() {
    }

}
