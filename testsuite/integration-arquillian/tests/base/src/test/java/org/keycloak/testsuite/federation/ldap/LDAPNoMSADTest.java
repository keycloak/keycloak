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
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPDn;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestConfiguration;
import org.keycloak.testsuite.util.LDAPTestUtils;

import java.util.List;
import java.util.Objects;

import static org.hamcrest.Matchers.equalToIgnoringCase;

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

            // Create LDAP user with "sn" attribute in RDN like "sn=Doe2,ou=People,dc=domain,dc=com"
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            LDAPObject john2 = LDAPTestUtils.addLDAPUser(ldapProvider, appRealm, "johnkeycloak2", "John2", "Doe2", "john2@email.org", null, "4321");

            john2.setRdnAttributeName("sn");
            ldapProvider.getLdapIdentityStore().update(john2);

            // Assert DN was changed
            Assert.assertEquals("sn=Doe2", john2.getDn().getFirstRdn().toString());

            // Remove "sn" mapper
            snMapper = appRealm.getComponentsStream(ctx.getLdapModel().getId(), LDAPStorageMapper.class.getName())
                    .filter(mapper -> Objects.equals(mapper.getName(), "last name"))
                    .findFirst()
                    .orElse(null);

            Assert.assertNotNull(snMapper);
            appRealm.removeComponent(snMapper);
        });


        // Try to update johnkeycloak2 user. It shouldn't try to update DN
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel johnkeycloak2 = session.users().getUserByUsername(appRealm, "johnkeycloak2");
            Assert.assertNotNull(johnkeycloak2);

            johnkeycloak2.setFirstName("foo2");
            johnkeycloak2.setLastName("foo");
        });

        // Re-create "sn" mapper back
        snMapperRep.setId(null);
        testRealm().components().add(snMapperRep);

    }


    // KEYCLOAK-12842
    @Test
    public void testMultivaluedRDN() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            ComponentModel snMapper = null;

            // Create LDAP user with both "uid" and "sn" attribute in RDN. Something like "uid=johnkeycloak3+sn=Doe3,ou=People,dc=domain,dc=com"
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            LDAPObject john2 = LDAPTestUtils.addLDAPUser(ldapProvider, appRealm, "johnkeycloak3", "John3", "Doe3", "john3@email.org", null, "4321");

            john2.addRdnAttributeName("sn");
            ldapProvider.getLdapIdentityStore().update(john2);

            // Assert DN was changed
            String rdnAttrName = ldapProvider.getLdapIdentityStore().getConfig().getRdnLdapAttribute();
            Assert.assertEquals(rdnAttrName + "=johnkeycloak3+sn=Doe3", john2.getDn().getFirstRdn().toString());
        });

        // Update some user attributes not mapped to DN. DN won't be changed
        String userId = testRealm().users().search("johnkeycloak3").get(0).getId();
        UserResource user = testRealm().users().get(userId);

        UserRepresentation userRep = user.toRepresentation();
        assertFirstRDNEndsWith(userRep, "johnkeycloak3", "Doe3");
        userRep.setEmail("newemail@email.cz");
        user.update(userRep);

        userRep = user.toRepresentation();
        Assert.assertEquals("newemail@email.cz", userRep.getEmail());
        assertFirstRDNEndsWith(userRep, "johnkeycloak3", "Doe3");

        // Update some user attributes mapped to DN. DN will be changed
        userRep.setLastName("Doe3Changed");
        user.update(userRep);

        userRep = user.toRepresentation();

        // ApacheDS bug causes that attribute, which was added to DN, is lowercased. Works for other LDAPs (RHDS, OpenLDAP)
        Assert.assertThat("Doe3Changed", equalToIgnoringCase(userRep.getLastName()));
        assertFirstRDNEndsWith(userRep, "johnkeycloak3", "Doe3Changed");

        // Remove user
        user.remove();
    }

    private void assertFirstRDNEndsWith(UserRepresentation user, String expectedUsernameInDN, String expectedLastNameInDN) {
        String currentDN = user.getAttributes().get(LDAPConstants.LDAP_ENTRY_DN).get(0);
        LDAPDn.RDN firstRDN = LDAPDn.fromString(currentDN).getFirstRdn();

        // Order is not guaranteed and can be dependent on LDAP server, so can't test simple string
        List<String> rdnKeys = firstRDN.getAllKeys();
        Assert.assertEquals(2, rdnKeys.size());
        Assert.assertEquals(expectedLastNameInDN, firstRDN.getAttrValue("sn"));
        rdnKeys.remove("sn");
        Assert.assertEquals(expectedUsernameInDN, firstRDN.getAttrValue(rdnKeys.get(0)));
    }


}
