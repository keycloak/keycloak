package org.keycloak.testsuite.broker;

import org.junit.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;

import java.util.List;

import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

public class KcOidcBrokerAcrParameterTest extends AbstractBrokerTest {

    private static final String ACR_VALUES = "acr_values";
    private static final String ACR_3 = "3";

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcOidcBrokerConfiguration.INSTANCE;
    }

    @Test
    public void testLogInAsUserInIDPWithAcrValues() {
        // Forward acr_values = true
        IdentityProviderResource idpRes = adminClient
                .realm(bc.consumerRealmName())
                .identityProviders()
                .get(BrokerTestConstants.IDP_OIDC_ALIAS);
        IdentityProviderRepresentation idpRep = idpRes.toRepresentation();
        OIDCIdentityProviderConfigRep cfg = new OIDCIdentityProviderConfigRep(idpRep);
        cfg.setForwardAcrValues(true);
        idpRes.update(idpRep);

        assertValidLogin(true);

        testSingleLogout();
    }

    @Override
    protected void loginUser() {
        assertValidLogin(false);
    }

    private void assertValidLogin(boolean expectHasAcrValues) {
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        driver.navigate().to(driver.getCurrentUrl() + "&" + ACR_VALUES + "=" + ACR_3);

        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());

        waitForPage(driver, "sign in to", true);

        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));

        if (expectHasAcrValues) {
            Assert.assertTrue(ACR_VALUES + "=" + ACR_3 + " SHOULD be part of the url",
                    driver.getCurrentUrl().contains(ACR_VALUES + "=" + ACR_3));
        } else {
            Assert.assertFalse(ACR_VALUES + "=" + ACR_3 + " SHOULD NOT be part of the url",
                    driver.getCurrentUrl().contains(ACR_VALUES + "=" + ACR_3));
        }

        log.debug("Logging in");
        loginPage.login(bc.getUserLogin(), bc.getUserPassword());

        waitForPage(driver, "update account information", false);

        updateAccountInformationPage.assertCurrent();
        Assert.assertTrue("We must be on correct realm right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));


        log.debug("Updating info on updateAccount page");
        updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail(), "Firstname", "Lastname");

        UsersResource consumerUsers = adminClient.realm(bc.consumerRealmName()).users();

        int userCount = consumerUsers.count();
        Assert.assertTrue("There must be at least one user", userCount > 0);

        List<UserRepresentation> users = consumerUsers.search("", 0, userCount);

        boolean isUserFound = false;
        for (UserRepresentation user : users) {
            if (user.getUsername().equals(bc.getUserLogin()) && user.getEmail().equals(bc.getUserEmail())) {
                isUserFound = true;
                break;
            }
        }

        Assert.assertTrue("There must be user " + bc.getUserLogin() + " in realm " + bc.consumerRealmName(),
                isUserFound);
    }
}
