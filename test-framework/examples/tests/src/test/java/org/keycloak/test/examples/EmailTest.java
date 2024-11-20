package org.keycloak.test.examples;

import com.nimbusds.oauth2.sdk.GeneralException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.events.email.EmailEventListenerProviderFactory;
import org.keycloak.test.framework.annotations.InjectRealm;
import org.keycloak.test.framework.annotations.InjectUser;
import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;
import org.keycloak.test.framework.mail.MailServer;
import org.keycloak.test.framework.mail.annotations.InjectMailServer;
import org.keycloak.test.framework.oauth.nimbus.OAuthClient;
import org.keycloak.test.framework.oauth.nimbus.annotations.InjectOAuthClient;
import org.keycloak.test.framework.realm.ManagedRealm;
import org.keycloak.test.framework.realm.ManagedUser;
import org.keycloak.test.framework.realm.RealmConfig;
import org.keycloak.test.framework.realm.RealmConfigBuilder;
import org.keycloak.test.framework.realm.UserConfig;
import org.keycloak.test.framework.realm.UserConfigBuilder;

import java.io.IOException;
import java.util.Map;

@KeycloakIntegrationTest
public class EmailTest {

    @InjectRealm(config = EmailSenderRealmConfig.class)
    ManagedRealm realm;

    @InjectUser(config = UserWithEmail.class)
    ManagedUser user;

    @InjectMailServer
    MailServer mail;

    @InjectOAuthClient
    OAuthClient oAuthClient;

    @Test
    public void testEmail() throws GeneralException, IOException, MessagingException {
        oAuthClient.resourceOwnerCredentialGrant(user.getUsername(), "invalid");

        Map<String, String> smtpServer = realm.admin().toRepresentation().getSmtpServer();
        Assertions.assertEquals("auto@keycloak.org", smtpServer.get("from"));
        Assertions.assertEquals("localhost", smtpServer.get("host"));
        Assertions.assertEquals("3025", smtpServer.get("port"));

        mail.waitForIncomingEmail(1);
        MimeMessage lastReceivedMessage = mail.getLastReceivedMessage();
        Assertions.assertEquals("Login error", lastReceivedMessage.getSubject());
    }

    public static class EmailSenderRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            return realm.eventsListeners(EmailEventListenerProviderFactory.ID);
        }
    }

    public static class UserWithEmail implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder user) {
            return user.username("test").email("test@local").password("password").emailVerified();
        }
    }

}
