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

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginConfigTotpPage;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;
import org.openqa.selenium.By;

import javax.ws.rs.ClientErrorException;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
            appRealm.updateComponent(ldapFedProvider.getModel());
        });
    }


    // KEYCLOAK-15139
    @Test
    public void testReadOnlyWithTOTPEnabled() {
        // Set TOTP required
        setTotpRequirementExecutionForRealm(AuthenticationExecutionModel.Requirement.REQUIRED);

        // Authenticate as the LDAP user and assert it works
        loginPage.open();
        loginPage.login("johnkeycloak", "Password1");

        assertTrue(totpPage.isCurrent());
        assertFalse(totpPage.isCancelDisplayed());

        // KEYCLOAK-11753 - Verify OTP label element present on "Configure OTP" required action form
        driver.findElement(By.id("userLabel"));

        totpPage.configure(totp.generateTOTP(totpPage.getTotpSecret()));

        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        // Revert TOTP
        setTotpRequirementExecutionForRealm(AuthenticationExecutionModel.Requirement.CONDITIONAL);
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), "johnkeycloak");
        String totpCredentialId = user.credentials().stream()
                .filter(credentialRep -> credentialRep.getType().equals(OTPCredentialModel.TYPE))
                .findFirst().get().getId();
        user.removeCredential(totpCredentialId);
    }

    // KEYCLOAK-3365
    @Test
    public void testReadOnlyUserDoesNotThrowIfUnchanged() {
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), "johnkeycloak");
        UserRepresentation userRepresentation = user.toRepresentation();
        userRepresentation.setRequiredActions(Collections.singletonList(UserModel.RequiredAction.CONFIGURE_TOTP.toString()));
        user.update(userRepresentation);

        // assert
        user = ApiUtil.findUserByUsernameId(testRealm(), "johnkeycloak");
        userRepresentation = user.toRepresentation();
        Assert.assertEquals(userRepresentation.getRequiredActions().size(), 1);
        Assert.assertEquals(userRepresentation.getRequiredActions().get(0), UserModel.RequiredAction.CONFIGURE_TOTP.toString());

        // reset
        userRepresentation.setRequiredActions(Collections.emptyList());
        user.update(userRepresentation);
    }

    // KEYCLOAK-3365
    @Test(expected = ClientErrorException.class)
    public void testReadOnlyUserThrowsIfChanged() {
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), "johnkeycloak");
        UserRepresentation userRepresentation = user.toRepresentation();
        userRepresentation.setFirstName("Jane");
        user.update(userRepresentation);
    }

    private void setTotpRequirementExecutionForRealm(AuthenticationExecutionModel.Requirement requirement) {
        adminClient.realm("test").flows().getExecutions("browser").
                stream().filter(execution -> execution.getDisplayName().equals("Browser - Conditional OTP"))
                .forEach(execution ->
                {execution.setRequirement(requirement.name());
                    adminClient.realm("test").flows().updateExecutions("browser", execution);});
    }


    protected void assertFederatedUserLink(UserRepresentation user) {
        Assert.assertTrue(StorageId.isLocalStorage(user.getId()));
        Assert.assertNotNull(user.getFederationLink());
        Assert.assertEquals(user.getFederationLink(), ldapModelId);
    }
}
