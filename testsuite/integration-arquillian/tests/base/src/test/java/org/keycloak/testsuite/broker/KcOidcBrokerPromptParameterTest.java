package org.keycloak.testsuite.broker;

import java.util.List;
import java.util.Map;

import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.UserRepresentation;

import org.junit.jupiter.api.Assertions;

import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

public class KcOidcBrokerPromptParameterTest extends AbstractBrokerTest {

    private static final String PROMPT_CONSENT = "consent";
    private static final String PROMPT_LOGIN = "login";

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration2();
    }


    @Override
    protected void loginUser() {
        oauth.client("broker-app");
        loginPage.open(bc.consumerRealmName());

        driver.navigate().to(driver.getCurrentUrl() + "&" + OIDCLoginProtocol.PROMPT_PARAM + "=" + PROMPT_CONSENT);

        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());

        waitForPage(driver, "sign in to", true);

        Assertions.assertTrue(driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"),
                "Driver should be on the provider realm page right now");

        Assertions.assertFalse(driver.getCurrentUrl().contains(OIDCLoginProtocol.PROMPT_PARAM + "=" + PROMPT_LOGIN),
                OIDCLoginProtocol.PROMPT_PARAM + "=" + PROMPT_LOGIN + " should not be part of the url");

        Assertions.assertTrue(driver.getCurrentUrl().contains(OIDCLoginProtocol.PROMPT_PARAM + "=" + PROMPT_CONSENT),
                OIDCLoginProtocol.PROMPT_PARAM + "=" + PROMPT_CONSENT + " should be part of the url");

        log.debug("Logging in");
        loginPage.login(bc.getUserLogin(), bc.getUserPassword());

        waitForPage(driver, "update account information", false);

        updateAccountInformationPage.assertCurrent();
        Assertions.assertTrue(driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"),
                "We must be on correct realm right now");


        log.debug("Updating info on updateAccount page");
        updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail(), "Firstname", "Lastname");

        UsersResource consumerUsers = adminClient.realm(bc.consumerRealmName()).users();

        int userCount = consumerUsers.count();
        Assertions.assertTrue(userCount > 0, "There must be at least one user");

        List<UserRepresentation> users = consumerUsers.search("", 0, userCount);

        boolean isUserFound = false;
        for (UserRepresentation user : users) {
            if (user.getUsername().equals(bc.getUserLogin()) && user.getEmail().equals(bc.getUserEmail())) {
                isUserFound = true;
                break;
            }
        }

        Assertions.assertTrue(isUserFound,
                "There must be user " + bc.getUserLogin() + " in realm " + bc.consumerRealmName());
    }

    private class KcOidcBrokerConfiguration2 extends KcOidcBrokerConfiguration {
        @Override
        protected void applyDefaultConfiguration(final Map<String, String> config, IdentityProviderSyncMode syncMode) {
            super.applyDefaultConfiguration(config, syncMode);
            config.remove("prompt");
        }
    }
}
