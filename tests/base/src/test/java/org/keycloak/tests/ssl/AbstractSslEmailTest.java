package org.keycloak.tests.ssl;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
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
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.VerifyEmailPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testsuite.util.AccountHelper;

import com.icegreen.greenmail.util.DummySSLServerSocketFactory;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.keycloak.testsuite.util.MailServerConfiguration.FROM;
import static org.keycloak.testsuite.util.MailServerConfiguration.HOST;
import static org.keycloak.testsuite.util.MailServerConfiguration.PORT_SSL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

abstract class AbstractSslEmailTest {

    private static final String SMTP_SERVER_KEYSTORE = "org/keycloak/tests/ssl/smtp-server.p12";
    private static boolean keystoreConfigured;
    static final String SMTP_SERVER_CERTIFICATE = "org/keycloak/tests/ssl/smtp-server.pem";
    static final String EMPTY_TRUSTSTORE = "org/keycloak/tests/ssl/empty-truststore.p12";

    private GreenMail greenMail;

    @InjectRealm(config = SslEmailRealmConfig.class)
    ManagedRealm realm;

    @InjectUser(config = TestUser.class)
    ManagedUser user;

    @InjectEvents
    Events events;

    @InjectOAuthClient(lifecycle = LifeCycle.METHOD)
    OAuthClient oauth;

    @InjectWebDriver(lifecycle = LifeCycle.CLASS)
    ManagedWebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    VerifyEmailPage verifyEmailPage;

    @InjectPage
    ErrorPage errorPage;

    @BeforeEach
    void setUp() {
        startSmtpsServer();
        setUserEmailUnverified();
    }

    @AfterEach
    void tearDown() {
        stopSmtpsServer();
    }

    MimeMessage getLastReceivedMessage() {
        MimeMessage[] messages = greenMail.getReceivedMessages();
        return messages.length > 0 ? messages[messages.length - 1] : null;
    }

    void assertEmailContent(MimeMessage message, String expectedRecipient) throws MessagingException, IOException {
        assertThat("Email recipient should match",
                message.getRecipients(MimeMessage.RecipientType.TO)[0].toString(), is(expectedRecipient));
        assertThat("Email sender should match",
                message.getFrom()[0].toString(), is(FROM));

        String body;
        if (message.getContent() instanceof MimeMultipart mimeMultipart) {
            body = String.valueOf(mimeMultipart.getBodyPart(0).getContent());
        } else {
            body = String.valueOf(message.getContent());
        }
        assertThat("Email body should contain account creation text",
                body, containsString("Someone has created a"));
    }

    void logoutAndVerifyReLogin() {
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

    private void setUserEmailUnverified() {
        UserRepresentation userRep = user.admin().toRepresentation();
        userRep.setEmailVerified(false);
        user.admin().update(userRep);
    }

    static GreenMail createSmtpsServer() {
        configureKeystore();
        GreenMail server = new GreenMail(new ServerSetup(Integer.parseInt(PORT_SSL), HOST, ServerSetup.PROTOCOL_SMTPS));
        server.start();
        return server;
    }

    private void startSmtpsServer() {
        greenMail = createSmtpsServer();
    }

    private void stopSmtpsServer() {
        if (greenMail != null) {
            greenMail.stop();
            greenMail = null;
        }
    }

    static String resourcePath(String resource) {
        URL url = AbstractSslEmailTest.class.getClassLoader().getResource(resource);
        if (url == null) {
            throw new IllegalStateException("Resource not found: " + resource);
        }
        return url.getFile();
    }

    static Map<String, String> sslSmtpConfig() {
        return Map.of("from", FROM, "host", HOST, "port", PORT_SSL, "ssl", "true");
    }

    private static void configureKeystore() {
        // TL;DR; ATM GreenMail only supports (the default) one Keystore configuration for SMTPS, and this package is
        // the only one that needs SMTPS, hence we just use it, instead of bringing new libraries like SubEthaSmtp
        if (keystoreConfigured) {
            return;
        }
        URL keystoreUrl = AbstractSslEmailTest.class.getClassLoader().getResource(SMTP_SERVER_KEYSTORE);
        if (keystoreUrl == null) {
            throw new IllegalStateException("SMTP server keystore not found: " + SMTP_SERVER_KEYSTORE);
        }
        System.setProperty(DummySSLServerSocketFactory.GREENMAIL_KEYSTORE_FILE_PROPERTY, keystoreUrl.getFile());
        System.setProperty(DummySSLServerSocketFactory.GREENMAIL_KEYSTORE_PASSWORD_PROPERTY, "changeit");
        keystoreConfigured = true;
    }

    static class SslEmailRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.verifyEmail(true);
            realm.build().setSmtpServer(sslSmtpConfig());
            return realm;
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
