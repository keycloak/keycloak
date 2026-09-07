/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.tests.organization.authentication;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginUsernamePage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.tests.organization.admin.AbstractOrganizationTest;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Regression test for the organization authenticator dropping the typed username when it falls
 * through to the default identity-provider-redirector because no organization matches the email
 * domain. See https://github.com/keycloak/keycloak/issues/52268.
 */
@KeycloakIntegrationTest
public class OrganizationLoginHintFallthroughTest extends AbstractOrganizationTest {

    @InjectRealm(ref = "provider", config = AbstractOrganizationTest.ProviderRealmConf.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm providerRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    LoginUsernamePage loginUsernamePage;

    @Test
    public void testLoginHintPreservedOnFallthroughToDefaultProvider() {
        // an organization exists, but with a domain that will not match the email used below, so
        // resolveOrganization() returns null and the authenticator falls through
        createOrganization(realm, "unrelated-org", "unrelated-org.example");

        String defaultIdpAlias = "default-identity-provider";
        IdentityProviderRepresentation defaultIdp = createRealOrgBroker(defaultIdpAlias, providerRealm);
        // forwarding login_hint to the external authorization URL is opt-in per broker
        // (AbstractOAuth2IdentityProvider#createAuthorizationUrl checks isLoginHint()); the
        // shared createRealOrgBroker helper does not set it, so add it here rather than change
        // a helper other tests share.
        defaultIdp.getConfig().put(IdentityProviderModel.LOGIN_HINT, "true");
        realm.admin().identityProviders().create(defaultIdp).close();
        realm.cleanup().add(r -> r.identityProviders().get(defaultIdpAlias).remove());

        configureBrowserFlowWithDefaultProviderAfterOrganization(defaultIdpAlias);

        String typedUsername = "alice@other-domain.example";

        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(typedUsername);
        loginUsernamePage.submit();

        // the fall-through should have redirected to the default provider (the provider realm)
        assertThat("Should have fallen through to the default provider",
                driver.getCurrentUrl(), Matchers.containsString("/realms/" + providerRealm.getName() + "/"));

        // the provider realm's login form is the plain combined username+password form (the
        // provider realm has no organizations / identity-first split configured), so login_hint
        // pre-fills the username input's value directly rather than the identity-first
        // "attempted username" banner; getUsername() reads that input, not getAttemptedUsername()
        assertThat(loginPage.getUsername(), Matchers.equalTo(typedUsername));
    }

    /**
     * The default browser flow runs identity-provider-redirector (priority 25) before the
     * organization sub-flow (priority 26), so a fall-through from organization never reaches it.
     * Copy the flow (raise/lowerPriority reject built-in flows outright) and add a second
     * identity-provider-redirector at priority 27, between organization (26) and forms (30), so a
     * fall-through from organization is the next alternative tried. The original, unconfigured
     * redirector at 25 is left in the copy untouched: with no default provider it just calls
     * context.attempted() immediately, so it does not interfere.
     */
    private void configureBrowserFlowWithDefaultProviderAfterOrganization(String defaultProviderAlias) {
        String newFlowAlias = "browser - login hint fallthrough test";
        runOnServer.run(session -> FlowUtil.inCurrentRealm(session)
                .copyBrowserFlow(newFlowAlias)
                .addAuthenticatorExecution(Requirement.ALTERNATIVE, "identity-provider-redirector", 27,
                        config -> config.getConfig().put("defaultProvider", defaultProviderAlias))
                .defineAsBrowserFlow());
    }
}
