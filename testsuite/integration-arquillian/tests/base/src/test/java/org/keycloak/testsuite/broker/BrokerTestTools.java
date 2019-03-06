package org.keycloak.testsuite.broker;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testsuite.arquillian.SuiteContext;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.keycloak.testsuite.pages.PageUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_PROVIDER_ID;

/**
 *
 * @author hmlnarik
 */
public class BrokerTestTools {

    public static String getAuthRoot(SuiteContext suiteContext) {
        return suiteContext.getAuthServerInfo().getContextRoot().toString();
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
     * @param suiteContext
     */
    public static void createKcOidcBroker(Keycloak adminClient, String childRealm, String idpRealm, SuiteContext suiteContext) {
        createKcOidcBroker(adminClient, childRealm, idpRealm, suiteContext, idpRealm, false);



    }

    public static void createKcOidcBroker(Keycloak adminClient, String childRealm, String idpRealm, SuiteContext suiteContext, String alias, boolean linkOnly) {
        IdentityProviderRepresentation idp = createIdentityProvider(alias, IDP_OIDC_PROVIDER_ID);
        idp.setLinkOnly(linkOnly);
        idp.setStoreToken(true);

        Map<String, String> config = idp.getConfig();

        config.put("clientId", childRealm);
        config.put("clientSecret", childRealm);
        config.put("authorizationUrl", getAuthRoot(suiteContext) + "/auth/realms/" + idpRealm + "/protocol/openid-connect/auth");
        config.put("tokenUrl", getAuthRoot(suiteContext) + "/auth/realms/" + idpRealm + "/protocol/openid-connect/token");
        config.put("logoutUrl", getAuthRoot(suiteContext) + "/auth/realms/" + idpRealm + "/protocol/openid-connect/logout");
        config.put("userInfoUrl", getAuthRoot(suiteContext) + "/auth/realms/" + idpRealm + "/protocol/openid-connect/userinfo");
        config.put("backchannelSupported", "true");
        adminClient.realm(childRealm).identityProviders().create(idp);

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(childRealm);
        client.setName(childRealm);
        client.setSecret(childRealm);
        client.setEnabled(true);

        client.setRedirectUris(Collections.singletonList(getAuthRoot(suiteContext) +
                "/auth/realms/" + childRealm + "/broker/" + idpRealm + "/endpoint/*"));

        client.setAdminUrl(getAuthRoot(suiteContext) +
                "/auth/realms/" + childRealm + "/broker/" + idpRealm + "/endpoint");
        adminClient.realm(idpRealm).clients().create(client);
    }
}
