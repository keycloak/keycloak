package org.keycloak.tests.broker;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.TestApp;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectTestApp;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testsuite.util.userprofile.UserProfileUtil;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.keycloak.tests.broker.BrokerTestConstants.USER_EMAIL;
import static org.keycloak.tests.broker.BrokerTestTools.waitForPage;
import static org.keycloak.tests.utils.admin.AdminApiUtil.createUserWithAdminClient;
import static org.keycloak.tests.utils.admin.AdminApiUtil.resetUserPassword;

/**
 * Base for broker tests that set up provider/consumer realms programmatically via {@link BrokerConfiguration}.
 */
public abstract class AbstractBaseBrokerTest {

    protected final Logger log = Logger.getLogger(getClass());

    @InjectAdminClient
    protected Keycloak adminClient;

    @InjectWebDriver
    protected ManagedWebDriver driver;

    @InjectOAuthClient
    protected OAuthClient oauth;

    @InjectTestApp
    protected TestApp testApp;

    @InjectKeycloakUrls
    protected KeycloakUrls keycloakUrls;

    @InjectPage
    protected LoginPage loginPage;

    @InjectPage
    protected ErrorPage errorPage;

    protected BrokerConfiguration bc = getBrokerConfiguration();

    protected abstract BrokerConfiguration getBrokerConfiguration();

    @BeforeEach
    public void beforeBrokerTest() {
        BrokerTestTools.setServerRoot(keycloakUrls.getBase());

        RealmRepresentation consumerRealm = bc.createConsumerRealm();
        RealmRepresentation providerRealm = bc.createProviderRealm();
        importRealm(consumerRealm);
        importRealm(providerRealm);

        UserProfileUtil.enableUnmanagedAttributes(adminClient.realm(consumerRealm.getRealm()).users().userProfile());
        UserProfileUtil.enableUnmanagedAttributes(adminClient.realm(providerRealm.getRealm()).users().userProfile());
    }

    @AfterEach
    public void afterBrokerTest() {
        driver.driver().manage().deleteAllCookies();
        removeRealm(bc.consumerRealmName());
        removeRealm(bc.providerRealmName());
        BrokerTestTools.clearServerRoot();
    }

    protected void importRealm(RealmRepresentation realmRepresentation) {
        removeRealm(realmRepresentation.getRealm());
        adminClient.realms().create(realmRepresentation);
    }

    protected void removeRealm(String realmName) {
        try {
            adminClient.realm(realmName).remove();
        } catch (NotFoundException ignored) {
        }
    }

    protected void addClientsToProviderAndConsumer() {
        RealmResource providerRealm = adminClient.realm(bc.providerRealmName());
        for (ClientRepresentation client : bc.createProviderClients()) {
            Response response = providerRealm.clients().create(client);
            response.close();
        }

        List<ClientRepresentation> consumerClients = bc.createConsumerClients();
        if (consumerClients != null) {
            RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());
            for (ClientRepresentation client : consumerClients) {
                if (KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_CLIENT_ID.equals(client.getClientId())) {
                    List<String> redirectUris = new ArrayList<>(client.getRedirectUris());
                    redirectUris.add(testApp.getRedirectionUri());
                    client.setRedirectUris(redirectUris);
                }
                Response response = consumerRealm.clients().create(client);
                response.close();
            }
        }
    }

    public String createUser(String realm, String username, String password, String... requiredActions) {
        UserRepresentation user = UserBuilder.create().username(username).enabled(true).build();
        user.setRequiredActions(Arrays.asList(requiredActions));
        String createdUserId = createUserWithAdminClient(adminClient.realm(realm), user);
        resetUserPassword(adminClient.realm(realm).users().get(createdUserId), password, false);
        return createdUserId;
    }

    protected String createUser(String realm, String username, String password, String firstName, String lastName, String email) {
        UserRepresentation newUser = UserBuilder.create()
                .username(username)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .enabled(true)
                .build();
        String createdUserId = createUserWithAdminClient(adminClient.realm(realm), newUser);
        resetUserPassword(adminClient.realm(realm).users().get(createdUserId), password, false);
        return createdUserId;
    }

    protected String createUser(String username, String email) {
        UserRepresentation newUser = UserBuilder.create().username(username).email(email).enabled(true).build();
        String userId = createUserWithAdminClient(adminClient.realm(bc.consumerRealmName()), newUser);
        resetUserPassword(adminClient.realm(bc.consumerRealmName()).users().get(userId), "password", false);
        return userId;
    }

    protected String createUser(String username) {
        return createUser(username, USER_EMAIL);
    }

    protected void openConsumerBrokerLoginForm() {
        oauth.client(KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_CLIENT_ID,
                KcOidcBrokerConfiguration.CONSUMER_BROKER_APP_SECRET);
        oauth.realm(bc.consumerRealmName());
        oauth.redirectUri(testApp.getRedirectionUri());
        oauth.openLoginForm();
    }

    protected void logInWithIdp(String idpAlias, String username, String password) {
        waitForPage(driver, "sign in to", true);
        String initialUrl = driver.getCurrentUrl();
        log.debug("Clicking social " + idpAlias);
        loginPage.clickSocial(idpAlias);
        try {
            new WebDriverWait(driver.driver(), Duration.ofSeconds(10)).until(webDriver ->
                    !webDriver.getCurrentUrl().equals(initialUrl));

            String brokerLoginSegment = "/broker/" + idpAlias + "/login";
            if (driver.getCurrentUrl().contains(brokerLoginSegment)) {
                List<org.openqa.selenium.WebElement> forms = driver.driver().findElements(By.tagName("form"));
                if (!forms.isEmpty()) {
                    forms.get(0).submit();
                }
                new WebDriverWait(driver.driver(), Duration.ofSeconds(10)).until(webDriver ->
                        !webDriver.getCurrentUrl().contains(brokerLoginSegment));
            }
        } catch (TimeoutException e) {
            Assertions.fail("Timed out waiting for social redirect for " + idpAlias + ". URL: " + driver.getCurrentUrl());
        }
        if (loginPage.isUsernameInputPresent()) {
            loginPage.login(username, password);
        } else if (loginPage.isPasswordInputPresent()) {
            loginPage.login(password);
        }
    }
}
