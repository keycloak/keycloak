package org.keycloak.testsuite.broker;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.pages.LoginUpdateProfilePage;
import org.openqa.selenium.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.admin.ApiUtil.removeUserByUsername;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KcOidcFirstBrokerLoginTest extends AbstractFirstBrokerLoginTest {

    @Page
    protected LoginUpdateProfilePage loginUpdateProfilePage;

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcOidcBrokerConfiguration.INSTANCE;
    }

    /**
     * KEYCLOAK-10932
     */
    @Test
    public void loginWithFirstnameLastnamePopulatedFromClaims() {

        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);

        String firstname = "Firstname";
        String lastname = "Lastname";
        String username = "firstandlastname";
        createUser(bc.providerRealmName(), username, BrokerTestConstants.USER_PASSWORD, firstname, lastname, "firstnamelastname@example.org");

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        logInWithIdp(bc.getIDPAlias(), username, BrokerTestConstants.USER_PASSWORD);

        accountUpdateProfilePage.assertCurrent();

        assertEquals(username, accountUpdateProfilePage.getUsername());
        assertEquals(firstname, accountUpdateProfilePage.getFirstName());
        assertEquals(lastname, accountUpdateProfilePage.getLastName());
    }

    /**
     * Tests that duplication is detected and user wants to link federatedIdentity with existing account. He will confirm link by reauthentication
     * with different broker already linked to his account
     */
    @Test
    public void testLinkAccountByReauthenticationWithDifferentBroker() {
        KcSamlBrokerConfiguration samlBrokerConfig = KcSamlBrokerConfiguration.INSTANCE;
        ClientRepresentation samlClient = samlBrokerConfig.createProviderClients().get(0);
        IdentityProviderRepresentation samlBroker = samlBrokerConfig.setUpIdentityProvider();
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());

        try {
            updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
            adminClient.realm(bc.providerRealmName()).clients().create(samlClient);
            consumerRealm.identityProviders().create(samlBroker);

            driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));

            logInWithBroker(samlBrokerConfig);
            waitForAccountManagementTitle();
            accountUpdateProfilePage.assertCurrent();
            logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());

            logInWithBroker(bc);

            waitForPage(driver, "account already exists", false);
            assertTrue(idpConfirmLinkPage.isCurrent());
            assertEquals("User with email user@localhost.com already exists. How do you want to continue?", idpConfirmLinkPage.getMessage());
            idpConfirmLinkPage.clickLinkAccount();

            assertEquals("Authenticate to link your account with " + bc.getIDPAlias(), loginPage.getInfoMessage());

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

    @Test
    public void testFilterMultipleBrokerWhenReauthenticating() {
        KcSamlBrokerConfiguration samlBrokerConfig = KcSamlBrokerConfiguration.INSTANCE;
        ClientRepresentation samlClient = samlBrokerConfig.createProviderClients().get(0);
        IdentityProviderRepresentation samlBroker = samlBrokerConfig.setUpIdentityProvider();
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());

        // create another oidc broker
        KcOidcBrokerConfiguration oidcBrokerConfig = KcOidcBrokerConfiguration.INSTANCE;
        ClientRepresentation oidcClient = oidcBrokerConfig.createProviderClients().get(0);
        IdentityProviderRepresentation oidcBroker = oidcBrokerConfig.setUpIdentityProvider();
        oidcBroker.setAlias("kc-oidc-idp2");
        oidcBroker.setDisplayName("kc-oidc-idp2");

        try {
            updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
            adminClient.realm(bc.providerRealmName()).clients().create(samlClient);
            adminClient.realm(bc.providerRealmName()).clients().create(oidcClient);
            consumerRealm.identityProviders().create(samlBroker);
            consumerRealm.identityProviders().create(oidcBroker);

            driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));

            logInWithBroker(samlBrokerConfig);
            waitForAccountManagementTitle();
            accountUpdateProfilePage.assertCurrent();
            logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());

            logInWithBroker(bc);

            waitForPage(driver, "account already exists", false);
            assertTrue(idpConfirmLinkPage.isCurrent());
            assertEquals("User with email user@localhost.com already exists. How do you want to continue?", idpConfirmLinkPage.getMessage());
            idpConfirmLinkPage.clickLinkAccount();

            assertEquals("Authenticate to link your account with " + bc.getIDPAlias(), loginPage.getInfoMessage());

            // There have to be two idp showed on login page
            // kc-saml-idp and kc-oidc-idp2 must be present but not kc-oidc-idp
            this.loginPage.findSocialButton(samlBroker.getAlias());
            this.loginPage.findSocialButton(oidcBroker.getAlias());

            try {
                this.loginPage.findSocialButton(bc.getIDPAlias());
                org.junit.Assert.fail("Not expected to see social button with " + bc.getIDPAlias());
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
     * Tests that nested first broker flows are not allowed. The user wants to link federatedIdentity with existing account. He will try link by reauthentication
     * with different broker not linked to his account. Error message should be shown, and reauthentication should be resumed.
     */
    @Test
    public void testNestedFirstBrokerFlow() {
        KcSamlBrokerConfiguration samlBrokerConfig = KcSamlBrokerConfiguration.INSTANCE;
        ClientRepresentation samlClient = samlBrokerConfig.createProviderClients().get(0);
        IdentityProviderRepresentation samlBroker = samlBrokerConfig.setUpIdentityProvider();
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());

        try {
            updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
            adminClient.realm(bc.providerRealmName()).clients().create(samlClient);
            consumerRealm.identityProviders().create(samlBroker);

            driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));

            createUser(bc.getUserLogin());

            logInWithBroker(bc);

            waitForPage(driver, "account already exists", false);
            assertTrue(idpConfirmLinkPage.isCurrent());
            assertEquals("User with email user@localhost.com already exists. How do you want to continue?", idpConfirmLinkPage.getMessage());
            idpConfirmLinkPage.clickLinkAccount();

            assertEquals("Authenticate to link your account with " + bc.getIDPAlias(), loginPage.getInfoMessage());

            try {
                this.loginPage.findSocialButton(bc.getIDPAlias());
                org.junit.Assert.fail("Not expected to see social button with " + samlBrokerConfig.getIDPAlias());
            } catch (NoSuchElementException expected) {
            }

            log.debug("Clicking social " + samlBrokerConfig.getIDPAlias());
            loginPage.clickSocial(samlBrokerConfig.getIDPAlias());
            assertEquals(String.format("The %s user %s is not linked to any known user.", samlBrokerConfig.getIDPAlias(), samlBrokerConfig.getUserLogin()), loginPage.getError());

            assertNumFederatedIdentities(consumerRealm.users().search(samlBrokerConfig.getUserLogin()).get(0).getId(), 0);
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
        ClientRepresentation samlClient = samlBrokerConfig.createProviderClients().get(0);
        IdentityProviderRepresentation samlBroker = samlBrokerConfig.setUpIdentityProvider();
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());

        try {
            updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);
            adminClient.realm(bc.providerRealmName()).clients().create(samlClient);
            consumerRealm.identityProviders().create(samlBroker);

            driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
            logInWithBroker(samlBrokerConfig);
            waitForPage(driver, "update account information", false);
            updateAccountInformationPage.updateAccountInformation("FirstName", "LastName");
            logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());

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

    @Test
    public void testEditUsername() {
        updateExecutions(AbstractBrokerTest::setUpMissingUpdateProfileOnFirstLogin);

        createUser(bc.providerRealmName(), "no-first-name", "password", null, "LastName", "no-first-name@localhost.com");
        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));
        log.debug("Logging in");
        loginPage.login("no-first-name", "password");

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
        updateAccountInformationPage.updateAccountInformation("", "no-first-name@localhost.com", "FirstName", "LastName");
        updateAccountInformationPage.assertCurrent();

        assertEquals("Please specify username.", loginUpdateProfilePage.getInputErrors().getUsernameError());
        
        updateAccountInformationPage.updateAccountInformation("new-username", "no-first-name@localhost.com", "First Name", "Last Name");
        waitForAccountManagementTitle();
        accountUpdateProfilePage.assertCurrent();
        Assert.assertEquals("First Name", accountUpdateProfilePage.getFirstName());
        Assert.assertEquals("Last Name", accountUpdateProfilePage.getLastName());
        Assert.assertEquals("no-first-name@localhost.com", accountUpdateProfilePage.getEmail());
        Assert.assertEquals("new-username", accountUpdateProfilePage.getUsername());

    }
}
