/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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

        accountUpdateProfilePage.openLanguage("German");
        Assert.assertEquals("Deutsch", accountUpdateProfilePage.getLanguageDropdownText());

        accountUpdateProfilePage.openLanguage("English");
        Assert.assertEquals("English", accountUpdateProfilePage.getLanguageDropdownText());
        accountUpdateProfilePage.logout();
    }
}
