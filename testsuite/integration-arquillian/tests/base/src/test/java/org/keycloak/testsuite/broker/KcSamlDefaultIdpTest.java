package org.keycloak.testsuite.broker;

import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authentication.authenticators.browser.IdentityProviderAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.UIUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;

import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;
import java.util.UUID;

/**
 * Test of various scenarios related to the use of default IdP option
 * in the Identity Provider Redirector authenticator
 */
public class KcSamlDefaultIdpTest extends AbstractInitializedBaseBrokerTest {

    @Override
    public void beforeBrokerTest() {
        super.beforeBrokerTest();
        // Require broker to show consent screen
        RealmResource brokeredRealm = adminClient.realm(bc.providerRealmName());
        List<ClientRepresentation> clients = brokeredRealm.clients().findByClientId(bc.getIDPClientIdInProviderRealm());
        org.junit.Assert.assertEquals(1, clients.size());
        ClientRepresentation brokerApp = clients.get(0);
        brokerApp.setConsentRequired(true);
        brokeredRealm.clients().get(brokerApp.getId()).update(brokerApp);
    }

    @Test
    public void testDefaultIdpNotSet() {
        // Set the Default Identity Provider option for the Identity Provider Redirector to null
        configureFlow(null);

        // Navigate to the auth page
        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        waitForPage(driver, "sign in to", true);

        Assert.assertTrue("Driver should be on the initial page and nothing should have happened",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));
    }

    @Test
    public void testDefaultIdpSet() {
        // Set the Default Identity Provider option to the remote IdP name
        configureFlow("kc-saml-idp");

        String username = "all-info-set@localhost.com";
        createUser(bc.providerRealmName(), username, "password", "FirstName");

        // Navigate to the auth page
        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        waitForPage(driver, "sign in to", true);

        // Make sure we got redirected to the remote IdP automatically
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));
    }

    // KEYCLOAK-17368
    @Test
    public void testDefaultIdpSetTriedAndReturnedError() {
        // Set the Default Identity Provider option to the remote IdP name
        configureFlow("kc-saml-idp");

        String username = "all-info-set@localhost.com";
        createUser(bc.providerRealmName(), username, "password", "FirstName");

        // Navigate to the auth page
        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        waitForPage(driver, "sign in to", true);

        // Make sure we got redirected to the remote IdP automatically
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));

        // Attempt login
        log.debug("Logging in");
        loginPage.login(bc.getUserLogin(), bc.getUserPassword());

        // Deny user consent
        grantPage.assertCurrent();
        grantPage.cancel();

        waitForPage(driver, "sign in to", true);

        WebElement errorElement = driver.findElement(By.className("alert-error"));
        assertNotNull("Page should show an error message but it's missing", errorElement);

        assertEquals("Unexpected error when authenticating with identity provider", UIUtils.getTextFromElement(errorElement));
    }

    private void configureFlow(String defaultIdpValue)
    {
        String newFlowAlias;

        HashMap<String, String> defaultIdpConfig = new HashMap<String, String>();
        if (defaultIdpValue != null && !defaultIdpValue.isEmpty())
        {
            defaultIdpConfig.put(IdentityProviderAuthenticatorFactory.DEFAULT_PROVIDER, defaultIdpValue);
            newFlowAlias = "Browser - Default IdP " + defaultIdpValue;
        }
        else
            newFlowAlias = "Browser - Default IdP OFF";

        testingClient.server("consumer").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server("consumer").run(session ->
            {
                List<AuthenticationExecutionModel> executions = FlowUtil.inCurrentRealm(session)
                    .selectFlow(newFlowAlias)
                    .getExecutions();

                int index = IntStream.range(0, executions.size())
                    .filter(t -> IdentityProviderAuthenticatorFactory.PROVIDER_ID.equals(executions.get(t).getAuthenticator()))
                    .findFirst()
                    .orElse(-1);

                assertTrue("Identity Provider Redirector execution not found", index >= 0);

                FlowUtil.inCurrentRealm(session)
                    .selectFlow(newFlowAlias)
                    .updateExecution(index,
                        config -> {
                            AuthenticatorConfigModel authConfig = new AuthenticatorConfigModel();
                            authConfig.setId(UUID.randomUUID().toString());
                            authConfig.setAlias("cfg" + authConfig.getId().hashCode());
                            authConfig.setConfig(defaultIdpConfig);

                            session.getContext().getRealm().addAuthenticatorConfig(authConfig);

                            config.setAuthenticatorConfig(authConfig.getId());
                        }
                    )
                .defineAsBrowserFlow();
            }
        );
    }

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcSamlBrokerConfiguration();
    }
}
