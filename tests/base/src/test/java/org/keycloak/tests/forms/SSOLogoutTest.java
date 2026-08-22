package org.keycloak.tests.forms;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectDependency;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.saml.SamlClient;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class SSOLogoutTest {

    private static final String SAML_CLIENT_ID = "mixed-protocol-saml";

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectUser(config = UserConfig.class)
    ManagedUser managedUser;

    @InjectOAuthClient
    OAuthClient oidcClient;

    @InjectClient(config = SamlClientConfig.class)
    ManagedClient samlClient;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @Test
    void logoutMultipleMixedSessionOidcInitiated() throws Exception {
        // OIDC Login
        oidcClient.openLoginForm();
        loginPage.assertCurrent();
        loginPage.fillLogin(managedUser.getUsername(), managedUser.getPassword());
        loginPage.submit();

        driver.waiting().waitForOAuthCallback();

        String code = oidcClient.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oidcClient.doAccessTokenRequest(code);

        // SAML Login
        String samlEndpoint = RealmsResource.protocolUrl(UriBuilder.fromUri(keycloakUrls.getBase()))
                .build(managedRealm.getName(), SamlProtocol.LOGIN_PROTOCOL)
                .toString();
        String acsUrl = keycloakUrls.getBase() + "/mixed-protocol-saml/acs";

        AuthnRequestType loginRequest = SamlClient.createLoginRequestDocument(samlClient.getClientId(), acsUrl, URI.create(samlEndpoint));
        loginRequest.setProtocolBinding(SamlClient.Binding.POST.getBindingUri());

        String encodedRequest = new BaseSAML2BindingBuilder()
                .postBinding(SAML2Request.convert(loginRequest))
                .encoded();

        driver.open("about:blank");
        ((JavascriptExecutor) driver.driver()).executeScript("""
                const form = document.createElement('form');
                form.method = 'POST';
                form.action = arguments[0];
                const input = document.createElement('input');
                input.type = 'hidden';
                input.name = arguments[1];
                input.value = arguments[2];
                form.appendChild(input);
                document.body.appendChild(form);
                form.submit();
                """, samlEndpoint, GeneralConstants.SAML_REQUEST_KEY, encodedRequest);

        new WebDriverWait(driver.driver(), Duration.ofSeconds(10))
                .until(d -> d.getCurrentUrl() != null && d.getCurrentUrl().startsWith(acsUrl));

        Map<String, String> clients = managedUser.admin().getUserSessions().get(0).getClients();
        assertTrue(clients.containsValue(oidcClient.getClientId()), "session should include the OIDC client");
        assertTrue(clients.containsValue(samlClient.getClientId()), "session should include the SAML client");

        oidcClient.logoutForm()
                .idTokenHint(tokenResponse.getIdToken())
                .postLogoutRedirectUri(oidcClient.getRedirectUri())
                .open();

        if (driver.getCurrentUrl().contains("logout-confirm")) {
            driver.driver().findElement(org.openqa.selenium.By.id("kc-logout")).click();
        }

        new WebDriverWait(driver.driver(), Duration.ofSeconds(10))
                .until(d -> d.getCurrentUrl() != null && d.getCurrentUrl().startsWith(oidcClient.getRedirectUri()));

        // We should be directed to the OIDC Post logout Redirect URI/ Not the SAML Logout redirect
        assertTrue(driver.getCurrentUrl().startsWith(oidcClient.getRedirectUri()));
    }



    public static class SamlClientConfig implements ClientConfig {
        @InjectDependency
        ManagedRealm realm;

        @InjectDependency
        KeycloakUrls keycloakUrls;

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            String assertionConsumerUrl = keycloakUrls.getBase() + "/mixed-protocol-saml/acs";
            String singleLogoutServiceUrl = RealmsResource.protocolUrl(UriBuilder.fromUri(keycloakUrls.getBase()))
                    .build(realm.getName(), SamlProtocol.LOGIN_PROTOCOL)
                    .toString();

            return client.clientId(SAML_CLIENT_ID)
                    .enabled(true)
                    .protocol(SamlProtocol.LOGIN_PROTOCOL)
                    .redirectUris(assertionConsumerUrl)
                    .frontchannelLogout(true)
                    .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, singleLogoutServiceUrl)
                    .attribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, assertionConsumerUrl)
                    .attribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, SamlProtocol.ATTRIBUTE_FALSE_VALUE)
                    .attribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, SamlProtocol.ATTRIBUTE_FALSE_VALUE);
        }
    }

    public static class UserConfig implements org.keycloak.testframework.realm.UserConfig {
        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("test-user")
                    .password("Password123")
                    .name("test", "user")
                    .email("test-user@email.org")
                    .emailVerified(true);
        }
    }

}
