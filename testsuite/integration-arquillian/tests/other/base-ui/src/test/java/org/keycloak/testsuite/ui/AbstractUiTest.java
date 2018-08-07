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

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractAuthTest;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public abstract class AbstractUiTest extends AbstractAuthTest {
    public static final String LOCALIZED_THEME = "localized-theme";
    public static final String CUSTOM_LOCALE_NAME = "Přísný jazyk";

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealmRep = new RealmRepresentation();
        testRealmRep.setId(TEST);
        testRealmRep.setRealm(TEST);
        testRealmRep.setEnabled(true);
        configureInternationalizationForRealm(testRealmRep);
        testRealms.add(testRealmRep);
    }

    protected void configureInternationalizationForRealm(RealmRepresentation realm) {
        // fetch the supported locales for the special test theme that includes some fake test locales
        Set<String> supportedLocales = adminClient.serverInfo().getInfo().getThemes().get("login").stream()
                .filter(x -> x.getName().equals(LOCALIZED_THEME))
                .flatMap(x -> Arrays.stream(x.getLocales()))
                .collect(Collectors.toSet());

        realm.setInternationalizationEnabled(true);
        realm.setSupportedLocales(supportedLocales);
        realm.setLoginTheme(LOCALIZED_THEME);
        realm.setAdminTheme(LOCALIZED_THEME);
        realm.setAccountTheme(LOCALIZED_THEME);
        realm.setEmailTheme(LOCALIZED_THEME);
    }
}
