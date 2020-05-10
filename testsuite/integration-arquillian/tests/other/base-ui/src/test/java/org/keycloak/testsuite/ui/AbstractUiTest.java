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
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractAuthTest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assume.assumeFalse;

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

    protected IdentityProviderRepresentation createIdentityProviderRepresentation(String alias, String providerId) {
        IdentityProviderRepresentation idpRep = new IdentityProviderRepresentation();
        idpRep.setProviderId(providerId);
        idpRep.setAlias(alias);
        idpRep.setConfig(new HashMap<>());
        return idpRep;
    }
}
