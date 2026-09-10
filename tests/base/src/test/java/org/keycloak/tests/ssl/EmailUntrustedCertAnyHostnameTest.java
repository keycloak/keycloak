package org.keycloak.tests.ssl;

import org.keycloak.config.TruststoreOptions;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.Test;

import static org.keycloak.common.enums.HostnameVerificationPolicy.ANY;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@KeycloakIntegrationTest(config = EmailUntrustedCertAnyHostnameTest.ServerConfig.class)
class EmailUntrustedCertAnyHostnameTest extends AbstractSslEmailTest {

    @Test
    void testEmailSucceedsWithSslAndAnyHostnamePolicyDespiteEmptyTruststore() {
        oauth.openLoginForm();
        loginPage.fillLogin(user.getUsername(), "password");
        loginPage.submit();

        EventRepresentation event = events.poll();
        EventAssertion.assertSuccess(event)
                .type(EventType.SEND_VERIFY_EMAIL)
                .details(Details.USERNAME, user.getUsername());

        assertThat("Email should be received because ANY hostname policy trusts all certificates",
                getLastReceivedMessage(), is(notNullValue()));
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
}
