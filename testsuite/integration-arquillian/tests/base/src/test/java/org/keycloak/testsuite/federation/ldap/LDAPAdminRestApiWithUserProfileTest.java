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
 *
 */

package org.keycloak.testsuite.federation.ldap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.keycloak.testsuite.forms.VerifyProfileTest.disableDynamicUserProfile;
import static org.keycloak.testsuite.forms.VerifyProfileTest.setUserProfileConfiguration;
import static org.keycloak.util.JsonSerialization.readValue;
import static org.keycloak.util.JsonSerialization.writeValueAsString;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collections;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.Profile;
import org.keycloak.models.LDAPConstants;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.forms.VerifyProfileTest;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.userprofile.config.UPAttribute;
import org.keycloak.userprofile.config.UPAttributePermissions;
import org.keycloak.userprofile.config.UPConfig;

@EnableFeature(value = Profile.Feature.DECLARATIVE_USER_PROFILE)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPAdminRestApiWithUserProfileTest extends LDAPAdminRestApiTest {

    @Test
    public void testUpdateReadOnlyAttributeWhenNotSetToUser() throws Exception {
        RealmRepresentation realmRep = testRealm().toRepresentation();
        enableSyncRegistration(realmRep, Boolean.FALSE);

        UserRepresentation newUser = UserBuilder.create()
                .username("admintestuser1")
                .password("userpass")
                .addAttribute("foo", "foo-value")
                .enabled(true)
                .build();

        try (Response response = testRealm().users().create(newUser)) {
            enableDynamicUserProfile(realmRep);
            String newUserId = ApiUtil.getCreatedId(response);

            getCleanup().addUserId(newUserId);

            UserResource user = testRealm().users().get(newUserId);
            UserRepresentation userRep = user.toRepresentation();

            assertTrue(userRep.getAttributes().containsKey(LDAPConstants.LDAP_ID));
            assertTrue(userRep.getAttributes().get(LDAPConstants.LDAP_ID).isEmpty());

            userRep.singleAttribute(LDAPConstants.LDAP_ID, "");
            user.update(userRep);
            userRep.singleAttribute(LDAPConstants.LDAP_ID, null);
            user.update(userRep);

            try {
                userRep.singleAttribute(LDAPConstants.LDAP_ID, "should-fail");
                user.update(userRep);
                fail("Should fail, attribute is read-only");
            } catch (BadRequestException ignore) {
            }
        } finally {
            disableDynamicUserProfile(testRealm());
            enableSyncRegistration(realmRep, Boolean.TRUE);
        }
    }

    private void enableDynamicUserProfile(RealmRepresentation realmRep) throws IOException {
        VerifyProfileTest.enableDynamicUserProfile(realmRep);

        testRealm().update(realmRep);

        UPConfig upConfig = readValue(testRealm().users().userProfile().getConfiguration(), UPConfig.class);
        UPAttribute attribute = new UPAttribute();

        attribute.setName(LDAPConstants.LDAP_ID);

        UPAttributePermissions permissions = new UPAttributePermissions();

        permissions.setView(Collections.singleton("admin"));

        attribute.setPermissions(permissions);

        upConfig.addAttribute(attribute);

        setUserProfileConfiguration(testRealm(), writeValueAsString(upConfig));
    }

    private void enableSyncRegistration(RealmRepresentation realmRep, Boolean aFalse) {
        ComponentRepresentation ldapStorage = testRealm().components()
                .query(realmRep.getRealm(), UserStorageProvider.class.getName()).get(0);
        ldapStorage.getConfig().put(LDAPConstants.SYNC_REGISTRATIONS, Collections.singletonList(aFalse.toString()));
        testRealm().components().component(ldapStorage.getId()).update(ldapStorage);
    }
}
