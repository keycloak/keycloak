package org.keycloak.tests.broker.oidc;

import java.util.List;

import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.HardcodedClaim;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ProtocolMapperBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.VerifyEmailPage;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * The e-mail sent by the identity provider is stored in lower case, which must not be treated as an e-mail
 * change made by the user in the first-broker-login review-profile form. Otherwise the address of a trusted
 * provider is created as unverified and the user has to verify it again. A different address must still be
 * treated as a change, even when it differs by characters that a case-insensitive comparison considers equal.
 */
@KeycloakIntegrationTest
public class KcOidcBrokerTrustEmailUppercaseEmailTest extends AbstractKcOidcBrokerTest {

    // The provider sends the address with an uppercase letter, which is what directories such as
    // Firstname.Lastname@example.com do, and with the dotless 'ı', which equalsIgnoreCase treats as equal
    // to 'i'. A user attribute cannot hold the address because e-mails are lower-cased on save, so the
    // claim is hardcoded on the provider client instead
    private static final String IDP_EMAIL = "Probe@gıthub.test";

    // Differs from the address the provider sends only by 'ı' and 'i', which equalsIgnoreCase considers
    // equal although the lower-casing done by the user profile keeps them distinct
    private static final String CHANGED_EMAIL = "probe@github.test";

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD, config = UppercaseEmailProviderRealmConfig.class)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD, config = TrustEmailConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    // Configures an SMTP server in the realms, so the verification e-mail of the changed address is sent
    @InjectMailServer
    MailServer mailServer;

    @InjectPage
    VerifyEmailPage verifyEmailPage;

    // The shared broker tests expect the user name and the profile of the default provider user, which this
    // fixture does not send, so they are replaced by the scenario below
    @Override
    @Test
    @Disabled("The provider sends only the e-mail claim, so the shared broker login scenarios do not apply")
    public void testLogInAsUserInIDP() {
    }

    @Override
    @Test
    @Disabled("The provider sends only the e-mail claim, so the shared broker login scenarios do not apply")
    public void testLoginWithExistingUser() {
    }

    @Test
    public void testTrustEmailAppliedWhenEmailDiffersOnlyInCase() {
        oauth.openLoginForm();
        logInWithBroker();
        logInAsUserInIDPForFirstTime();

        // the provider user has no first and last name, so the review-profile form is shown
        updateAccountInformation();

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess(),
                "The trusted e-mail must not require an e-mail verification");

        // no preferred_username claim is sent, so the user name is derived from the e-mail address
        List<UserRepresentation> users = consumerRealm.admin().users().search(IDP_EMAIL.toLowerCase(), true);
        Assertions.assertEquals(1, users.size(), "The user should have been created in the consumer realm");
        UserRepresentation user = users.get(0);
        Assertions.assertEquals(IDP_EMAIL.toLowerCase(), user.getEmail());
        Assertions.assertTrue(user.isEmailVerified(), "The e-mail of the trusted provider should be verified");
    }

    @Test
    public void testChangedEmailRequiresVerification() throws MessagingException {
        oauth.openLoginForm();
        logInWithBroker();
        logInAsUserInIDPForFirstTime();

        // the provider user has no first and last name, so the review-profile form is shown
        updateProfilePage.assertCurrent();
        updateProfilePage.update("Firstname", "Lastname", CHANGED_EMAIL);

        verifyEmailPage.assertCurrent();
        Assertions.assertTrue(mailServer.waitForIncomingEmail(5000, 1),
                "The changed e-mail address should receive a verification e-mail");

        // the address submitted in the form must be the one stored and verified, not the address of the provider
        List<UserRepresentation> users = consumerRealm.admin().users().search(IDP_EMAIL.toLowerCase(), true);
        Assertions.assertEquals(1, users.size(), "The user should have been created in the consumer realm");
        UserRepresentation user = users.get(0);
        Assertions.assertEquals(CHANGED_EMAIL, user.getEmail(), "The submitted e-mail address should be stored");
        Assertions.assertFalse(user.isEmailVerified(), "A changed e-mail address must require verification");

        MimeMessage[] messages = mailServer.getReceivedMessages();
        Assertions.assertEquals(1, messages.length, "Only the changed address should receive a verification e-mail");
        Address[] recipients = messages[0].getAllRecipients();
        Assertions.assertNotNull(recipients);
        Assertions.assertEquals(1, recipients.length);
        Assertions.assertEquals(CHANGED_EMAIL, ((InternetAddress) recipients[0]).getAddress(),
                "The verification e-mail should be sent to the submitted e-mail address");
    }

    // Only the e-mail claim is needed here, so the provider client keeps the basic client scope and the
    // broker asks for the openid scope only. The given and family name claims are not sent, which is what
    // makes the review-profile form appear
    static ClientBuilder createUppercaseEmailProviderClient() {
        return ClientBuilder.create(CLIENT_ID)
                .secret(CLIENT_SECRET)
                .redirectUris("http://localhost:8080/broker/" + IDP_OIDC_ALIAS + "/endpoint/*")
                .adminUrl("http://localhost:8080/broker/" + IDP_OIDC_ALIAS + "/endpoint")
                .defaultClientScopes("basic")
                .protocolMappers(ProtocolMapperBuilder.create()
                        .name("email")
                        .protocolMapper(HardcodedClaim.PROVIDER_ID)
                        .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                        .config(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "email")
                        .config(OIDCAttributeMapperHelper.JSON_TYPE, "String")
                        .config(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true")
                        .config(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true")
                        .config(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "true")
                        .config(HardcodedClaim.CLAIM_VALUE, IDP_EMAIL)
                        .build());
    }

    static class UppercaseEmailProviderRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.name(PROVIDER_REALM)
                    .eventsListeners("jboss-logging")
                    .users(UserBuilder.create(USER_LOGIN)
                            .email(USER_EMAIL)
                            .emailVerified(true)
                            .password(USER_PASSWORD)
                            .enabled(true))
                    .clients(createUppercaseEmailProviderClient());
        }
    }

    static class TrustEmailConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return configureConsumerRealm(realm, createOidcIdentityProvider()
                            .trustEmail(true)
                            .attribute("defaultScope", "openid"))
                    .verifyEmail(true);
        }
    }
}
