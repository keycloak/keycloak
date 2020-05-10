package org.keycloak.testsuite.broker;

import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createHardcodedClaim;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.OAuthClient;

public class KcOidcBrokerSubMatchIntrospectionest extends AbstractBrokerTest {

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

            @Override
            public List<ClientRepresentation> createProviderClients(SuiteContext suiteContext) {
                List<ClientRepresentation> clients = super.createProviderClients(suiteContext);
                List<ProtocolMapperRepresentation> mappers = new ArrayList<>();
                
                mappers.add(createHardcodedClaim("sub-override", "sub", "overriden", "String", true, true));
                
                clients.get(0).setProtocolMappers(mappers);
                
                return clients;
            }
        };
    }

    @Override
    public void testLogInAsUserInIDP() {
        driver.navigate().to(getAccountUrl(bc.consumerRealmName()));

        oauth.realm(bc.consumerRealmName());
        oauth.clientId("consumer-client");

        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "log in to", true);

        log.debug("Logging in");
        loginPage.login(bc.getUserLogin(), bc.getUserPassword());
        errorPage.assertCurrent();
    }

    @Ignore
    @Override
    public void loginWithExistingUser() {
    }
}
