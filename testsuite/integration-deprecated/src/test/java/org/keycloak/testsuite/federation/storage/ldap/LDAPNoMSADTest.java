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

package org.keycloak.testsuite.federation.storage.ldap;

import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runners.MethodSorters;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProviderFactory;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.LDAPRule;

/**
 * Test for special scenarios, which don't work on MSAD (eg. renaming user RDN to "sn=john2" )
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPNoMSADTest {

    private static final Logger log = Logger.getLogger(LDAPProvidersIntegrationTest.class);


    // Skip this test on MSAD
    private static LDAPRule ldapRule = new LDAPRule((Map<String, String> ldapConfig) -> {

        String vendor = ldapConfig.get(LDAPConstants.VENDOR);
        return (vendor.equals(LDAPConstants.VENDOR_ACTIVE_DIRECTORY));

    });

    private static ComponentModel ldapModel = null;


    private static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            LDAPTestUtils.addLocalUser(manager.getSession(), appRealm, "marykeycloak", "mary@test.com", "password-app");

            MultivaluedHashMap<String,String> ldapConfig = LDAPTestUtils.getLdapRuleConfig(ldapRule);
            ldapConfig.putSingle(LDAPConstants.SYNC_REGISTRATIONS, "true");
            ldapConfig.putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.WRITABLE.toString());
            UserStorageProviderModel model = new UserStorageProviderModel();
            model.setLastSync(0);
            model.setChangedSyncPeriod(-1);
            model.setFullSyncPeriod(-1);
            model.setName("test-ldap");
            model.setPriority(0);
            model.setProviderId(LDAPStorageProviderFactory.PROVIDER_NAME);
            model.setConfig(ldapConfig);

            ldapModel = appRealm.addComponentModel(model);
            LDAPTestUtils.addZipCodeLDAPMapper(appRealm, ldapModel);

            // Delete all LDAP users and add some new for testing
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPTestUtils.removeAllLDAPUsers(ldapFedProvider, appRealm);

            LDAPObject john = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "johnkeycloak", "John", "Doe", "john@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ldapFedProvider, john, "Password1");

            LDAPObject existing = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "existing", "Existing", "Foo", "existing@email.org", null, "5678");

            appRealm.getClientByClientId("test-app").setDirectAccessGrantsEnabled(true);
        }
    });

    @ClassRule
    public static TestRule chain = RuleChain
            .outerRule(ldapRule)
            .around(keycloakRule);


    // KEYCLOAK-4364
    @Test
    public void testUpdateWithUnmappedRdnAttribute() {
        KeycloakSession session = keycloakRule.startSession();
        ComponentModel snMapper = null;
        try {
            // Create LDAP user with "sn" attribute in RDN like "sn=johnkeycloak2,ou=People,dc=domain,dc=com"
            RealmModel appRealm = session.realms().getRealmByName("test");
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPObject john2 = LDAPTestUtils.addLDAPUser(ldapProvider, appRealm, "johnkeycloak2", "john2", "Doe2", "john2@email.org", null, "4321");

            john2.setRdnAttributeName("sn");
            ldapProvider.getLdapIdentityStore().update(john2);

            // Remove "sn" mapper
            List<ComponentModel> components = appRealm.getComponents(ldapModel.getId(), LDAPStorageMapper.class.getName());
            for (ComponentModel mapper : components) {
                if (mapper.getName().equals("last name")) {
                    snMapper = mapper;
                    break;
                }
            }

            Assert.assertNotNull(snMapper);
            appRealm.removeComponent(snMapper);
        } finally {
            keycloakRule.stopSession(session, true);
        }


        // Try to update johnkeycloak2 user. It shouldn't try to update DN
        session = keycloakRule.startSession();
        try {
            RealmModel appRealm = session.realms().getRealmByName("test");
            UserModel johnkeycloak2 = session.users().getUserByUsername("johnkeycloak2", appRealm);
            Assert.assertNotNull(johnkeycloak2);

            johnkeycloak2.setFirstName("foo2");
            johnkeycloak2.setLastName("foo");
        } finally {
            keycloakRule.stopSession(session, true);
        }

        // Re-create "sn" mapper back
        session = keycloakRule.startSession();
        try {
            RealmModel appRealm = session.realms().getRealmByName("test");
            snMapper.setId(null);
            appRealm.addComponentModel(snMapper);
        } finally {
            keycloakRule.stopSession(session, true);
        }

    }
}
