package org.keycloak.testsuite.broker;

import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.OAuthClient;
import org.openqa.selenium.Cookie;

public class KcOidcBrokerNonceParameterTest extends AbstractBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration() {
            @Override
            public List<ClientRepresentation> createConsumerClients(SuiteContext suiteContext) {
                List<ClientRepresentation> clients = new ArrayList<>(super.createConsumerClients(suiteContext));
                
                clients.add(ClientBuilder.create().clientId("consumer-client")
                        .publicClient()
                        .redirectUris("http://localhost:8180/auth/realms/master/app/auth/*", "https://localhost:8543/auth/realms/master/app/auth/*")
                        .publicClient().build());
                
                return clients;
            }
        };
    }

    @Override
    protected void loginUser() {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);

        oauth.realm(bc.consumerRealmName());
        oauth.clientId("consumer-client");
        oauth.nonce("123456");

        OAuthClient.AuthorizationEndpointResponse authzResponse = oauth
                .doLoginSocial(bc.getIDPAlias(), bc.getUserLogin(), bc.getUserPassword());
        String code = authzResponse.getCode();
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, null);
        IDToken idToken = toIdToken(response.getIdToken());
        
        Assert.assertEquals("123456", idToken.getNonce());
    }
    
    @Test
    public void testNonceNotSet() {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);

        oauth.realm(bc.consumerRealmName());
        oauth.clientId("consumer-client");
        oauth.nonce(null);

        OAuthClient.AuthorizationEndpointResponse authzResponse = oauth
                .doLoginSocial(bc.getIDPAlias(), bc.getUserLogin(), bc.getUserPassword());
        String code = authzResponse.getCode();
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, null);
        IDToken idToken = toIdToken(response.getIdToken());

        Assert.assertNull(idToken.getNonce());
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
