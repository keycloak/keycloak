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

package org.keycloak.testsuite.ui.account3;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.keycloak.common.Profile;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.ui.AbstractUiTest;
import org.keycloak.testsuite.ui.account3.page.PageNotFound;
import org.keycloak.testsuite.ui.account3.page.WelcomeScreen;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWithLoginUrlOf;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public abstract class AbstractAccountTest extends AbstractUiTest {
    public static final String ACCOUNT_THEME_NAME_KC = "keycloak.v2";
    public static final DateTimeFormatter DEFAULT_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a", Locale.ENGLISH);

    @Page
    protected WelcomeScreen accountWelcomeScreen;

    @Page
    protected PageNotFound pageNotFound;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        super.addTestRealms(testRealms);
        RealmRepresentation testRealmRep = testRealms.get(0);
        testRealmRep.setAccountTheme(getAccountThemeName());
    }

    @Before
    public void navigateBeforeTest() {
        accountWelcomeScreen.navigateTo();
    }

    @Override
    protected boolean isAccountPreviewTheme() {
        return true;
    }

    protected void loginToAccount() {
        assertCurrentUrlStartsWithLoginUrlOf(accountWelcomeScreen);
        loginPage.form().login(testUser);
    }

    protected String getAccountThemeName() {
        return ACCOUNT_THEME_NAME_KC;
    }
}
