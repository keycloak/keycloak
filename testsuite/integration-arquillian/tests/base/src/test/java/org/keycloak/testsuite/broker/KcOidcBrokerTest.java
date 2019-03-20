package org.keycloak.testsuite.broker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.admin.ApiUtil.removeUserByUsername;
import static org.keycloak.testsuite.broker.BrokerRunOnServerUtil.configureAutoLinkFlow;
import static org.keycloak.testsuite.broker.BrokerRunOnServerUtil.configurePostBrokerLoginWithOTP;
import static org.keycloak.testsuite.broker.BrokerTestConstants.REALM_PROV_NAME;
import static org.keycloak.testsuite.broker.BrokerTestTools.getAuthRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createHardcodedClaim;

import java.util.List;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.mappers.ExternalKeycloakRoleToRoleMapper;
import org.keycloak.broker.oidc.mappers.UserAttributeMapper;
import org.keycloak.crypto.Algorithm;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.openqa.selenium.NoSuchElementException;

public class KcOidcBrokerTest extends AbstractBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcOidcBrokerConfiguration.INSTANCE;
    }

    @Override
    protected Iterable<IdentityProviderMapperRepresentation> createIdentityProviderMappers() {
        IdentityProviderMapperRepresentation attrMapper1 = new IdentityProviderMapperRepresentation();
        attrMapper1.setName("manager-role-mapper");
        attrMapper1.setIdentityProviderMapper(ExternalKeycloakRoleToRoleMapper.PROVIDER_ID);
        attrMapper1.setConfig(ImmutableMap.<String,String>builder()
                .put("external.role", ROLE_MANAGER)
                .put("role", ROLE_MANAGER)
                .build());

        IdentityProviderMapperRepresentation attrMapper2 = new IdentityProviderMapperRepresentation();
        attrMapper2.setName("user-role-mapper");
        attrMapper2.setIdentityProviderMapper(ExternalKeycloakRoleToRoleMapper.PROVIDER_ID);
        attrMapper2.setConfig(ImmutableMap.<String,String>builder()
                .put("external.role", ROLE_USER)
                .put("role", ROLE_USER)
                .build());

        return Lists.newArrayList(attrMapper1, attrMapper2);
    }

    @Test
    public void loginFetchingUserFromUserEndpoint() {
        RealmResource realm = realmsResouce().realm(bc.providerRealmName());
        ClientsResource clients = realm.clients();
        ClientRepresentation brokerApp = clients.findByClientId("brokerapp").get(0);

        try {
            IdentityProviderResource identityProviderResource = realmsResouce().realm(bc.consumerRealmName()).identityProviders().get(bc.getIDPAlias());
            IdentityProviderRepresentation idp = identityProviderResource.toRepresentation();

            idp.getConfig().put(OIDCIdentityProviderConfig.JWKS_URL, getAuthRoot(suiteContext) + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/certs");
            identityProviderResource.update(idp);

            brokerApp.getAttributes().put(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG, Algorithm.RS256);
            brokerApp.getAttributes().put("validateSignature", Boolean.TRUE.toString());
            clients.get(brokerApp.getId()).update(brokerApp);

            driver.navigate().to(getAccountUrl(bc.consumerRealmName()));
            logInWithBroker(bc);

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
        } finally {
            brokerApp.getAttributes().put(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG, null);
            brokerApp.getAttributes().put("validateSignature", Boolean.FALSE.toString());
            clients.get(brokerApp.getId()).update(brokerApp);
        }
    }

    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.OIDCBrokerUserPropertyTest
     */
    @Test
    public void loginFetchingUserFromUserEndpointWithClaimMapper() {
        RealmResource realm = realmsResouce().realm(bc.providerRealmName());
        ClientsResource clients = realm.clients();
        ClientRepresentation brokerApp = clients.findByClientId("brokerapp").get(0);
        IdentityProviderResource identityProviderResource = getIdentityProviderResource();

        clients.get(brokerApp.getId()).getProtocolMappers().createMapper(createHardcodedClaim("hard-coded", "hard-coded", "hard-coded", "String", true, true)).close();

        IdentityProviderMapperRepresentation hardCodedSessionNoteMapper = new IdentityProviderMapperRepresentation();

        hardCodedSessionNoteMapper.setName("hard-coded");
        hardCodedSessionNoteMapper.setIdentityProviderAlias(bc.getIDPAlias());
        hardCodedSessionNoteMapper.setIdentityProviderMapper(UserAttributeMapper.PROVIDER_ID);
        hardCodedSessionNoteMapper.setConfig(ImmutableMap.<String, String>builder()
                .put(UserAttributeMapper.USER_ATTRIBUTE, "hard-coded")
                .put(UserAttributeMapper.CLAIM, "hard-coded")
                .build());

        identityProviderResource.addMapper(hardCodedSessionNoteMapper).close();

        loginFetchingUserFromUserEndpoint();

        UserRepresentation user = getFederatedIdentity();

        Assert.assertEquals(1, user.getAttributes().size());
        Assert.assertEquals("hard-coded", user.getAttributes().get("hard-coded").get(0));
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
            waitForPage(driver, "keycloak account management", true);
            accountUpdateProfilePage.assertCurrent();
            logoutFromRealm(bc.consumerRealmName());

            logInWithBroker(bc);

            waitForPage(driver, "account already exists", false);
            assertTrue(idpConfirmLinkPage.isCurrent());
            assertEquals("User with email user@localhost.com already exists. How do you want to continue?", idpConfirmLinkPage.getMessage());
            idpConfirmLinkPage.clickLinkAccount();

            assertEquals("Authenticate as testuser to link your account with " + bc.getIDPAlias(), loginPage.feedbackMessage().getText());

            try {
                this.accountLoginPage.findSocialButton(bc.getIDPAlias());
                org.junit.Assert.fail("Not expected to see social button with " + samlBrokerConfig.getIDPAlias());
            } catch (NoSuchElementException expected) {
            }

            log.debug("Clicking social " + samlBrokerConfig.getIDPAlias());
            accountLoginPage.clickSocial(samlBrokerConfig.getIDPAlias());
            waitForPage(driver, "keycloak account management", true);
            accountUpdateProfilePage.assertCurrent();

            assertNumFederatedIdentities(consumerRealm.users().search(samlBrokerConfig.getUserLogin()).get(0).getId(), 2);
        } finally {
            updateExecutions(AbstractBrokerTest::setUpMissingUpdateProfileOnFirstLogin);
            removeUserByUsername(consumerRealm, "consumer");
        }
    }

    /**
     * Tests that user can link federated identity with existing brokered
     * account without prompt (KEYCLOAK-7270).
     */
    @Test
    public void testAutoLinkAccountWithBroker() {
        testingClient.server(bc.consumerRealmName()).run(configureAutoLinkFlow(bc.getIDPAlias()));

        driver.navigate().to(getAccountUrl(bc.consumerRealmName()));
        logInWithBroker(bc);

        RealmResource realm = adminClient.realm(bc.consumerRealmName());

        assertNumFederatedIdentities(realm.users().search(bc.getUserLogin()).get(0).getId(), 1);
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
            accountLoginPage.clickSocial(samlBrokerConfig.getIDPAlias());
            waitForPage(driver, "keycloak account management", true);
            accountUpdateProfilePage.assertCurrent();

            assertNumFederatedIdentities(consumerRealm.users().search(samlBrokerConfig.getUserLogin()).get(0).getId(), 1);
        } finally {
            updateExecutions(AbstractBrokerTest::setUpMissingUpdateProfileOnFirstLogin);
            removeUserByUsername(consumerRealm, "consumer");
        }
    }

    /**
     * Refers to in old test suite: PostBrokerFlowTest#testBrokerReauthentication_samlBrokerWithOTPRequired
     */
    @Test
    public void testReauthenticationSamlBrokerWithOTPRequired() throws Exception {
        KcSamlBrokerConfiguration samlBrokerConfig = KcSamlBrokerConfiguration.INSTANCE;
        ClientRepresentation samlClient = samlBrokerConfig.createProviderClients(suiteContext).get(0);
        IdentityProviderRepresentation samlBroker = samlBrokerConfig.setUpIdentityProvider(suiteContext);
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());

        try {
            updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
            adminClient.realm(bc.providerRealmName()).clients().create(samlClient);
            consumerRealm.identityProviders().create(samlBroker);

            driver.navigate().to(getAccountUrl(bc.consumerRealmName()));
            testingClient.server(bc.consumerRealmName()).run(configurePostBrokerLoginWithOTP(samlBrokerConfig.getIDPAlias()));
            logInWithBroker(samlBrokerConfig);

            totpPage.assertCurrent();
            String totpSecret = totpPage.getTotpSecret();
            totpPage.configure(totp.generateTOTP(totpSecret));
            logoutFromRealm(bc.consumerRealmName());

            logInWithBroker(bc);

            waitForPage(driver, "account already exists", false);
            idpConfirmLinkPage.assertCurrent();
            idpConfirmLinkPage.clickLinkAccount();

            accountLoginPage.clickSocial(samlBrokerConfig.getIDPAlias());
            waitForPage(driver, "log in to", true);
            log.debug("Logging in");
            loginTotpPage.login(totp.generateTOTP(totpSecret));

            assertNumFederatedIdentities(consumerRealm.users().search(samlBrokerConfig.getUserLogin()).get(0).getId(), 2);
        } finally {
            updateExecutions(AbstractBrokerTest::setUpMissingUpdateProfileOnFirstLogin);
            removeUserByUsername(consumerRealm, "consumer");
        }
    }

    /**
     * Refers to in old test suite: PostBrokerFlowTest#testBrokerReauthentication_oidcBrokerWithOTPRequired
     */
    @Test
    public void testReauthenticationOIDCBrokerWithOTPRequired() throws Exception {
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
            logoutFromRealm(bc.consumerRealmName());

            testingClient.server(bc.consumerRealmName()).run(configurePostBrokerLoginWithOTP(bc.getIDPAlias()));
            logInWithBroker(bc);

            waitForPage(driver, "account already exists", false);
            idpConfirmLinkPage.assertCurrent();
            idpConfirmLinkPage.clickLinkAccount();
            logoutFromRealm(bc.providerRealmName());

            driver.navigate().back();
            logInWithBroker(samlBrokerConfig);

            totpPage.assertCurrent();
            String totpSecret = totpPage.getTotpSecret();
            totpPage.configure(totp.generateTOTP(totpSecret));
            logoutFromRealm(bc.consumerRealmName());

            assertNumFederatedIdentities(consumerRealm.users().search(samlBrokerConfig.getUserLogin()).get(0).getId(), 2);
        } finally {
            updateExecutions(AbstractBrokerTest::setUpMissingUpdateProfileOnFirstLogin);
            removeUserByUsername(consumerRealm, "consumer");
        }
    }

    /**
     * Refers to in old test suite: PostBrokerFlowTest#testBrokerReauthentication_bothBrokerWithOTPRequired
     */
    @Test
    public void testReauthenticationBothBrokersWithOTPRequired() throws Exception {
        KcSamlBrokerConfiguration samlBrokerConfig = KcSamlBrokerConfiguration.INSTANCE;
        ClientRepresentation samlClient = samlBrokerConfig.createProviderClients(suiteContext).get(0);
        IdentityProviderRepresentation samlBroker = samlBrokerConfig.setUpIdentityProvider(suiteContext);
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());

        try {
            updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
            adminClient.realm(bc.providerRealmName()).clients().create(samlClient);
            consumerRealm.identityProviders().create(samlBroker);

            driver.navigate().to(getAccountUrl(bc.consumerRealmName()));
            testingClient.server(bc.consumerRealmName()).run(configurePostBrokerLoginWithOTP(samlBrokerConfig.getIDPAlias()));
            logInWithBroker(samlBrokerConfig);
            totpPage.assertCurrent();
            String totpSecret = totpPage.getTotpSecret();
            totpPage.configure(totp.generateTOTP(totpSecret));
            logoutFromRealm(bc.consumerRealmName());

            testingClient.server(bc.consumerRealmName()).run(configurePostBrokerLoginWithOTP(bc.getIDPAlias()));
            logInWithBroker(bc);

            waitForPage(driver, "account already exists", false);
            idpConfirmLinkPage.assertCurrent();
            idpConfirmLinkPage.clickLinkAccount();
            logoutFromRealm(bc.providerRealmName());

            driver.navigate().back();
            logInWithBroker(samlBrokerConfig);

            loginTotpPage.assertCurrent();
            loginTotpPage.login(totp.generateTOTP(totpSecret));
            logoutFromRealm(bc.providerRealmName());
            logoutFromRealm(bc.consumerRealmName());

            logInWithBroker(bc);

            loginTotpPage.assertCurrent();
            loginTotpPage.login(totp.generateTOTP(totpSecret));
            waitForPage(driver, "keycloak account management", true);
            accountUpdateProfilePage.assertCurrent();

            assertNumFederatedIdentities(consumerRealm.users().search(samlBrokerConfig.getUserLogin()).get(0).getId(), 2);
        } finally {
            updateExecutions(AbstractBrokerTest::setUpMissingUpdateProfileOnFirstLogin);
            removeUserByUsername(consumerRealm, "consumer");
        }
    }

    private UserRepresentation getFederatedIdentity() {
        List<UserRepresentation> users = realmsResouce().realm(bc.consumerRealmName()).users().search(bc.getUserLogin());

        Assert.assertEquals(1, users.size());

        return users.get(0);
    }

    private IdentityProviderResource getIdentityProviderResource() {
        return realmsResouce().realm(bc.consumerRealmName()).identityProviders().get(bc.getIDPAlias());
    }
}
