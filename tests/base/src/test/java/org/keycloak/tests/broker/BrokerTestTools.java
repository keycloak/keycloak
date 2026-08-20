package org.keycloak.tests.broker;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.apache.http.client.utils.URIBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.keycloak.tests.broker.BrokerTestConstants.IDP_OIDC_PROVIDER_ID;
import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;

/**
 *
 * @author hmlnarik
 */
public class BrokerTestTools {

    public static String getProviderRoot() {
        String host2 = System.getProperty("auth.server.host2");
        if (host2 == null || host2.isBlank()) {
            return getConsumerRoot();
        }
        return new URIBuilder(URI.create(getConsumerRoot()))
                .setHost(host2)
                .toString();
    }

    public static String getConsumerRoot() {
        if (OAuthClient.SERVER_ROOT != null && !OAuthClient.SERVER_ROOT.isBlank()) {
            return OAuthClient.SERVER_ROOT;
        }
        return getAuthServerContextRoot();
    }

    public static String getAuthPath() {
        String path = URI.create(getConsumerRoot()).getPath();
        if (path == null || path.isBlank() || "/".equals(path)) {
            return "";
        }
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    public static IdentityProviderRepresentation createIdentityProvider(String alias, String providerId) {
        IdentityProviderRepresentation identityProviderRepresentation = new IdentityProviderRepresentation();

        identityProviderRepresentation.setAlias(alias);
        identityProviderRepresentation.setDisplayName(alias);
        identityProviderRepresentation.setProviderId(providerId);
        identityProviderRepresentation.setEnabled(true);

        return identityProviderRepresentation;
    }

    public static void waitForPage(final WebDriver driver, final String title, final boolean isHtmlTitle) {
        waitForPageToLoad();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        ExpectedCondition<Boolean> condition = new ExpectedCondition<Boolean>() {
            private String actualTitle = null;

            public Boolean apply(final WebDriver input) {
                if (input == null) {
                    return false;
                }

                actualTitle = isHtmlTitle ? input.getTitle() : input.findElement(By.id("kc-page-title")).getText();
                if (actualTitle == null) {
                    return false;
                }

                return actualTitle.toLowerCase().contains(title.toLowerCase());
            }

            public String toString() {
                return String.format("value to contain (ignoring case) \"%s\". Current value: \"%s\"", title,
                        this.actualTitle);
            }
        };

        wait.until(condition);
    }

    public static void waitForPage(final ManagedWebDriver driver, final String title, final boolean isHtmlTitle) {
        waitForPage(driver.driver(), title, isHtmlTitle);
    }

    public static void waitForElementEnabled(WebDriver driver, final String elementName) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        ExpectedCondition<Boolean> condition = (WebDriver input) -> {
            List<WebElement> elements = input.findElements(By.name(elementName));
            return (! elements.isEmpty()) && elements.get(0).isEnabled();
        };

        wait.until(condition);
    }

    public static void waitForElementEnabled(ManagedWebDriver driver, final String elementName) {
        waitForElementEnabled(driver.driver(), elementName);
    }

    public static String encodeUrl(String url) {
        return URLEncoder.encode(url, StandardCharsets.UTF_8);
    }

    /**
     * Expects a child idp and parent idp running on same Keycloak instance.  Links the two with non-signature checks.
     *
     * @param adminClient
     * @param childRealm
     * @param idpRealm
     */
    public static void createKcOidcBroker(Keycloak adminClient, String childRealm, String idpRealm) {
        createKcOidcBroker(adminClient, childRealm, idpRealm, idpRealm, false);



    }

    public static void createKcOidcBroker(Keycloak adminClient, String childRealm, String idpRealm, String alias, boolean linkOnly) {
        IdentityProviderRepresentation idp = createIdentityProvider(alias, IDP_OIDC_PROVIDER_ID);
        idp.setLinkOnly(linkOnly);
        idp.setStoreToken(true);

        Map<String, String> config = idp.getConfig();

        config.put("clientId", childRealm);
        config.put("clientSecret", childRealm);
        config.put("authorizationUrl", getProviderRoot() + "/auth/realms/" + idpRealm + "/protocol/openid-connect/auth");
        config.put("tokenUrl", getProviderRoot() + "/auth/realms/" + idpRealm + "/protocol/openid-connect/token");
        config.put("logoutUrl", getProviderRoot() + "/auth/realms/" + idpRealm + "/protocol/openid-connect/logout");
        config.put("userInfoUrl", getProviderRoot() + "/auth/realms/" + idpRealm + "/protocol/openid-connect/userinfo");
        config.put("backchannelSupported", "true");
        adminClient.realm(childRealm).identityProviders().create(idp);

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(childRealm);
        client.setName(childRealm);
        client.setSecret(childRealm);
        client.setEnabled(true);

        client.setRedirectUris(Collections.singletonList(getConsumerRoot() +
                "/auth/realms/" + childRealm + "/broker/" + idpRealm + "/endpoint/*"));

        client.setAdminUrl(getConsumerRoot() +
                "/auth/realms/" + childRealm + "/broker/" + idpRealm + "/endpoint");
        adminClient.realm(idpRealm).clients().create(client);
    }
}
