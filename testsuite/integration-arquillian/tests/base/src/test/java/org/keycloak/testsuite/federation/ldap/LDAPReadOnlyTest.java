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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.ClientErrorException;

import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticatorFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.HardcodedAttributeMapper;
import org.keycloak.storage.ldap.mappers.HardcodedAttributeMapperFactory;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.UserAttributeLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.msad.MSADUserAccountControlStorageMapper;
import org.keycloak.storage.ldap.mappers.msad.MSADUserAccountControlStorageMapperFactory;
import org.keycloak.testsuite.admin.AdminApiUtil;
import org.keycloak.testsuite.pages.LoginConfigTotpPage;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileProvider;

import org.hamcrest.MatcherAssert;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.By;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for more advanced scenarios related to LDAP read-only mode
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPReadOnlyTest extends AbstractLDAPTest  {

    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }

    @Page
    protected LoginConfigTotpPage totpPage;

    private TimeBasedOTP totp = new TimeBasedOTP();

    @Override
    protected void afterImportTestRealm() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            LDAPTestUtils.addZipCodeLDAPMapper(appRealm, ctx.getLdapModel());

            // Delete all LDAP users and add some new for testing
            LDAPTestUtils.removeAllLDAPUsers(ctx.getLdapProvider(), appRealm);

            LDAPObject john = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "johnkeycloak", "John", "Doe", "john@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), john, "Password1");

            LDAPObject existing = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "existing", "Existing", "Foo", "existing@email.org", null, "5678");

            appRealm.getClientByClientId("test-app").setDirectAccessGrantsEnabled(true);

            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            ldapFedProvider.getModel().put(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.READ_ONLY.toString());

            // change MSAD mapper config "ALWAYS_READ_ENABLED_VALUE_FROM_LDAP" to false as edit mode is read only so setEnable(false) is not propagated to LDAP
            ComponentModel msadMapperComponent = appRealm.getComponentsStream(ctx.getLdapModel().getId(), LDAPStorageMapper.class.getName())
                    .filter(c -> MSADUserAccountControlStorageMapperFactory.PROVIDER_ID.equals(c.getProviderId()))
                    .findFirst().orElse(null);
            if (msadMapperComponent != null) {
                msadMapperComponent.getConfig().putSingle(MSADUserAccountControlStorageMapper.ALWAYS_READ_ENABLED_VALUE_FROM_LDAP, "false");
                appRealm.updateComponent(msadMapperComponent);
            }

            appRealm.updateComponent(ldapFedProvider.getModel());
        });
    }


    // KEYCLOAK-15139
    @Test
    public void testReadOnlyWithTOTPEnabled() {
        // Set TOTP required
        setTotpRequirementExecutionForRealm(AuthenticationExecutionModel.Requirement.REQUIRED, AuthenticationExecutionModel.Requirement.REQUIRED);

        // Authenticate as the LDAP user and assert it works
        oauth.openLoginForm();
        loginPage.login("johnkeycloak", "Password1");

        totpPage.assertCurrent();
        assertFalse(totpPage.isCancelDisplayed());

        // KEYCLOAK-11753 - Verify OTP label element present on "Configure OTP" required action form
        driver.findElement(By.id("userLabel"));

        totpPage.configure(totp.generateTOTP(totpPage.getTotpSecret()));
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        // Revert TOTP
        setTotpRequirementExecutionForRealm(AuthenticationExecutionModel.Requirement.CONDITIONAL, AuthenticationExecutionModel.Requirement.ALTERNATIVE);
        UserResource user = AdminApiUtil.findUserByUsernameId(managedRealm.admin(), "johnkeycloak");
        String totpCredentialId = user.credentials().stream()
                .filter(credentialRep -> credentialRep.getType().equals(OTPCredentialModel.TYPE))
                .findFirst().get().getId();
        user.removeCredential(totpCredentialId);
    }

    // KEYCLOAK-3365
    @Test
    public void testReadOnlyUserDoesNotThrowIfUnchanged() {
        UserResource user = AdminApiUtil.findUserByUsernameId(managedRealm.admin(), "johnkeycloak");
        UserRepresentation userRepresentation = user.toRepresentation();
        userRepresentation.setRequiredActions(Collections.singletonList(UserModel.RequiredAction.CONFIGURE_TOTP.toString()));
        user.update(userRepresentation);

        // assert
        user = AdminApiUtil.findUserByUsernameId(managedRealm.admin(), "johnkeycloak");
        userRepresentation = user.toRepresentation();
        Assertions.assertEquals(userRepresentation.getRequiredActions().size(), 1);
        Assertions.assertEquals(userRepresentation.getRequiredActions().get(0), UserModel.RequiredAction.CONFIGURE_TOTP.toString());

        // reset
        userRepresentation.setRequiredActions(Collections.emptyList());
        user.update(userRepresentation);
    }

    @Test
    public void testUpdateLocale() {
        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        realm.setInternationalizationEnabled(true);
        managedRealm.admin().update(realm);
        UserResource user = AdminApiUtil.findUserByUsernameId(managedRealm.admin(), "johnkeycloak");

        UserRepresentation userRepresentation = user.toRepresentation();
        String language = "pt_BR";
        userRepresentation.setAttributes(Map.of(UserModel.LOCALE, List.of(language)));
        user.update(userRepresentation);

        userRepresentation = user.toRepresentation();
        assertEquals(language, userRepresentation.getAttributes().get(UserModel.LOCALE).get(0));

        userRepresentation.getAttributes().remove(UserModel.LOCALE);
        user.update(userRepresentation);
        assertNull(userRepresentation.getAttributes().get(UserModel.LOCALE));
    }

    @Test
    public void testLocalAttributeWritesDoNotUnlockLdapAttributes() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            UserModel user = session.users().getUserByUsername(ctx.getRealm(), "johnkeycloak");
            user.setSingleAttribute("localAttribute", "first");
            assertEquals("first", user.getFirstAttribute("localAttribute"));
            user.setAttribute("localAttribute", List.of("second", "third"));
            assertEquals(List.of("second", "third"), user.getAttributeStream("localAttribute").toList());
            user.removeAttribute("localAttribute");
            assertNull(user.getFirstAttribute("localAttribute"));

            for (String name : List.of(UserModel.USERNAME, UserModel.EMAIL, UserModel.FIRST_NAME,
                    UserModel.LAST_NAME, "postal_code", LDAPConstants.LDAP_ID, LDAPConstants.LDAP_ENTRY_DN)) {
                List<String> original = user.getAttributeStream(name).toList();
                user.setAttribute(name, original);
                assertThrows(ReadOnlyException.class, () -> user.setSingleAttribute(name, "changed"), name);
                assertThrows(ReadOnlyException.class, () -> user.setAttribute(name, List.of("changed")), name);
                assertThrows(ReadOnlyException.class, () -> user.removeAttribute(name), name);
                assertEquals(original, user.getAttributeStream(name).toList());
            }
            assertThrows(ReadOnlyException.class, () -> user.setUsername("changed"));
            assertThrows(ReadOnlyException.class, () -> user.setFirstName("changed"));
            assertThrows(ReadOnlyException.class, () -> user.setLastName("changed"));
            assertThrows(ReadOnlyException.class, () -> user.setEmail("changed@example.org"));
            assertThrows(ReadOnlyException.class, () -> ctx.getLdapProvider().updateCredential(ctx.getRealm(), user,
                    UserCredentialModel.password("ChangedPassword1")));
        });
    }

    @Test
    public void testMappedLocaleRemainsReadOnly() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel realm = ctx.getRealm();
            boolean internationalizationEnabled = realm.isInternationalizationEnabled();
            ComponentModel mapper = KeycloakModelUtils.createComponentModel("fixed-locale", ctx.getLdapModel().getId(),
                    HardcodedAttributeMapperFactory.PROVIDER_ID, LDAPStorageMapper.class.getName(),
                    HardcodedAttributeMapper.USER_MODEL_ATTRIBUTE, UserModel.LOCALE,
                    HardcodedAttributeMapper.ATTRIBUTE_VALUE, "en");
            realm.addComponentModel(mapper);
            try {
                realm.setInternationalizationEnabled(true);
                UserModel user = session.users().getUserByUsername(realm, "johnkeycloak");
                assertTrue(session.getProvider(UserProfileProvider.class).create(UserProfileContext.USER_API, user)
                        .getAttributes().isReadOnly(UserModel.LOCALE));
                assertThrows(ReadOnlyException.class, () -> user.setSingleAttribute(UserModel.LOCALE, "fr"));
            } finally {
                realm.removeComponent(mapper);
                realm.setInternationalizationEnabled(internationalizationEnabled);
            }
        });
    }

    @Test
    public void testMappedPropertiesAreCaseInsensitive() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            ComponentModel mapper = LDAPTestUtils.getSubcomponentByName(ctx.getRealm(), ctx.getLdapModel(), "email");
            String attribute = mapper.getConfig().getFirst(UserAttributeLDAPStorageMapper.USER_MODEL_ATTRIBUTE);
            mapper.put(UserAttributeLDAPStorageMapper.USER_MODEL_ATTRIBUTE, "EMAIL");
            ctx.getRealm().updateComponent(mapper);
            try {
                UserModel user = session.users().getUserByUsername(ctx.getRealm(), "johnkeycloak");
                assertThrows(ReadOnlyException.class, () -> user.setEmail("changed@example.org"));
                assertThrows(ReadOnlyException.class, () -> user.setSingleAttribute("EMAIL", "changed@example.org"));
            } finally {
                mapper.put(UserAttributeLDAPStorageMapper.USER_MODEL_ATTRIBUTE, attribute);
                ctx.getRealm().updateComponent(mapper);
            }
        });
    }

    // KEYCLOAK-3365
    @Test(expected = ClientErrorException.class)
    public void testReadOnlyUserThrowsIfChanged() {
        UserResource user = AdminApiUtil.findUserByUsernameId(managedRealm.admin(), "johnkeycloak");
        UserRepresentation userRepresentation = user.toRepresentation();
        userRepresentation.setFirstName("Jane");
        user.update(userRepresentation);
    }

    // issue #28580
    @Test
    public void testReadOnlyUserGetsPermanentlyLocked(){
        int failureFactor = 2;
        RealmRepresentation realm = managedRealm.admin().toRepresentation();
        try {
            // Set permanent lockout for the test
            realm.setBruteForceProtected(true);
            realm.setPermanentLockout(true);
            realm.setFailureFactor(failureFactor);
            managedRealm.admin().update(realm);

            UserRepresentation user = adminClient.realm("test").users().search("johnkeycloak", 0, 1).get(0);
            Map<String, Object> bruteForceStatus = managedRealm.admin().attackDetection().bruteForceUserStatus(user.getId());
            assertFalse((boolean) bruteForceStatus.get("disabled"), "User should not be disabled by brute force.");
            assertTrue(user.isEnabled());

            // Lock user (permanently) and make sure the number of failures matches failure factor
            loginInvalidPassword("johnkeycloak");
            loginInvalidPassword("johnkeycloak");
            assertUserNumberOfFailures(user.getId(), failureFactor);

            // Make sure user is now disabled
            bruteForceStatus = managedRealm.admin().attackDetection().bruteForceUserStatus(user.getId());
            assertTrue((boolean) bruteForceStatus.get("disabled"), "User should be disabled by brute force.");
            user = adminClient.realm("test").users().search("johnkeycloak", 0, 1).get(0);
            assertFalse(user.isEnabled());

            events.clear();
        } finally {
            realm.setBruteForceProtected(false);
            realm.setPermanentLockout(false);
            realm.setFailureFactor(30);
            managedRealm.admin().update(realm);
            UserRepresentation user = adminClient.realm("test").users().search("johnkeycloak", 0, 1).get(0);
            user.setEnabled(true);
            updateUser(user);
        }
    }

    public void loginInvalidPassword(String username) {
        oauth.openLoginForm();
        loginPage.login(username, "invalid");

        loginPage.assertCurrent();

        Assertions.assertEquals("Invalid username or password.", loginPage.getInputError());

        WaitUtils.waitForBruteForceExecutors(testingClient);

        events.clear();
    }

    private void assertUserNumberOfFailures(String userId, Integer numberOfFailures) {
        Map<String, Object> userAttackInfo = adminClient.realm("test").attackDetection().bruteForceUserStatus(userId);
        MatcherAssert.assertThat((Integer) userAttackInfo.get("numFailures"), is(numberOfFailures));
    }

    private void setTotpRequirementExecutionForRealm(AuthenticationExecutionModel.Requirement conditionalReq, AuthenticationExecutionModel.Requirement otpReq) {
        AuthenticationManagementResource authMgtRes = managedRealm.admin().flows();
        AuthenticationExecutionInfoRepresentation browserConditionalExecution = authMgtRes.getExecutions("browser").stream()
                .filter(execution -> execution.getDisplayName().equals("Browser - Conditional 2FA"))
                .findAny()
                .get();
        browserConditionalExecution.setRequirement(conditionalReq.name());
        authMgtRes.updateExecutions("browser", browserConditionalExecution);
        AuthenticationExecutionInfoRepresentation otpExecution = authMgtRes.getExecutions("Browser - Conditional 2FA").stream()
                .filter(execution -> OTPFormAuthenticatorFactory.PROVIDER_ID.equals(execution.getProviderId()))
                .findAny()
                .get();
        otpExecution.setRequirement(otpReq.name());
        authMgtRes.updateExecutions("browser", otpExecution);
    }

    protected void assertFederatedUserLink(UserRepresentation user) {
        Assertions.assertTrue(StorageId.isLocalStorage(user.getId()));
        Assertions.assertNotNull(user.getFederationLink());
        Assertions.assertEquals(user.getFederationLink(), ldapModelId);
    }
}
