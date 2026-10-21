package org.keycloak.tests.broker.oidc;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class KcOidcBrokerLogoutTest extends AbstractKcOidcBrokerTest {

    @Test
    public void consumerInitiatedLogoutWithUpstreamBackchannelLogout() {
        IdentityProviderRepresentation idp = consumerRealm.admin().identityProviders().get(getIdpAlias()).toRepresentation();
        idp.getConfig().put("backchannelSupported", "false");
        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idp);

        ClientRepresentation providerClient = providerRealm.admin().clients().findByClientId(CLIENT_ID).get(0);
        providerClient.setFrontchannelLogout(false);
        providerClient.getAttributes().put(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_SESSION_REQUIRED, "true");
        providerRealm.admin().clients().get(providerClient.getId()).update(providerClient);

        // Use the application callback for the final redirect, after both realms complete logout.
        ClientResource consumerClient = consumerRealm.admin().clients().get(
                consumerRealm.admin().clients().findByClientId(CONSUMER_BROKER_APP_CLIENT_ID).get(0).getId());
        consumerClient.update(ClientBuilder.update(consumerClient.toRepresentation())
                .attribute(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "+")
                .build());

        loginUser();
        String consumerUserId = AccountHelper.getUserRepresentation(consumerRealm.admin(), getUserLogin()).getId();
        String providerUserId = AccountHelper.getUserRepresentation(providerRealm.admin(), getUserLogin()).getId();
        assertEquals(1, consumerRealm.admin().users().get(consumerUserId).getUserSessions().size());
        assertEquals(1, providerRealm.admin().users().get(providerUserId).getUserSessions().size());

        AccessTokenResponse tokens = oauth.doAccessTokenRequest(oauth.parseLoginResponse().getCode());
        assertEquals(200, tokens.getStatusCode());

        String state = "broker-logout-state";
        oauth.logoutForm()
                .idTokenHint(tokens.getIdToken())
                .postLogoutRedirectUri(oauth.getRedirectUri())
                .state(state)
                .open();

        // Session removal alone is insufficient: the upstream notification must not break the
        // consumer's pending browser logout callback and lose the application's redirect/state.
        assertTrue(consumerRealm.admin().users().get(consumerUserId).getUserSessions().isEmpty());
        assertTrue(providerRealm.admin().users().get(providerUserId).getUserSessions().isEmpty());
        assertEquals(oauth.getRedirectUri() + "?state=" + state, webDriver.getCurrentUrl());
    }
}
