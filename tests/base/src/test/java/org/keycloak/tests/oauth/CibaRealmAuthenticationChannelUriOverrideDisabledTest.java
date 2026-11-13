package org.keycloak.tests.oauth;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.CibaConfig;
import org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationChannelResponse;
import org.keycloak.protocol.oidc.grants.ciba.channel.HttpAuthenticationChannelProviderFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.CibaProvider;
import org.keycloak.testframework.oauth.DefaultOAuthClientConfiguration;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectCibaProvider;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.ciba.AuthenticationRequestAcknowledgement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests that the realm-level HTTP authentication channel URI is ignored if the realm override is not allowed.
 * The realm-level URI points to a closed port, so a CIBA login only succeeds if the server-wide URI is used.
 */
@KeycloakIntegrationTest(config = CibaRealmAuthenticationChannelUriOverrideDisabledTest.CibaServerConfig.class)
public class CibaRealmAuthenticationChannelUriOverrideDisabledTest {

    private static final String USERNAME = "ciba-user";
    private static final String PASSWORD = "password";

    private static final String SERVER_CHANNEL_URI = "http://localhost:8500" + CibaProvider.CONTEXT_REQUEST_AUTH_CHANNEL;
    private static final String REALM_CHANNEL_URI = "http://127.0.0.1:1/ciba/unused-authentication-channel";

    @InjectRealm(config = CibaRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient(config = CibaOAuthClientConfig.class)
    OAuthClient oauth;

    @InjectCibaProvider
    CibaProvider ciba;

    @AfterEach
    public void afterEach() {
        AccountHelper.logout(realm.admin(), USERNAME);
    }

    @Test
    public void realmAuthenticationChannelUriIsIgnored() throws Exception {
        Assertions.assertEquals(REALM_CHANNEL_URI,
                realm.admin().toRepresentation().getAttributes().get(CibaConfig.CIBA_HTTP_AUTHENTICATION_CHANNEL_URI));

        String bindingMessage = "realm-override-disabled";
        AuthenticationRequestAcknowledgement ack = oauth.ciba().backchannelAuthenticationRequest(USERNAME)
                .bindingMessage(bindingMessage)
                .send();
        Assertions.assertTrue(ack.isSuccess(), ack.getError() + " - " + ack.getErrorDescription());

        CibaProvider.CibaAuthenticationChannelRequest channelReq = ciba.getAuthChannel(bindingMessage);
        Assertions.assertNotNull(channelReq, "Authentication channel request was not sent to the server-wide URI");

        Assertions.assertEquals(Response.Status.OK.getStatusCode(),
                oauth.ciba().doAuthenticationChannelCallback(channelReq.getBearerToken(), AuthenticationChannelResponse.Status.SUCCEED));

        AccessTokenResponse tokenRes = oauth.ciba().doBackchannelAuthenticationTokenRequest(ack.getAuthReqId());
        Assertions.assertTrue(tokenRes.isSuccess(), tokenRes.getError() + " - " + tokenRes.getErrorDescription());
    }

    public static class CibaServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            String prefix = "spi-ciba-auth-channel-" + HttpAuthenticationChannelProviderFactory.PROVIDER_ID + "-";
            return config
                    .option(prefix + "http-authentication-channel-uri", SERVER_CHANNEL_URI)
                    .option(prefix + "allow-realm-override", "false");
        }
    }

    public static class CibaRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                    .attribute(CibaConfig.CIBA_HTTP_AUTHENTICATION_CHANNEL_URI, REALM_CHANNEL_URI)
                    .users(UserBuilder.create(USERNAME).password(PASSWORD)
                            .email("ciba-user@localhost").firstName("Ciba").lastName("User"));
        }
    }

    public static class CibaOAuthClientConfig extends DefaultOAuthClientConfiguration {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return super.configure(client)
                    .attribute(CibaConfig.CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE_PER_CLIENT, CibaConfig.CIBA_POLL_MODE)
                    .attribute(CibaConfig.OIDC_CIBA_GRANT_ENABLED, Boolean.TRUE.toString());
        }
    }
}
