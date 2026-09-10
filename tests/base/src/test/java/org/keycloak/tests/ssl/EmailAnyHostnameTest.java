package org.keycloak.tests.ssl;

import jakarta.mail.internet.MimeMessage;

import org.keycloak.config.TruststoreOptions;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.utils.MailUtils;

import org.junit.jupiter.api.Test;

import static org.keycloak.common.enums.HostnameVerificationPolicy.ANY;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@KeycloakIntegrationTest(config = EmailAnyHostnameTest.ServerConfig.class)
class EmailAnyHostnameTest extends AbstractSslEmailTest {

    @Test
    void testVerifyEmailWithSslWrongHostnameSucceeds() throws Exception {
        realm.updateWithCleanup(r -> {
            r.build().getSmtpServer().put("host", "localhost.localdomain");
            return r;
        });

        oauth.openLoginForm();
        loginPage.fillLogin(user.getUsername(), "password");
        loginPage.submit();

        EventRepresentation event = events.poll();
        EventAssertion.assertSuccess(event)
                .type(EventType.SEND_VERIFY_EMAIL)
                .details(Details.USERNAME, user.getUsername());

        MimeMessage message = getLastReceivedMessage();
        assertThat("Email should have been received despite hostname mismatch with ANY policy",
                message, is(notNullValue()));
        assertEmailContent(message, user.getUsername());

        String verifyUrl = MailUtils.getPasswordResetEmailLink(message);
        driver.open(verifyUrl);

        EventAssertion.assertSuccess(events.poll()).type(EventType.VERIFY_EMAIL);
        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGIN);

        logoutAndVerifyReLogin();
    }

    static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            String path = resourcePath(SMTP_SERVER_CERTIFICATE);
            return config
                    .option(TruststoreOptions.TRUSTSTORE_PATHS.getKey(), path)
                    .option(TruststoreOptions.HOSTNAME_VERIFICATION_POLICY.getKey(), ANY.name());
        }
    }
}
