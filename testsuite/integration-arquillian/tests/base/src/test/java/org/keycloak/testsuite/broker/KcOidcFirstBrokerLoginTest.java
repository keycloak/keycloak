package org.keycloak.testsuite.broker;

import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.openqa.selenium.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.admin.ApiUtil.removeUserByUsername;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KcOidcFirstBrokerLoginTest extends AbstractFirstBrokerLoginTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcOidcBrokerConfiguration.INSTANCE;
    }


    /**
     * Tests that duplication is detected and user wants to link federatedIdentity with existing account. He will confirm link by reauthentication
     * with different broker already linked to his account
     */
    @Test
    public void testLinkAccountByReauthenticationWithDifferentBroker() {
        KcSamlBrokerConfiguration samlBrokerConfig = KcSamlBrokerConfiguration.INSTANCE;
        ClientRepresentation samlClient = samlBrokerConfig.createProviderClients(suiteContext).get(0);
        IdentityProviderRepresentation samlBroker = samlBrokerConfig.setUpIdentityProvider(suiteContext);
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());

        try {
            updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
            adminClient.realm(bc.providerRealmName()).clients().create(samlClient);
            consumerRealm.identityProviders().create(samlBroker);

            driver.navigate().to(getAccountUrl(bc.consumerRealmName()));

            logInWithBroker(samlBrokerConfig);
            waitForAccountManagementTitle();
            accountUpdateProfilePage.assertCurrent();
            logoutFromRealm(bc.consumerRealmName());

            logInWithBroker(bc);

            waitForPage(driver, "account already exists", false);
            assertTrue(idpConfirmLinkPage.isCurrent());
            assertEquals("User with email user@localhost.com already exists. How do you want to continue?", idpConfirmLinkPage.getMessage());
            idpConfirmLinkPage.clickLinkAccount();

            assertEquals("Authenticate as testuser to link your account with " + bc.getIDPAlias(), loginPage.getInfoMessage());

            try {
                this.loginPage.findSocialButton(bc.getIDPAlias());
                org.junit.Assert.fail("Not expected to see social button with " + samlBrokerConfig.getIDPAlias());
            } catch (NoSuchElementException expected) {
            }

            log.debug("Clicking social " + samlBrokerConfig.getIDPAlias());
            loginPage.clickSocial(samlBrokerConfig.getIDPAlias());
            waitForAccountManagementTitle();
            accountUpdateProfilePage.assertCurrent();

            assertNumFederatedIdentities(consumerRealm.users().search(samlBrokerConfig.getUserLogin()).get(0).getId(), 2);
        } finally {
            updateExecutions(AbstractBrokerTest::setUpMissingUpdateProfileOnFirstLogin);
            removeUserByUsername(consumerRealm, "consumer");
        }
    }


    /**
     * Refers to in old test suite: OIDCFirstBrokerLoginTest#testMoreIdpAndBackButtonWhenLinkingAccount
     */
    @Test
    public void testLoginWithDifferentBrokerWhenUpdatingProfile() {
        KcSamlBrokerConfiguration samlBrokerConfig = KcSamlBrokerConfiguration.INSTANCE;
        ClientRepresentation samlClient = samlBrokerConfig.createProviderClients(suiteContext).get(0);
        IdentityProviderRepresentation samlBroker = samlBrokerConfig.setUpIdentityProvider(suiteContext);
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());

        try {
            updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);
            adminClient.realm(bc.providerRealmName()).clients().create(samlClient);
            consumerRealm.identityProviders().create(samlBroker);

            driver.navigate().to(getAccountUrl(bc.consumerRealmName()));
            logInWithBroker(samlBrokerConfig);
            waitForPage(driver, "update account information", false);
            updateAccountInformationPage.updateAccountInformation("FirstName", "LastName");
            logoutFromRealm(bc.consumerRealmName());

            logInWithBroker(bc);

            // User doesn't want to continue linking account. He rather wants to revert and try the other broker. Click browser "back" 3 times now
            driver.navigate().back();
            driver.navigate().back();

            // User is federated after log in with the original broker
            log.debug("Clicking social " + samlBrokerConfig.getIDPAlias());
            loginPage.clickSocial(samlBrokerConfig.getIDPAlias());
            waitForAccountManagementTitle();
            accountUpdateProfilePage.assertCurrent();

            assertNumFederatedIdentities(consumerRealm.users().search(samlBrokerConfig.getUserLogin()).get(0).getId(), 1);
        } finally {
            updateExecutions(AbstractBrokerTest::setUpMissingUpdateProfileOnFirstLogin);
            removeUserByUsername(consumerRealm, "consumer");
        }
    }

}
