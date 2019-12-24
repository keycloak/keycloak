/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestConfiguration;
import org.keycloak.testsuite.util.LDAPTestUtils;

import java.util.List;

/**
 * Test for special scenarios, which don't work on MSAD (eg. renaming user RDN to "sn=john2" )
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPNoMSADTest extends AbstractLDAPTest {

    // Skip this test on MSAD
    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule()
            .assumeTrue((LDAPTestConfiguration ldapConfig) -> {

                String vendor = ldapConfig.getLDAPConfig().get(LDAPConstants.VENDOR);
                return !LDAPConstants.VENDOR_ACTIVE_DIRECTORY.equals(vendor);

            });

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }


    @Override
    protected void afterImportTestRealm() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            LDAPTestUtils.addLocalUser(session, appRealm, "marykeycloak", "mary@test.com", "password-app");

            LDAPTestUtils.addZipCodeLDAPMapper(appRealm, ctx.getLdapModel());

            // Delete all LDAP users and add some new for testing
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            LDAPTestUtils.removeAllLDAPUsers(ldapFedProvider, appRealm);

            LDAPObject john = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "johnkeycloak", "John", "Doe", "john@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ldapFedProvider, john, "Password1");

            LDAPObject existing = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "existing", "Existing", "Foo", "existing@email.org", null, "5678");

            appRealm.getClientByClientId("test-app").setDirectAccessGrantsEnabled(true);
        });
    }


    // KEYCLOAK-4364
    @Test
    public void testUpdateWithUnmappedRdnAttribute() {
        ComponentRepresentation snMapperRep = findMapperRepByName("last name");

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            ComponentModel snMapper = null;

            // Create LDAP user with "sn" attribute in RDN like "sn=johnkeycloak2,ou=People,dc=domain,dc=com"
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            LDAPObject john2 = LDAPTestUtils.addLDAPUser(ldapProvider, appRealm, "johnkeycloak2", "john2", "Doe2", "john2@email.org", null, "4321");

            john2.setRdnAttributeName("sn");
            ldapProvider.getLdapIdentityStore().update(john2);

            // Remove "sn" mapper
            List<ComponentModel> components = appRealm.getComponents(ctx.getLdapModel().getId(), LDAPStorageMapper.class.getName());
            for (ComponentModel mapper : components) {
                if (mapper.getName().equals("last name")) {
                    snMapper = mapper;
                    break;
                }
            }

            Assert.assertNotNull(snMapper);
            appRealm.removeComponent(snMapper);
        });


        // Try to update johnkeycloak2 user. It shouldn't try to update DN
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel johnkeycloak2 = session.users().getUserByUsername("johnkeycloak2", appRealm);
            Assert.assertNotNull(johnkeycloak2);

            johnkeycloak2.setFirstName("foo2");
            johnkeycloak2.setLastName("foo");
        });

        // Re-create "sn" mapper back
        snMapperRep.setId(null);
        testRealm().components().add(snMapperRep);

    }
}
