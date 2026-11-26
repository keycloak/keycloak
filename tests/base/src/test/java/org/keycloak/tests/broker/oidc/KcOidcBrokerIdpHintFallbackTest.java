package org.keycloak.tests.broker.oidc;

import java.util.List;
import java.util.stream.IntStream;

import org.keycloak.authentication.authenticators.browser.IdentityProviderAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;
import org.keycloak.testsuite.util.FlowUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the "Fall back to default provider" option of the Identity Provider Redirector, which redirects to the default
 * identity provider when {@code kc_idp_hint} references an identity provider that does not exist or is disabled.
 */
@KeycloakIntegrationTest
public class KcOidcBrokerIdpHintFallbackTest extends AbstractKcOidcBrokerTest {

    private static final String FLOW_ALIAS = "browser-idp-redirector-fallback";
    private static final String DISABLED_IDP_ALIAS = "disabled-idp";
    private static final String OTHER_IDP_ALIAS = "other-idp";

    @InjectRunOnServer(realmRef = "consumer")
    RunOnServerClient runOnServer;

    @Test
    public void testUnknownHintFallsBackToDefaultProvider() {
        configureIdpRedirector(IDP_OIDC_ALIAS, true);

        oauth.loginForm().param("kc_idp_hint", "bogus-idp").open();

        assertOnRealmPage(PROVIDER_REALM);
    }

    @Test
    public void testDisabledHintFallsBackToDefaultProvider() {
        copyIdentityProvider(DISABLED_IDP_ALIAS, false);
        configureIdpRedirector(IDP_OIDC_ALIAS, true);

        oauth.loginForm().param("kc_idp_hint", DISABLED_IDP_ALIAS).open();

        assertOnRealmPage(PROVIDER_REALM);
    }

    @Test
    public void testEnabledHintWinsOverDefaultProviderWithFallback() {
        copyIdentityProvider(OTHER_IDP_ALIAS, true);
        configureIdpRedirector(OTHER_IDP_ALIAS, true);

        oauth.loginForm().param("kc_idp_hint", IDP_OIDC_ALIAS).open();

        assertOnRealmPage(PROVIDER_REALM);
        // the provider's login URL carries the consumer broker endpoint of the selected identity provider as redirect_uri
        String currentUrl = webDriver.getCurrentUrl();
        assertTrue(currentUrl.contains("broker%2F" + IDP_OIDC_ALIAS + "%2Fendpoint"),
                "Hinted identity provider should be selected, but URL was " + currentUrl);
        assertFalse(currentUrl.contains("broker%2F" + OTHER_IDP_ALIAS + "%2Fendpoint"),
                "Default identity provider should not be selected, but URL was " + currentUrl);
    }

    @Test
    public void testUnknownHintWithoutFallbackSkipsRedirector() {
        configureIdpRedirector(IDP_OIDC_ALIAS, false);

        oauth.loginForm().param("kc_idp_hint", "bogus-idp").open();

        assertOnRealmPage(CONSUMER_REALM);
    }

    @Test
    public void testEmptyHintSkipsRedirectorEvenWithFallback() {
        configureIdpRedirector(IDP_OIDC_ALIAS, true);

        oauth.loginForm().param("kc_idp_hint", "").open();

        assertOnRealmPage(CONSUMER_REALM);
    }

    private void assertOnRealmPage(String realm) {
        webDriver.waiting().until(d -> webDriver.getCurrentUrl().contains("/realms/" + realm + "/"));
        assertTrue(webDriver.getCurrentUrl().contains("/realms/" + realm + "/"),
                "Driver should be on the " + realm + " realm page");
    }

    /**
     * Creates a copy of {@link #IDP_OIDC_ALIAS} with the given alias in the consumer realm.
     */
    private void copyIdentityProvider(String alias, boolean enabled) {
        runOnServer.run(session -> {
            IdentityProviderModel copy = new IdentityProviderModel(session.identityProviders().getByAlias(IDP_OIDC_ALIAS));
            copy.setInternalId(null);
            copy.setAlias(alias);
            copy.setEnabled(enabled);
            session.identityProviders().create(copy);
        });
    }

    /**
     * Uses a copy of the browser flow whose Identity Provider Redirector has the given default provider.
     */
    private void configureIdpRedirector(String defaultProvider, boolean fallbackToDefaultProvider) {
        runOnServer.run(session -> {
            FlowUtil.inCurrentRealm(session).copyBrowserFlow(FLOW_ALIAS);

            List<AuthenticationExecutionModel> executions = FlowUtil.inCurrentRealm(session).selectFlow(FLOW_ALIAS).getExecutions();
            int index = IntStream.range(0, executions.size())
                    .filter(i -> IdentityProviderAuthenticatorFactory.PROVIDER_ID.equals(executions.get(i).getAuthenticator()))
                    .findFirst()
                    .orElseThrow();

            FlowUtil.inCurrentRealm(session)
                    .selectFlow(FLOW_ALIAS)
                    .updateExecution(index, execution -> {
                        AuthenticatorConfigModel config = new AuthenticatorConfigModel();
                        config.setAlias("idp-redirector-fallback-config");
                        config.getConfig().put(IdentityProviderAuthenticatorFactory.DEFAULT_PROVIDER, defaultProvider);
                        config.getConfig().put(IdentityProviderAuthenticatorFactory.FALLBACK_TO_DEFAULT_PROVIDER, String.valueOf(fallbackToDefaultProvider));
                        config = session.getContext().getRealm().addAuthenticatorConfig(config);
                        execution.setAuthenticatorConfig(config.getId());
                    })
                    .defineAsBrowserFlow();
        });
    }
}
