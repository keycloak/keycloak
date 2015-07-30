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
package org.keycloak.testsuite.console.settings;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.testsuite.console.AbstractAdminConsoleTest;
import org.keycloak.testsuite.model.Theme;
import org.keycloak.testsuite.console.page.settings.ThemesSettingsPage;
import static org.keycloak.testsuite.page.auth.AuthRealm.TEST;

/**
 *
 * @author Filip Kiss
 */
public class ThemesSettingsTest extends AbstractAdminConsoleTest {

    @Page
    private ThemesSettingsPage page;

    @Before
    public void beforeThemeTest() {
        navigation.themes(TEST);
    }

    @Test
    public void changeLoginThemeTest() {
        page.changeLoginTheme(Theme.BASE.getName());
        page.saveTheme();
        logoutFromTestRealm();
        page.verifyBaseTheme();

        loginAsTestUser();
        navigation.themes(TEST);
        page.changeLoginTheme(Theme.KEYCLOAK.getName());
        page.saveTheme();
        logoutFromTestRealm();
        page.verifyKeycloakTheme();

        loginAsTestUser();
    }

}
