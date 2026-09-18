package org.keycloak.tests.broker;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.apache.http.client.utils.URIBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.keycloak.tests.broker.BrokerTestConstants.IDP_OIDC_PROVIDER_ID;

public final class BrokerTestTools {

    private static final ThreadLocal<String> SERVER_ROOT = new ThreadLocal<>();

    private BrokerTestTools() {
    }

    public static void setServerRoot(String baseUrl) {
        SERVER_ROOT.set(baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl);
    }

    public static void clearServerRoot() {
        SERVER_ROOT.remove();
    }

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
        String serverRoot = SERVER_ROOT.get();
        if (serverRoot != null && !serverRoot.isBlank()) {
            return serverRoot;
        }
        throw new IllegalStateException("BrokerTestTools server root not initialized");
    }

    public static String getAuthPath() {
        String path = URI.create(getConsumerRoot()).getPath();
        if (path == null || path.isBlank() || "/".equals(path)) {
            return "";
        }
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    public static String encodeUrl(String url) {
        return URLEncoder.encode(url, StandardCharsets.UTF_8);
    }

    public static IdentityProviderRepresentation createIdentityProvider(String alias, String providerId) {
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setAlias(alias);
        idp.setProviderId(providerId);
        idp.setEnabled(true);
        idp.setTrustEmail(true);
        idp.setStoreToken(false);
        idp.setAddReadTokenRoleOnCreate(false);
        idp.setConfig(new HashMap<>());
        return idp;
    }

    public static void waitForPage(final WebDriver driver, final String title, final boolean isHtmlTitle) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        ExpectedCondition<Boolean> condition = new ExpectedCondition<Boolean>() {
            private String actualTitle = null;

            public Boolean apply(final WebDriver input) {
                if (input == null) {
                    return false;
                }
                actualTitle = isHtmlTitle ? input.getTitle() : input.findElement(By.id("kc-page-title")).getText();
                return actualTitle != null && actualTitle.toLowerCase().contains(title.toLowerCase());
            }

            public String toString() {
                return "page title to contain '" + title + "'. Actual title: '" + actualTitle + "'";
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
            return (!elements.isEmpty()) && elements.get(0).isEnabled();
        };
        wait.until(condition);
    }

    public static void waitForElementEnabled(ManagedWebDriver driver, final String elementName) {
        waitForElementEnabled(driver.driver(), elementName);
    }

    public static ClientRepresentation findClientByClientId(List<ClientRepresentation> clients, String clientId) {
        return clients.stream()
                .filter(client -> clientId.equals(client.getClientId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Client not found: " + clientId));
    }

    public static IdentityProviderRepresentation findIdentityProvider(List<IdentityProviderRepresentation> providers, String alias) {
        return providers.stream()
                .filter(provider -> alias.equals(provider.getAlias()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Identity provider not found: " + alias));
    }

    public static boolean isOidcProvider(IdentityProviderRepresentation provider) {
        return IDP_OIDC_PROVIDER_ID.equals(provider.getProviderId());
    }

    public static Map<String, String> config(IdentityProviderRepresentation provider) {
        return provider.getConfig();
    }
}
