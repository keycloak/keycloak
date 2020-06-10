package org.keycloak.testsuite.broker;

import org.apache.http.client.utils.URIBuilder;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testsuite.pages.PageUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_PROVIDER_ID;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_HOST2;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;
import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;

/**
 *
 * @author hmlnarik
 */
public class BrokerTestTools {

    private static String providerRoot;
    private static String consumerRoot;

    public static String getProviderRoot() {
        if (providerRoot == null) {
            // everything is identical to consumerRoot but the host (it's technically the same server instance)
            providerRoot = new URIBuilder(URI.create(getConsumerRoot()))
                    .setHost(AUTH_SERVER_HOST2).toString();
        }
        return providerRoot;
    }

    public static String getConsumerRoot() {
        if (consumerRoot == null) {
            consumerRoot = getAuthServerContextRoot();
        }
        return consumerRoot;
    }

    public static IdentityProviderRepresentation createIdentityProvider(String alias, String providerId) {
        IdentityProviderRepresentation identityProviderRepresentation = new IdentityProviderRepresentation();

        identityProviderRepresentation.setAlias(alias);
        identityProviderRepresentation.setDisplayName(alias);
        identityProviderRepresentation.setProviderId(providerId);
        identityProviderRepresentation.setEnabled(true);

        return identityProviderRepresentation;
    }

    public static void waitForPage(WebDriver driver, final String title, final boolean isHtmlTitle) {
        waitForPageToLoad();

        WebDriverWait wait = new WebDriverWait(driver, 5);
        ExpectedCondition<Boolean> condition = (WebDriver input) -> isHtmlTitle ? input.getTitle().toLowerCase().contains(title) : PageUtils.getPageTitle(input).toLowerCase().contains(title);

        wait.until(condition);
    }

    public static void waitForElementEnabled(WebDriver driver, final String elementName) {
        WebDriverWait wait = new WebDriverWait(driver, 5);

        ExpectedCondition<Boolean> condition = (WebDriver input) -> {
            List<WebElement> elements = input.findElements(By.name(elementName));
            return (! elements.isEmpty()) && elements.get(0).isEnabled();
        };

        wait.until(condition);
    }

    public static String encodeUrl(String url) {
        String result;
        try {
            result = URLEncoder.encode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            result = url;
        }

        return result;
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
