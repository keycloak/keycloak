/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.ui;

import org.junit.Before;
import org.junit.BeforeClass;
import org.keycloak.authentication.authenticators.browser.WebAuthnAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.WebAuthnPasswordlessAuthenticatorFactory;
import org.keycloak.authentication.requiredactions.WebAuthnPasswordlessRegisterFactory;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderSimpleRepresentation;
import org.keycloak.testsuite.AbstractAuthTest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assume.assumeFalse;
import static org.keycloak.models.AuthenticationExecutionModel.Requirement.REQUIRED;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public abstract class AbstractUiTest extends AbstractAuthTest {
    public static final String LOCALIZED_THEME = "localized-theme";
    public static final String LOCALIZED_THEME_PREVIEW = "localized-theme-preview";
    public static final String CUSTOM_LOCALE = "test";
    public static final String CUSTOM_LOCALE_NAME = "Přísný jazyk";
    public static final String DEFAULT_LOCALE="en";
    public static final String DEFAULT_LOCALE_NAME = "English";
    public static final String LOCALE_CLIENT_NAME = "${client_localized-client}";
    public static final String LOCALE_CLIENT_NAME_LOCALIZED = "Přespříliš lokalizovaný klient";
    public static final String WEBAUTHN_FLOW_ID = "75e2390e-f296-49e6-acf8-6d21071d7e10";

    @BeforeClass
    public static void assumeSupportedBrowser() {
        assumeFalse("Browser must not be htmlunit", System.getProperty("browser").equals("htmlUnit"));
        assumeFalse("Browser must not be PhantomJS", System.getProperty("browser").equals("phantomjs"));
    }

    @Before
    public void addTestUser() {
        createTestUserWithAdminClient(false);
    }

    protected boolean isAccountPreviewTheme() {
        return false;
    }

    protected void configureInternationalizationForRealm(RealmRepresentation realm) {
        final String localizedTheme = isAccountPreviewTheme() ? LOCALIZED_THEME_PREVIEW : LOCALIZED_THEME;

        // fetch the supported locales for the special test theme that includes some fake test locales
        Set<String> supportedLocales = adminClient.serverInfo().getInfo().getThemes().get("login").stream()
                .filter(x -> x.getName().equals(LOCALIZED_THEME))
                .flatMap(x -> Arrays.stream(x.getLocales()))
                .collect(Collectors.toSet());

        realm.setInternationalizationEnabled(true);
        realm.setSupportedLocales(supportedLocales);
        realm.setLoginTheme(LOCALIZED_THEME);
        realm.setAdminTheme(LOCALIZED_THEME);
        realm.setAccountTheme(localizedTheme);
        realm.setEmailTheme(LOCALIZED_THEME);
    }

    protected void configureWebAuthNForRealm() {
        // configure WebAuthn
        // we can't do this during the realm import because we'd need to specify all built-in flows as well

        AuthenticationFlowRepresentation flow = new AuthenticationFlowRepresentation();
        flow.setId(WEBAUTHN_FLOW_ID);
        flow.setAlias("webauthn flow");
        flow.setProviderId("basic-flow");
        flow.setBuiltIn(false);
        flow.setTopLevel(true);
        testRealmResource().flows().createFlow(flow);

        AuthenticationExecutionRepresentation execution = new AuthenticationExecutionRepresentation();
        execution.setAuthenticator(WebAuthnAuthenticatorFactory.PROVIDER_ID);
        execution.setPriority(10);
        execution.setRequirement(REQUIRED.toString());
        execution.setParentFlow(WEBAUTHN_FLOW_ID);
        testRealmResource().flows().addExecution(execution);

        execution.setAuthenticator(WebAuthnPasswordlessAuthenticatorFactory.PROVIDER_ID);
        testRealmResource().flows().addExecution(execution);

        RequiredActionProviderSimpleRepresentation requiredAction = new RequiredActionProviderSimpleRepresentation();
        requiredAction.setProviderId(WebAuthnRegisterFactory.PROVIDER_ID);
        requiredAction.setName("blahblah");
        testRealmResource().flows().registerRequiredAction(requiredAction);

        requiredAction.setProviderId(WebAuthnPasswordlessRegisterFactory.PROVIDER_ID);
        testRealmResource().flows().registerRequiredAction(requiredAction);

        // no need to actually configure the authentication, in Account Console tests we just verify the registration

    }

    protected IdentityProviderRepresentation createIdentityProviderRepresentation(String alias, String providerId) {
        IdentityProviderRepresentation idpRep = new IdentityProviderRepresentation();
        idpRep.setProviderId(providerId);
        idpRep.setAlias(alias);
        idpRep.setConfig(new HashMap<>());
        return idpRep;
    }
}
