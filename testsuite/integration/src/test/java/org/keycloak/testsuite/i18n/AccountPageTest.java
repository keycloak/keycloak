/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.i18n;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;

/**
 * @author <a href="mailto:gerbermichi@me.com">Michael Gerber</a>
 */
public class AccountPageTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {
        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            UserModel user = manager.getSession().users().addUser(appRealm, "login-test");
            user.setEmail("login@test.com");
            user.setEnabled(true);

            UserCredentialModel creds = new UserCredentialModel();
            creds.setType(CredentialRepresentation.PASSWORD);
            creds.setValue("password");

            user.updateCredential(creds);
        }
    });

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected AccountUpdateProfilePage accountUpdateProfilePage;

    @WebResource
    protected LoginPage loginPage;

    @Test
    public void languageDropdown() {
        accountUpdateProfilePage.open();
        loginPage.login("login@test.com", "password");
        Assert.assertTrue(accountUpdateProfilePage.isCurrent());

        Assert.assertEquals("English", accountUpdateProfilePage.getLanguageDropdownText());

        accountUpdateProfilePage.openLanguage("Deutsch");
        Assert.assertEquals("Deutsch", accountUpdateProfilePage.getLanguageDropdownText());

        accountUpdateProfilePage.openLanguage("English");
        Assert.assertEquals("English", accountUpdateProfilePage.getLanguageDropdownText());
        accountUpdateProfilePage.logout();
    }
}
