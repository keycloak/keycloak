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

package org.keycloak.testsuite.account;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;

import org.keycloak.admin.client.resource.UserProfileResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.representations.account.UserRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.userprofile.config.UPConfig.UnmanagedAttributePolicy;
import org.keycloak.services.messages.Messages;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testsuite.admin.AdminApiUtil;
import org.keycloak.userprofile.UserProfileConstants;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@KeycloakIntegrationTest
public class AccountRestServiceReadOnlyAttributesTest {

    private static final Logger logger = Logger.getLogger(AccountRestServiceReadOnlyAttributesTest.class);

    @InjectRealm(config = AccountRestServiceReadOnlyAttributesRealm.class)
    ManagedRealm managedRealm;

    @InjectUser(config = AccountRestServiceReadOnlyAttributesUser.class)
    ManagedUser testUser;

    @InjectSimpleHttp
    SimpleHttp simpleHttp;

    @InjectOAuthClient
    OAuthClient oAuthClient;

    private String accessToken;

    @TestSetup
    public void configureUserProfile() {
        // Get the OAuth access token for the test user
        accessToken = oAuthClient.doPasswordGrantRequest(testUser.getUsername(), testUser.getPassword()).getAccessToken();

        UserProfileResource userProfileRes = managedRealm.admin().users().userProfile();
        UPConfig cfg = userProfileRes.getConfiguration();
        //cfg.setUnmanagedAttributePolicy(UPConfig.UnmanagedAttributePolicy.ENABLED);
        cfg.addOrReplaceAttribute(createUpAttribute("someOtherAttr"));
        cfg.addOrReplaceAttribute(createUpAttribute("usercertificate"));
        cfg.addOrReplaceAttribute(createUpAttribute("uSErCertificate"));
        cfg.addOrReplaceAttribute(createUpAttribute("KERBEROS_PRINCIPAL"));
        cfg.addOrReplaceAttribute(createUpAttribute("noKerberos_Principal"));
        cfg.addOrReplaceAttribute(createUpAttribute("KERBEROS_PRINCIPALno"));
        cfg.addOrReplaceAttribute(createUpAttribute("enabled"));
        cfg.addOrReplaceAttribute(createUpAttribute("CREATED_TIMESTAMP"));
        cfg.addOrReplaceAttribute(createUpAttribute("saml.something"));

        cfg.addOrReplaceAttribute(createUpAttribute("deniedfoo"));
        cfg.addOrReplaceAttribute(createUpAttribute("deniedFOo"));
        cfg.addOrReplaceAttribute(createUpAttribute("deniedFoot"));
        cfg.addOrReplaceAttribute(createUpAttribute("deniedbar"));
        cfg.addOrReplaceAttribute(createUpAttribute("deniedBAr"));
        cfg.addOrReplaceAttribute(createUpAttribute("deniedBArr"));
        cfg.addOrReplaceAttribute(createUpAttribute("deniedbarrier"));
        cfg.addOrReplaceAttribute(createUpAttribute("nodeniedbar"));
        cfg.addOrReplaceAttribute(createUpAttribute("nodeniedBARrier"));
        cfg.addOrReplaceAttribute(createUpAttribute("saml.persistent.name.id.for.foo"));
        cfg.addOrReplaceAttribute(createUpAttribute("saml.persistent.name.id.for._foo_"));
        cfg.addOrReplaceAttribute(createUpAttribute("saml.persistent.name.idafor.foo"));
        // TODO: Doublecheck this. We should either document that attributes with custom characters are not allowed or we should enable to configure them
        // cfg.addOrReplaceAttribute(createUpAttribute("deniedsome/thing"));
        // cfg.addOrReplaceAttribute(createUpAttribute("deniedsome*thing"));
        cfg.addOrReplaceAttribute(createUpAttribute("deniedsomeithing"));
        cfg.addOrReplaceAttribute(createUpAttribute("deniedSomeAdmin"));
        userProfileRes.update(cfg);
    }

    private UPAttribute createUpAttribute(String name) {
        return new UPAttribute(name, new UPAttributePermissions(Collections.emptySet(), Set.of(UserProfileConstants.ROLE_USER, UserProfileConstants.ROLE_ADMIN)));
    }

    // Test read-only attributes from provider configuration have precedence over the user-profile realm configuration settings (Read-only attributes from provider config are always read-only)
    @Test
    public void testUpdateProfileCannotUpdateReadOnlyAttributes() throws IOException {
        // Denied by default
        testAccountUpdateAttributeExpectFailure("usercertificate");
        testAccountUpdateAttributeExpectFailure("uSErCertificate");
        testAccountUpdateAttributeExpectFailure("KERBEROS_PRINCIPAL", true);

        // Should be allowed
        testAccountUpdateAttributeExpectSuccess("noKerberos_Principal");
        testAccountUpdateAttributeExpectSuccess("KERBEROS_PRINCIPALno");

        // Denied by default
        testAccountUpdateAttributeExpectFailure("enabled");
        testAccountUpdateAttributeExpectFailure("CREATED_TIMESTAMP", true);

        // Should be allowed
        testAccountUpdateAttributeExpectSuccess("saml.something");

        // Denied by configuration. "deniedFoot" is allowed as there is no wildcard
        testAccountUpdateAttributeExpectFailure("deniedfoo");
        testAccountUpdateAttributeExpectFailure("deniedFOo");
        testAccountUpdateAttributeExpectSuccess("deniedFoot");

        // Denied by configuration. There is wildcard at the end
        testAccountUpdateAttributeExpectFailure("deniedbar");
        testAccountUpdateAttributeExpectFailure("deniedBAr");
        testAccountUpdateAttributeExpectFailure("deniedBArr");
        testAccountUpdateAttributeExpectFailure("deniedbarrier");

        // Wildcard just at the end
        testAccountUpdateAttributeExpectSuccess("nodeniedbar");
        testAccountUpdateAttributeExpectSuccess("nodeniedBARrier");

        // Wildcard at the end
        testAccountUpdateAttributeExpectFailure("saml.persistent.name.id.for.foo");
        testAccountUpdateAttributeExpectFailure("saml.persistent.name.id.for._foo_");
        testAccountUpdateAttributeExpectSuccess("saml.persistent.name.idafor.foo");

        // TODO: Uncomment similarly like above
        // Special characters inside should be quoted
        //testAccountUpdateAttributeExpectFailure("deniedsome/thing");
        //testAccountUpdateAttributeExpectFailure("deniedsome*thing");
        testAccountUpdateAttributeExpectSuccess("deniedsomeithing");

        // Denied only for admin, but allowed for normal user
        testAccountUpdateAttributeExpectSuccess("deniedSomeAdmin");
    }

    @Test
    public void testUpdateProfileCannotUpdateReadOnlyAttributesUnmanagedEnabled() throws IOException {
        UPConfig configuration = managedRealm.admin().users().userProfile().getConfiguration();
        UnmanagedAttributePolicy unmanagedAttributePolicy = configuration.getUnmanagedAttributePolicy();
        configuration.setUnmanagedAttributePolicy(UnmanagedAttributePolicy.ENABLED);

        // Register cleanup to restore original policy
        managedRealm.cleanup().add(() -> {
            configuration.setUnmanagedAttributePolicy(unmanagedAttributePolicy);
            managedRealm.admin().users().userProfile().update(configuration);
        });

        managedRealm.admin().users().userProfile().update(configuration);
        UserRepresentation user = get();
        UserResource adminUserResource = AdminApiUtil.findUserByUsernameId(managedRealm.admin(), user.getUsername());
        org.keycloak.representations.idm.UserRepresentation adminUserRep = adminUserResource.toRepresentation();
        adminUserRep.singleAttribute("deniedFoo", "foo");
        adminUserResource.update(adminUserRep);
        adminUserResource = AdminApiUtil.findUserByUsernameId(managedRealm.admin(), user.getUsername());
        adminUserRep = adminUserResource.toRepresentation();
        assertEquals("foo", adminUserRep.getAttributes().get("deniedFoo").get(0));
        assertNull(user.getAttributes());
        updateAndGet(user);
        adminUserResource = AdminApiUtil.findUserByUsernameId(managedRealm.admin(), user.getUsername());
        adminUserRep = adminUserResource.toRepresentation();
        assertEquals("foo", adminUserRep.getAttributes().get("deniedFoo").get(0));
    }

    private void testAccountUpdateAttributeExpectFailure(String attrName) throws IOException {
        testAccountUpdateAttributeExpectFailure(attrName, false);
    }

    private void testAccountUpdateAttributeExpectFailure(String attrName, boolean deniedForAdminAsWell) throws IOException {
        // Attribute not yet supposed to be on the user
        UserRepresentation user = get();
        assertThat(Optional.ofNullable(user.getAttributes()).orElse(Map.of()).keySet(), not(contains(attrName)));

        // Assert not possible to add the attribute to the user
        user.singleAttribute(attrName, "foo");
        updateError(user, 400, Messages.UPDATE_READ_ONLY_ATTRIBUTES_REJECTED);

        // Add the attribute to the user with admin REST (Case when we are adding new attribute)
        UserResource adminUserResource = null;
        org.keycloak.representations.idm.UserRepresentation adminUserRep = null;
        try {
            adminUserResource = AdminApiUtil.findUserByUsernameId(managedRealm.admin(), user.getUsername());
            adminUserRep = adminUserResource.toRepresentation();
            adminUserRep.singleAttribute(attrName, "foo");
            adminUserResource.update(adminUserRep);
            if (deniedForAdminAsWell) {
                Assertions.fail("Not expected to update attribute " + attrName + " by admin REST API");
            }
        } catch (BadRequestException bre) {
            if (!deniedForAdminAsWell) {
                Assertions.fail("Was expected to update attribute " + attrName + " by admin REST API");
            }
            return;
        }

        // Update attribute of the user with account REST to the same value (Case when we are updating existing attribute) - should be fine as our attribute is not changed
        user = get();
        Assertions.assertEquals("foo", user.getAttributes().get(attrName).get(0));
        user.singleAttribute("someOtherAttr", "foo");
        user = updateAndGet(user);

        // Update attribute of the user with account REST (Case when we are updating existing attribute
        user.singleAttribute(attrName, "foo-updated");
        updateError(user, 400, Messages.UPDATE_READ_ONLY_ATTRIBUTES_REJECTED);

        // Removal of read-only attribute not allowed
        user.getAttributes().remove(attrName);
        updateError(user, 400, Messages.UPDATE_READ_ONLY_ATTRIBUTES_REJECTED);
        user = get();
        assertTrue(user.getAttributes().containsKey(attrName));

        // Revert with admin REST
        adminUserRep.getAttributes().remove(attrName);
        adminUserRep.getAttributes().remove("someOtherAttr");
        adminUserResource.update(adminUserRep);
    }

    private void testAccountUpdateAttributeExpectSuccess(String attrName) throws IOException {
        // Attribute not yet supposed to be on the user
        UserRepresentation user = get();
        assertThat(Optional.ofNullable(user.getAttributes()).orElse(Map.of()).keySet(), not(contains(attrName)));

        // Assert not possible to add the attribute to the user
        user.singleAttribute(attrName, "foo");
        user = updateAndGet(user);

        // Update attribute of the user with account REST to the same value (Case when we are updating existing attribute) - should be fine as our attribute is not changed
        user = get();
        Assertions.assertEquals("foo", user.getAttributes().get(attrName).get(0));
        user.singleAttribute("someOtherAttr", "foo");
        user = updateAndGet(user);

        // Update attribute of the user with account REST (Case when we are updating existing attribute
        user.singleAttribute(attrName, "foo-updated");
        user = updateAndGet(user);

        // Remove attribute from the user with account REST (Case when we are removing existing attribute)
        user.getAttributes().remove(attrName);
        user = updateAndGet(user);

        // Revert
        user.getAttributes().remove("foo");
        user.getAttributes().remove("someOtherAttr");
        user = updateAndGet(user);
    }

    private UserRepresentation updateAndGet(UserRepresentation user) throws IOException {
        int status = simpleHttp.doPost(getAccountUrl(null))
                .header("Authorization", "Bearer " + accessToken)
                .json(user)
                .asStatus();
        assertEquals(204, status);
        return get();
    }

    private UserRepresentation get() throws IOException {
        return simpleHttp.doGet(getAccountUrl(null))
                .header("Authorization", "Bearer " + accessToken)
                .asJson(UserRepresentation.class);
    }

    private void updateError(UserRepresentation user, int expectedStatus, String expectedMessage) throws IOException {
        var response = simpleHttp.doPost(getAccountUrl(null))
                .header("Authorization", "Bearer " + accessToken)
                .json(user)
                .asResponse();
        assertEquals(expectedStatus, response.getStatus());
        assertEquals(expectedMessage, response.asJson(ErrorRepresentation.class).getErrorMessage());
    }

    private String getAccountUrl(String resource) {
        String url = managedRealm.getBaseUrl() + "/realms/" + managedRealm.getName() + "/account";
        if (resource != null) {
            url += "/" + resource;
        }
        return url;
    }

    /**
     * Realm configuration for the test
     */
    public static class AccountRestServiceReadOnlyAttributesRealm implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.user(UserBuilder.create().username("no-account-access").password("password"))
                    .user(UserBuilder.create().username("view-account-access").clientRoles("account", "view-profile").password("password"))
                    .user(UserBuilder.create().username("view-applications-access").realmRoles("user", "offline_access").clientRoles("account", "view-applications").clientRoles("account", "manage-consent").password("password"))
                    .user(UserBuilder.create().username("view-consent-access").clientRoles("account", "view-consent").password("password"))
                    .user(UserBuilder.create().username("manage-consent-access").clientRoles("account", "manage-consent").clientRoles("account", "view-profile").password("password"))
                    .user(UserBuilder.create().username("manage-account-access").clientRoles("account", "view-profile").clientRoles("account", "manage-account").realmRoles("user", "offline_access").password("password"));

            return realm;
        }
    }

    /**
     * User configuration for the test
     */
    public static class AccountRestServiceReadOnlyAttributesUser implements UserConfig {

        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("test-user@localhost")
                    .password("password")
                    .clientRoles("account", "view-profile")
                    .clientRoles("account", "manage-account")
                    .realmRoles("user", "offline_access");
        }
    }
}
