package org.keycloak.tests.forms;

import java.util.Objects;

import org.keycloak.crypto.Algorithm;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.Constants;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.https.CertificatesConfig;
import org.keycloak.testframework.https.CertificatesConfigBuilder;
import org.keycloak.testframework.https.InjectCertificates;
import org.keycloak.testframework.https.ManagedCertificates;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.suites.DatabaseTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest
public class LoginSSLTest {

    // Share realm configuration with LoginTest
    @InjectRealm(ref = "login-test", config = LoginTest.LoginRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient(realmRef = "login-test")
    OAuthClient oauth;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectEvents(realmRef = "login-test")
    Events events;

    @InjectPage
    protected LoginPage loginPage;

    @InjectCertificates(config = TlsEnabledConfig.class)
    ManagedCertificates managedCertificates;

    private static class TlsEnabledConfig implements CertificatesConfig {

        @Override
        public CertificatesConfigBuilder configure(CertificatesConfigBuilder config) {
            return config.tlsEnabled(true);
        }
    }

    @Test
    @DatabaseTest
    public void loginSuccessRealmSigningAlgorithms() throws JWSInputException {
        managedRealm.updateUser("login-test", user -> user.password("test"));
        String userId = managedRealm.admin().users().search("login-test", true).get(0).getId();

        oauth.openLoginForm();
        loginPage.fillLogin("login-test", "test");
        loginPage.submit();

        assertNotNull(oauth.parseLoginResponse().getCode());

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .userId(userId)
                .details(Details.USERNAME, "login-test");

        driver.driver().navigate().to(keycloakUrls.getBase() + "/realms/" + managedRealm.getName() + "/");
        String keycloakIdentity = Objects.requireNonNull(driver.driver().manage().getCookieNamed("KEYCLOAK_IDENTITY")).getValue();

        // Check identity cookie is signed with HS256
        String algorithm = new JWSInput(keycloakIdentity).getHeader().getAlgorithm().name();
        assertEquals(Constants.INTERNAL_SIGNATURE_ALGORITHM, algorithm);

        // Change realm signature algorithm
        managedRealm.updateWithCleanup(realm -> realm.defaultSignatureAlgorithm(Algorithm.ES256));

        oauth.openLoginForm();

        driver.driver().navigate().to(keycloakUrls.getBase() + "/realms/" + managedRealm.getName() + "/");
        keycloakIdentity = Objects.requireNonNull(driver.driver().manage().getCookieNamed("KEYCLOAK_IDENTITY")).getValue();

        // Check identity cookie is still signed with HS256
        algorithm = new JWSInput(keycloakIdentity).getHeader().getAlgorithm().name();
        assertEquals(Constants.INTERNAL_SIGNATURE_ALGORITHM, algorithm);

        // Check identity cookie still works
        oauth.openLoginForm();
        assertNotNull(oauth.parseLoginResponse().getCode());
    }
}
