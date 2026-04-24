package org.keycloak.tests.ssl;

import java.util.Map;

import jakarta.mail.internet.MimeMessage;

import org.keycloak.config.TruststoreOptions;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.LoginPage;

import com.icegreen.greenmail.util.GreenMail;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.common.enums.HostnameVerificationPolicy.ANY;
import static org.keycloak.tests.ssl.AbstractSslEmailTest.EMPTY_TRUSTSTORE;
import static org.keycloak.tests.ssl.AbstractSslEmailTest.resourcePath;
import static org.keycloak.testsuite.util.MailServerConfiguration.FROM;
import static org.keycloak.testsuite.util.MailServerConfiguration.HOST;
import static org.keycloak.testsuite.util.MailServerConfiguration.PORT_SSL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@KeycloakIntegrationTest(config = TrustStoreEmailUntrustedCertAnyHostnameTest.ServerConfig.class)
class TrustStoreEmailUntrustedCertAnyHostnameTest {


    private GreenMail greenMail;

    @InjectRealm(config = StarttlsEmailRealmConfig.class)
    ManagedRealm realm;

    @InjectUser(config = TestUser.class)
    ManagedUser user;

    @InjectEvents
    Events events;

    @InjectOAuthClient(lifecycle = LifeCycle.METHOD)
    OAuthClient oauth;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    ErrorPage errorPage;

    @BeforeEach
    void setUp() {
        greenMail = AbstractSslEmailTest.createSmtpsServer();

        UserRepresentation userRep = user.admin().toRepresentation();
        userRep.setEmailVerified(false);
        user.admin().update(userRep);
    }

    @AfterEach
    void tearDown() {
        if (greenMail != null) {
            greenMail.stop();
            greenMail = null;
        }
    }

    @Test
    void testVerifyEmailWithUntrustedCertAndAnyHostnamePolicy() {
        oauth.openLoginForm();
        loginPage.fillLogin(user.getUsername(), "password");
        loginPage.submit();

        EventRepresentation event = events.poll();
        EventAssertion.assertError(event)
                .type(EventType.SEND_VERIFY_EMAIL_ERROR)
                .error(Errors.EMAIL_SEND_FAILED)
                .details(Details.USERNAME, user.getUsername());

        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat("Email should not have been received even with ANY hostname policy",
                messages.length > 0 ? messages[messages.length - 1] : null, is(nullValue()));
        assertThat("Error page should show email failure message",
                errorPage.getError(), is("Failed to send email, please try again later."));
    }

    static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            String path = resourcePath(EMPTY_TRUSTSTORE);
            return config
                    .option(TruststoreOptions.TRUSTSTORE_PATHS.getKey(), path)
                    .option(TruststoreOptions.HOSTNAME_VERIFICATION_POLICY.getKey(), ANY.name());
        }
    }

    static class StarttlsEmailRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.verifyEmail(true);
            realm.build().setSmtpServer(starttlsSmtpConfig());
            return realm;
        }

        private static Map<String, String> starttlsSmtpConfig() {
            return Map.of("from", FROM, "host", HOST, "port", PORT_SSL, "starttls", "true");
        }
    }

    static class TestUser implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("test-user@localhost")
                       .name("Test", "User")
                       .email("test-user@localhost")
                       .password("password")
                       .emailVerified(true);
        }
    }
}
