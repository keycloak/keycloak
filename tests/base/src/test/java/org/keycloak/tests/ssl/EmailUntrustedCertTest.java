package org.keycloak.tests.ssl;

import org.keycloak.config.TruststoreOptions;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@KeycloakIntegrationTest(config = EmailUntrustedCertTest.ServerConfig.class)
class EmailUntrustedCertTest extends AbstractSslEmailTest {


    @Test
    void testVerifyEmailWithUntrustedCert() {
        oauth.openLoginForm();
        loginPage.fillLogin(user.getUsername(), "password");
        loginPage.submit();

        EventRepresentation event = events.poll();
        EventAssertion.assertError(event)
                .type(EventType.SEND_VERIFY_EMAIL_ERROR)
                .error(Errors.EMAIL_SEND_FAILED)
                .details(Details.USERNAME, user.getUsername());

        assertThat("Email should not have been received with untrusted certificate",
                getLastReceivedMessage(), is(nullValue()));
        assertThat("Error page should show email failure message",
                errorPage.getError(), is("Failed to send email, please try again later."));
    }

    static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            String path = resourcePath(EMPTY_TRUSTSTORE);
            return config
                    .option(TruststoreOptions.TRUSTSTORE_PATHS.getKey(), path);
        }
    }
}
