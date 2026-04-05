package org.keycloak.testsuite.broker;

import java.util.List;
import java.util.Map;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.updaters.Creator;
import org.keycloak.testsuite.util.UserBuilder;

import org.junit.Test;

import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_ALIAS;
import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_PROVIDER_ID;
import static org.keycloak.testsuite.broker.BrokerTestConstants.USER_EMAIL;
import static org.keycloak.testsuite.broker.BrokerTestTools.createIdentityProvider;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;

import static org.junit.Assert.assertTrue;

public class KcOidcBrokerLoginHintTest extends AbstractBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfigurationWithLoginHint();
    }
    
    private class KcOidcBrokerConfigurationWithLoginHint extends KcOidcBrokerConfiguration {
        
        @Override
        public IdentityProviderRepresentation setUpIdentityProvider(IdentityProviderSyncMode syncMode) {
            IdentityProviderRepresentation idp = createIdentityProvider(IDP_OIDC_ALIAS, IDP_OIDC_PROVIDER_ID);

            Map<String, String> config = idp.getConfig();
            applyDefaultConfiguration(config, syncMode);
            config.put(IdentityProviderModel.LOGIN_HINT, "true");
            return idp;
        }
    }

    @Override
    protected void loginUser() {
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        
        driver.navigate().to(driver.getCurrentUrl() + "&login_hint=" + USER_EMAIL);

        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());

        waitForPage(driver, "sign in to", true);

        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));

        Assert.assertTrue("User identifiant should be fullfilled",
                loginPage.getUsername().equalsIgnoreCase(USER_EMAIL));
        
        log.debug("Logging in");
        loginPage.login(bc.getUserPassword());

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

    @Test
    public void loginHintWithExistingUser() {
        try (Creator<UserResource> c = Creator.create(adminClient.realm(bc.consumerRealmName()),
                UserBuilder.create()
                        .username(bc.getUserLogin())
                        .password(bc.getUserPassword())
                        .email(bc.getUserEmail())
                        .enabled(true)
                        .build()
            )) {
            oauth.clientId("broker-app");
            loginPage.open(bc.consumerRealmName());
            waitForPageToLoad();
            driver.navigate().to(driver.getCurrentUrl() + "&login_hint=" + USER_EMAIL + "&kc_idp_hint=" + IDP_OIDC_ALIAS);
            waitForPageToLoad();

            loginPage.login(bc.getUserPassword());

            updateAccountInformationPage.assertCurrent();
            updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail(), "Firstname", "Lastname");

            idpConfirmLinkPage.assertCurrent();
            idpConfirmLinkPage.clickLinkAccount();

            loginPage.login(bc.getUserPassword());
            assertTrue("Test user should be successfully logged in.", driver.getTitle().contains("AUTH_RESPONSE"));
        }
    }
}
