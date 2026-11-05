package org.keycloak.testsuite.broker;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.broker.OIDCIdentityProviderConfigRep;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.junit.Assert;
import org.junit.Test;

import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;

public class KcOidcBrokerNonceParameterTest extends AbstractBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration() {
            @Override
            public List<ClientRepresentation> createConsumerClients() {
                List<ClientRepresentation> clients = new ArrayList<>(super.createConsumerClients());
                
                ClientRepresentation client = ClientBuilder.create().clientId("consumer-client")
                        .publicClient()
                        .redirectUris(getConsumerRoot() + "/auth/realms/master/app/auth/*")
                        .publicClient().build();

                clients.add(client);

                return clients;
            }
        };
    }

    @Override
    protected void loginUser() {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);

        oauth.realm(bc.consumerRealmName());
        oauth.clientId("consumer-client");

        AuthorizationEndpointResponse authzResponse = doLoginSocial(oauth, bc.getIDPAlias(), bc.getUserLogin(), bc.getUserPassword(), "123456");
        String code = authzResponse.getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        IDToken idToken = toIdToken(response.getIdToken());
        
        Assert.assertEquals("123456", idToken.getNonce());
        Assert.assertNotNull(brokeredIdToken(idToken.getSessionId()).getNonce());
    }
    
    @Test
    public void testNonceNotSet() {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);

        // do not send nonce at IDP provider level either
        IdentityProviderResource idpRes = adminClient.realm(bc.consumerRealmName()).identityProviders().get(BrokerTestConstants.IDP_OIDC_ALIAS);
        IdentityProviderRepresentation idpRep = idpRes.toRepresentation();
        OIDCIdentityProviderConfigRep cfg = new OIDCIdentityProviderConfigRep(idpRep);
        cfg.setDisableNonce(true);
        idpRes.update(idpRep);

        oauth.realm(bc.consumerRealmName());
        oauth.clientId("consumer-client");

        AuthorizationEndpointResponse authzResponse = doLoginSocial(oauth, bc.getIDPAlias(), bc.getUserLogin(), bc.getUserPassword(), null);
        String code = authzResponse.getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        IDToken idToken = toIdToken(response.getIdToken());

        Assert.assertNull(idToken.getNonce());
        Assert.assertNull(brokeredIdToken(idToken.getSessionId()).getNonce());
    }

    /** Read the external provider's credential from the session without exposing it through a protocol mapper. */
    private IDToken brokeredIdToken(String sessionId) {
        String encoded = testingClient.server(bc.consumerRealmName()).fetch(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserSessionModel userSession = session.sessions().getUserSession(realm, sessionId);
            return userSession.getNote(OIDCIdentityProvider.FEDERATED_ID_TOKEN);
        }, String.class);

        Assert.assertNotNull("Identity provider did not store an ID token in the user session", encoded);
        return toIdToken(encoded);
    }

    protected IDToken toIdToken(String encoded) {
        IDToken idToken;

        try {
            idToken = new JWSInput(encoded).readJsonContent(IDToken.class);
        } catch (JWSInputException cause) {
            throw new RuntimeException("Failed to deserialize RPT", cause);
        }
        return idToken;
    }
}
