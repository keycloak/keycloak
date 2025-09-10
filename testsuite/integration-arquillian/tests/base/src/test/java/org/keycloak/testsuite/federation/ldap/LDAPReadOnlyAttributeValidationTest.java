/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import jakarta.ws.rs.BadRequestException;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class LDAPReadOnlyAttributeValidationTest extends AbstractLDAPTest {

    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }

    @Override
    protected void afterImportTestRealm() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // Delete all LDAP users and add test user
            LDAPTestUtils.removeAllLDAPUsers(ctx.getLdapProvider(), appRealm);

            // Add LDAP user with standard LDAP attributes
            LDAPObject testUser = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, 
                "testuser", "Test", "User", "testuser@example.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), testUser, "Password1");
        });
    }

    /**
     * Tests that LDAP users can update custom attributes when LDAP timestamps are omitted from the request.
     * 
     * LDAP timestamps (createTimestamp, modifyTimestamp) are auto-managed by the LDAP server and should
     * be allowed to be omitted in update requests. When omitted, these attributes are preserved unchanged
     * rather than being cleared. This is different from other read-only attributes which should not be 
     * removable by users.
     */
    @Test
    public void updateCustomAttributeWithTimestampsOmitted() {
        UserRepresentation user = testRealm().users().search("testuser").get(0);
        UserResource userResource = testRealm().users().get(user.getId());
        
        // Try to update a custom attribute (not imported from LDAP)
        user.singleAttribute("customAttribute", "customValue");

        // omit auto-managed timestamps since they're maintained by the LDAP server
        if (user.getAttributes() != null) {
            user.getAttributes().remove("createTimestamp");
            user.getAttributes().remove("modifyTimestamp");
        }
        
        try {
            userResource.update(user);
        } catch (BadRequestException e) {
            fail("Custom attribute update should succeed when timestamps are omitted");
        }
    }


    @Test
    public void updateCustomAttributeWithCurrentTimestamp() {
        UserRepresentation user = testRealm().users().search("testuser").get(0);
        UserResource userResource = testRealm().users().get(user.getId());
        user = userResource.toRepresentation();
        
        // Try to update a custom attribute with current timestamp
        user.singleAttribute("customAttribute", "customValue");
        
        // Set timestamps to current time (this should be blocked)
        String currentTime = String.valueOf(System.currentTimeMillis());
        user.singleAttribute("modifyTimestamp", currentTime);
        
        try {
            userResource.update(user);
            fail("Should not be able to modify timestamp");
        } catch (BadRequestException e) {
            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            assertEquals("updateReadOnlyAttributesRejectedMessage", error.getErrorMessage());
            // Expected - modifying timestamps should be blocked
        }
    }

    @Test
    public void updateMultiValueCustomAttribute() {
        UserRepresentation user = testRealm().users().search("testuser").get(0);
        UserResource userResource = testRealm().users().get(user.getId());
        user = userResource.toRepresentation();
        
        // Try to update a multi-value custom attribute
        user.getAttributes().put("customMultiValueAttr", Arrays.asList("value1", "value2", "value3"));
        
        // Remove timestamp attributes
        if (user.getAttributes() != null) {
            user.getAttributes().remove("createTimestamp");
            user.getAttributes().remove("modifyTimestamp");
        }
        
        try {
            userResource.update(user);
        } catch (BadRequestException e) {
            fail("Multi-value custom attribute update should succeed when timestamps are omitted");
        }
    }

    @Test
    public void updateCustomAttributeWithTimestampsPreserved() {
        UserRepresentation user = testRealm().users().search("testuser").get(0);
        UserResource userResource = testRealm().users().get(user.getId());
        user = userResource.toRepresentation();
        
        // Add a custom attribute without modifying timestamps
        user.singleAttribute("testAttribute", "testValue");
        
        try {
            userResource.update(user);
            // Should succeed - timestamps are preserved
        } catch (BadRequestException e) {
            fail("Custom attribute update should succeed when timestamps are preserved");
        }
    }
}
