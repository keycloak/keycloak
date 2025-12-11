/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.federation.ldap;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestConfiguration;
import org.keycloak.testsuite.util.LDAPTestUtils;

import org.junit.ClassRule;
import org.junit.Test;

public class LDAPUserPropertiesMappingTest extends AbstractLDAPTest {

    public static final String USER_EMAIL_VERIFIED_LDAP_ATTRIBUTE = "l";
    public static final String USER_ENABLED_LDAP_ATTRIBUTE = "o";

    public static final String DIETMAR = "dietmar"; // enabled=true, emailVerified=true
    public static final String STEFAN = "stefan"; // enabled=false, emailVerified=false

    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule()
            .assumeTrue(LDAPTestConfiguration::isStartEmbeddedLdapServer);

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }

    @Override
    protected void afterImportTestRealm() {
        testingClient.server().run(session -> {

            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            ComponentModel ldapModel = appRealm.getComponentsStream(appRealm.getId(), UserStorageProvider.class.getName()).findFirst().get();
            ldapModel.getConfig().putSingle(UserStorageProviderModel.IMPORT_ENABLED, "false");
            appRealm.updateComponent(ldapModel);

            ComponentModel emailVerifiedMapperModel = LDAPTestUtils.addUserAttributeMapper(appRealm, ldapModel, "customEmailVerifiedMapper", "emailVerified", USER_EMAIL_VERIFIED_LDAP_ATTRIBUTE);
            appRealm.updateComponent(emailVerifiedMapperModel);

            ComponentModel enabledMapperModel = LDAPTestUtils.addUserAttributeMapper(appRealm, ldapModel, "customEnabledMapper", "enabled", USER_ENABLED_LDAP_ATTRIBUTE);
            appRealm.updateComponent(enabledMapperModel);

            appRealm.getClientByClientId("test-app").setDirectAccessGrantsEnabled(true);

            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);

            LDAPTestUtils.addLdapUser(session, appRealm, ldapFedProvider, DIETMAR, null, user -> {
                user.setEnabled(true);
                user.setEmailVerified(true);
            });

            LDAPTestUtils.addLdapUser(session, appRealm, ldapFedProvider, STEFAN, null, user -> {
                user.setEnabled(false);
                user.setEmailVerified(false);
            });
        });
    }

    @Test
    public void createAndReadUser() {
        testingClient.server(TEST_REALM_NAME).run(session -> {
            KeycloakContext context = session.getContext();
            RealmModel realm = context.getRealm();

            UserModel test10 = session.users().getUserByUsername(realm, DIETMAR);
            Assert.assertTrue(test10.isEnabled());
            Assert.assertTrue(test10.isEmailVerified());

            UserModel test11 = session.users().getUserByUsername(realm, STEFAN);
            Assert.assertFalse(test11.isEnabled());
            Assert.assertFalse(test11.isEmailVerified());

            ComponentModel ldapProviderModel = LDAPTestUtils.getLdapProviderModel(realm);
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ldapProviderModel);

            LDAPObject user10FromLdap = ldapProvider.loadLDAPUserByUsername(realm, DIETMAR);
            Assert.assertTrue(Boolean.parseBoolean(user10FromLdap.getAttributeAsString(USER_EMAIL_VERIFIED_LDAP_ATTRIBUTE)));
            Assert.assertTrue(Boolean.parseBoolean(user10FromLdap.getAttributeAsString(USER_ENABLED_LDAP_ATTRIBUTE)));

            LDAPObject user11FromLdap = ldapProvider.loadLDAPUserByUsername(realm, STEFAN);
            Assert.assertFalse(Boolean.parseBoolean(user11FromLdap.getAttributeAsString(USER_EMAIL_VERIFIED_LDAP_ATTRIBUTE)));
            Assert.assertFalse(Boolean.parseBoolean(user11FromLdap.getAttributeAsString(USER_ENABLED_LDAP_ATTRIBUTE)));
        });
    }
}
