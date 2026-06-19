package org.keycloak.tests.ssl;

import java.io.IOException;
import java.util.Map;

import jakarta.mail.internet.MimeMessage;

import org.keycloak.common.enums.HostnameVerificationPolicy;
import org.keycloak.config.TruststoreOptions;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
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
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.utils.MailUtils;
import org.keycloak.testsuite.util.AccountHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.tests.ssl.AbstractSslEmailTest.SMTP_SERVER_CERTIFICATE;
import static org.keycloak.tests.ssl.AbstractSslEmailTest.resourcePath;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@KeycloakIntegrationTest(config = EmailPlainAuthTest.ServerConfig.class)
class EmailPlainAuthTest {

    @InjectRealm(config = VerifyEmailRealmConfig.class)
    ManagedRealm realm;

    @InjectUser(config = TestUser.class)
    ManagedUser user;

    @InjectEvents
    Events events;

    @InjectMailServer
    MailServer mailServer;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @BeforeEach
    void setUp() {
        UserRepresentation userRep = user.admin().toRepresentation();
        userRep.setEmailVerified(false);
        user.admin().update(userRep);
    }

    @Test
    void testVerifyEmailWithPlainSmtpAuth() throws IOException {
        realm.updateWithCleanup(r -> {
            Map<String, String> smtp = r.build().getSmtpServer();
            smtp.put("host", "localhost.localdomain");
            smtp.put("auth", "true");
            smtp.put("ssl", "false");
            smtp.put("starttls", "false");
            smtp.put("user", "user");
            smtp.put("password", "password");
            return r;
        });

        mailServer.credentials("user", "password");

        oauth.openLoginForm();
        loginPage.fillLogin(user.getUsername(), "password");
        loginPage.submit();

        EventRepresentation event = events.poll();
        EventAssertion.assertSuccess(event)
                .type(EventType.SEND_VERIFY_EMAIL)
                .details(Details.USERNAME, user.getUsername());

        MimeMessage message = mailServer.getLastReceivedMessage();
        assertThat("Email should have been received over plain SMTP with authentication",
                message, is(notNullValue()));

        String verifyUrl = MailUtils.getPasswordResetEmailLink(message);
        driver.open(verifyUrl);

        EventAssertion.assertSuccess(events.poll()).type(EventType.VERIFY_EMAIL);
        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGIN);

        String code = oauth.parseLoginResponse().getCode();
        assertThat("Should have received auth code after verify-email flow", code, is(notNullValue()));

        AccountHelper.logout(realm.admin(), user.getUsername());
        oauth.openLoginForm();
        loginPage.fillLogin(user.getUsername(), "password");
        loginPage.submit();
        code = oauth.parseLoginResponse().getCode();
        assertThat("Should be able to log in without email verification after it was completed",
                code, is(notNullValue()));
    }

    static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            String path = resourcePath(SMTP_SERVER_CERTIFICATE);
            return config
                    .option(TruststoreOptions.TRUSTSTORE_PATHS.getKey(), path)
                    .option(TruststoreOptions.HOSTNAME_VERIFICATION_POLICY.getKey(), HostnameVerificationPolicy.ANY.name());
        }
    }

    static class VerifyEmailRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.verifyEmail(true);
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
