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

package org.keycloak.testsuite.federation.storage.ldap.noimport;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runners.MethodSorters;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.mappers.UserAttributeMapper;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProviderFactory;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.federation.storage.ldap.LDAPExampleServlet;
import org.keycloak.testsuite.federation.storage.ldap.LDAPMultipleAttributesTest;
import org.keycloak.testsuite.federation.storage.ldap.LDAPTestUtils;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.LDAPRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.core.UriBuilder;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPMultipleAttributesNoImportTest extends LDAPMultipleAttributesTest {

    @Override
    protected void checkUserAndImportMode(KeycloakSession session, RealmModel realm, String username, String expectedFirstName, String expectedLastName, String expectedEmail, String expectedPostalCode) {
        UserModel user = session.users().getUserByUsername(username, realm);
        Assert.assertNull(session.userLocalStorage().getUserByUsername(username, realm));
        LDAPTestUtils.assertLoaded(user, username, expectedFirstName, expectedLastName, expectedEmail, expectedPostalCode);
    }

    @Override
    protected void checkImportMode(KeycloakSession session, RealmModel realm, UserModel user) {
        Assert.assertNull(session.userLocalStorage().getUserById(user.getId(), realm));

    }

    @Before
    public void setNoImportMode() throws Exception {
        KeycloakSession session = keycloakRule.startSession();
        try {
            session.userCache().clear();
            RealmModel appRealm = session.realms().getRealmByName("test");
            ldapModel.setImportEnabled(false);
            appRealm.updateComponent(ldapModel);

        } finally {
            keycloakRule.stopSession(session, true);
        }
    }

    @Test
    public void testModel() {
        super.testModel();
    }

    @Test
    public void ldapPortalEndToEndTest() {
       super.ldapPortalEndToEndTest();
    }



}


