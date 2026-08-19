package org.keycloak.tests.broker;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;

import org.junit.Ignore;

import static org.keycloak.tests.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.tests.broker.BrokerTestTools.waitForPage;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createHardcodedClaim;

@KeycloakIntegrationTest(config = org.keycloak.tests.broker.BrokerServerConfig.class)
public class KcOidcBrokerSubMatchIntrospectionTest extends AbstractBrokerTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectOAuthClient
    OAuthClient oauth;

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration() {
            @Override
            public List<ClientRepresentation> createConsumerClients() {
                List<ClientRepresentation> clients = new ArrayList<>(super.createConsumerClients());

                clients.add(ClientBuilder.create().clientId("consumer-client")
                        .publicClient()
                        .redirectUris(getConsumerRoot() + "/auth/realms/master/app/auth/*")
                        .publicClient().build());

                return clients;
            }

            @Override
            public List<ClientRepresentation> createProviderClients() {
                List<ClientRepresentation> clients = super.createProviderClients();
                List<ProtocolMapperRepresentation> mappers = new ArrayList<>();

                ProtocolMapperRepresentation hardcodedClaim = createHardcodedClaim("sub-override", "sub", "overriden",
                        "String", false, false, false);

                hardcodedClaim.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, Boolean.TRUE.toString());

                mappers.add(hardcodedClaim);

                clients.get(0).setProtocolMappers(mappers);

                return clients;
            }
        };
    }

    @Override
    public void testLogInAsUserInIDP() {
        oauth.client("broker-app");
        oauth.realm(bc.consumerRealmName());
        oauth.openLoginForm();

        oauth.realm(bc.consumerRealmName());
        oauth.client("consumer-client");

        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);

        log.debug("Logging in");
        loginPage.login(bc.getUserLogin(), bc.getUserPassword());
        errorPage.assertCurrent();
    }

    @Ignore
    @Override
    public void loginWithExistingUser() {
    }
}
