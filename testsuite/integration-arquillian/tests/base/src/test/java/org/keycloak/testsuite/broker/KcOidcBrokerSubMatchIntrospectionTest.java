package org.keycloak.testsuite.broker;

import org.junit.Ignore;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testsuite.util.ClientBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createHardcodedClaim;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;

public class KcOidcBrokerSubMatchIntrospectionTest extends AbstractBrokerTest {

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
                        "String", false, false);
                
                hardcodedClaim.getConfig().put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, Boolean.TRUE.toString());
                
                mappers.add(hardcodedClaim);
                
                clients.get(0).setProtocolMappers(mappers);
                
                return clients;
            }
        };
    }

    @Override
    public void testLogInAsUserInIDP() {
        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));

        oauth.realm(bc.consumerRealmName());
        oauth.clientId("consumer-client");

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
