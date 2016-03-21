/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.console.realm;

import org.apache.commons.configuration.ConfigurationException;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.testsuite.console.page.realm.ThemeSettings;

/**
 *
 * @author Filip Kiss
 * @author tkyjovsk
 */
public class ThemeSettingsTest extends AbstractRealmTest {

    @Page
    private ThemeSettings themeSettingsPage;

    @Before
    public void beforeThemeTest() {
//        configure().realmSettings();
//        tabs().themes();
        themeSettingsPage.navigateTo();
    }

    @Test
    public void changeLoginThemeTest() throws ConfigurationException {
        testRealmLoginPage.setKeycloakThemeCssName(getConstantsProperties().getString("theme-default-css-name"));

        themeSettingsPage.changeLoginTheme("base");
        themeSettingsPage.saveTheme();

        testRealmAdminConsolePage.navigateTo();
        testRealmLoginPage.waitForKeycloakThemeNotPresent();
        testRealmLoginPage.form().login(testUser);
        testRealmAdminConsolePage.logOut();

        themeSettingsPage.navigateTo();
        themeSettingsPage.changeLoginTheme(getConstantsProperties().getString("theme-default-name"));
        themeSettingsPage.saveTheme();

        testRealmAdminConsolePage.navigateTo();
        testRealmLoginPage.waitForKeycloakThemePresent();
        testRealmLoginPage.form().login(testUser);
        testRealmAdminConsolePage.logOut();
    }

}
