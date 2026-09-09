/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.forms;

import java.util.Arrays;
import java.util.Collections;

import org.keycloak.authentication.authenticators.access.AllowAccessAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.representations.ClaimsRepresentation;
import org.keycloak.representations.IDToken;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.tests.common.BasicUserConfig;
import org.keycloak.tests.providers.forms.CustomAuthenticationFlowCallback;
import org.keycloak.tests.providers.forms.CustomAuthenticationFlowCallbackFactory;
import org.keycloak.testsuite.util.FlowUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
@KeycloakIntegrationTest(config = AuthenticationFlowCallbackProviderTest.ServerConfig.class)
public class AuthenticationFlowCallbackProviderTest {
    public static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.dependency("org.keycloak.tests", "keycloak-tests-custom-providers");
        }
    }

    protected static final String DEFAULT_FLOW = "newCallbackFlow";

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectUser(config = BasicUserConfig.class)
    ManagedUser user;

    @InjectOAuthClient(lifecycle = LifeCycle.METHOD)
    OAuthClient oauth;

    @InjectRunOnServer(lifecycle = LifeCycle.METHOD)
    RunOnServerClient runOnServer;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    ErrorPage errorPage;

    @BeforeEach
    public void setUpFlow() {
        runOnServer.run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(DEFAULT_FLOW));
        runOnServer.run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(DEFAULT_FLOW)
                .inForms(forms -> forms
                        .clear()
                        .addSubFlowExecution(AuthenticationExecutionModel.Requirement.CONDITIONAL, subflow -> subflow
                                .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, UsernamePasswordFormFactory.PROVIDER_ID)
                                .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, ConditionalLoaAuthenticatorFactory.PROVIDER_ID,
                                        config -> {
                                            config.getConfig().put(ConditionalLoaAuthenticator.LEVEL, "1");
                                            config.getConfig().put(ConditionalLoaAuthenticator.MAX_AGE, String.valueOf(ConditionalLoaAuthenticator.DEFAULT_MAX_AGE));
                                        })
                                .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, AllowAccessAuthenticatorFactory.PROVIDER_ID)
                        )
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, CustomAuthenticationFlowCallbackFactory.PROVIDER_ID)
                )
                .defineAsBrowserFlow()
        );
    }

    @Test
    public void loaEssentialNonExisting() {
        openLoginFormWithAcrClaim(true, "4");

        loginPage.assertCurrent();
        loginPage.fillLogin(user.getUsername(), user.getPassword());
        loginPage.submit();

        errorPage.assertCurrent();
        assertThat(errorPage.getError(), is("Authentication requirements not fulfilled"));
    }

    @Test
    public void errorWithCustomProvider() {
        openLoginFormWithAcrClaim(true, "1");

        loginPage.assertCurrent();
        loginPage.fillLogin(user.getUsername(), user.getPassword());
        loginPage.submit();

        errorPage.assertCurrent();
        assertThat(errorPage.getError(), is(CustomAuthenticationFlowCallback.EXPECTED_ERROR_MESSAGE));
    }

    private void openLoginFormWithAcrClaim(boolean essential, String... acrValues) {
        ClaimsRepresentation.ClaimValue<String> acrClaim = new ClaimsRepresentation.ClaimValue<>();
        acrClaim.setEssential(essential);
        if (essential || acrValues.length > 1) {
            acrClaim.setValues(Arrays.asList(acrValues));
        } else {
            acrClaim.setValue(acrValues[0]);
        }

        ClaimsRepresentation claims = new ClaimsRepresentation();
        claims.setIdTokenClaims(Collections.singletonMap(IDToken.ACR, acrClaim));

        oauth.loginForm().claims(claims).open();
    }
}
