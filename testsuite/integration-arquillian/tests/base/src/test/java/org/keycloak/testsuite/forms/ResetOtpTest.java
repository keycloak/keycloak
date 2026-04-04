package org.keycloak.testsuite.forms;

import java.util.Map;
import java.util.stream.Collectors;

import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordResetPage;
import org.keycloak.testsuite.pages.ResetOtpPage;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Test;

import static org.wildfly.common.Assert.assertTrue;

public class ResetOtpTest extends AbstractTestRealmKeycloakTest {

    @Page
    protected LoginPage loginPage;

    @Page
    protected ResetOtpPage resetOtpPage;

    @Page
    protected LoginPasswordResetPage resetPasswordPage;

    private static RealmResource realmResource;
    private static String resetOtpExecutionId;
    private static String resetOtpConfigId;
    private static String flowId;
    private static String origResetCredentialsFlowId;

    private static final String FLOW_ALIAS = "otpResetTestFlow";
    private static final String RESET_OTP_TEST_USER_REMOVE_NONE = "reset-otp-test-user-remove-none";
    private static final String RESET_OTP_TEST_USER_REMOVE_ONE = "reset-otp-test-user-remove-one";
    private static final String RESET_OTP_TEST_USER_REMOVE_ALL = "reset-otp-test-user-remove-all";

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        var secretCredentialData = "{\"digits\":6,\"counter\":0,\"period\":30,\"algorithm\":\"HmacSHA1\",\"subType\":\"totp\"}";

        var removeNoneCredential = new CredentialRepresentation();
        removeNoneCredential.setId("Otp-RemoveNone");
        removeNoneCredential.setType("otp");
        removeNoneCredential.setUserLabel("Otp");
        removeNoneCredential.setCreatedDate(-1L);
        removeNoneCredential.setSecretData("{\"value\":\"DJmQfC73VGFhw7D4QJ8C\"}");
        removeNoneCredential.setCredentialData(secretCredentialData);

        var removeAllCredential1 = new CredentialRepresentation();
        removeAllCredential1.setId("Otp1-RemoveAll");
        removeAllCredential1.setType("otp");
        removeAllCredential1.setUserLabel("Otp1");
        removeAllCredential1.setCreatedDate(-1L);
        removeAllCredential1.setSecretData("{\"value\":\"DJmQfC73VGFhw7D4QJ8D\"}");
        removeAllCredential1.setCredentialData(secretCredentialData);

        var removeAllCredential2 = new CredentialRepresentation();
        removeAllCredential2.setId("Otp2-RemoveAll");
        removeAllCredential2.setType("otp");
        removeAllCredential2.setUserLabel("Otp2");
        removeAllCredential2.setCreatedDate(-1L);
        removeAllCredential2.setSecretData("{\"value\":\"DJmQfC73VGFhw7D4QJ8E\"}");
        removeAllCredential2.setCredentialData(secretCredentialData);

        var removeOneCredential1 = new CredentialRepresentation();
        removeOneCredential1.setId("Otp1-RemoveOne");
        removeOneCredential1.setType("otp");
        removeOneCredential1.setUserLabel("Otp1");
        removeOneCredential1.setCreatedDate(-1L);
        removeOneCredential1.setSecretData("{\"value\":\"DJmQfC73VGFhw7D4QJ8F\"}");
        removeOneCredential1.setCredentialData(secretCredentialData);

        var removeOneCredential2 = new CredentialRepresentation();
        removeOneCredential2.setId("Otp2-RemoveOne");
        removeOneCredential2.setType("otp");
        removeOneCredential2.setUserLabel("Otp2");
        removeOneCredential2.setCreatedDate(-1L);
        removeOneCredential2.setSecretData("{\"value\":\"DJmQfC73VGFhw7D4QJ8G\"}");
        removeOneCredential2.setCredentialData(secretCredentialData);

        var userRemoveNone = UserBuilder.create();
        userRemoveNone.username("reset-otp-test-user-remove-none");
        userRemoveNone.secret(removeNoneCredential);

        var userRemoveOne = UserBuilder.create();
        userRemoveOne.username("reset-otp-test-user-remove-one");
        userRemoveOne.secret(removeOneCredential1);
        userRemoveOne.secret(removeOneCredential2);

        var userRemoveAll = UserBuilder.create();
        userRemoveAll.username("reset-otp-test-user-remove-all");
        userRemoveAll.secret(removeAllCredential1);
        userRemoveAll.secret(removeAllCredential2);

        RealmBuilder.edit(testRealm).user(userRemoveNone.build()).user(userRemoveOne.build()).user(userRemoveAll.build());

        realmResource = adminClient.realm(testRealm.getRealm());
    }

    @After
    public void afterTest() {
        var realmRep = realmResource.toRepresentation();
        realmRep.setResetCredentialsFlow(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW);
        realmResource.update(realmRep);

        flowId = null;
        resetOtpExecutionId = null;
        resetOtpConfigId = null;
    }

    @Test
    public void noOtpIsRemovedOnResetWithoutConfig_LegacyBehaviour() {
        createOrChangeResetOtpFlowConfig(null);

        var userRep = realmResource.users().search(RESET_OTP_TEST_USER_REMOVE_NONE).get(0);
        var credentials = realmResource.users().get(userRep.getId()).credentials();
        var otpCredentials = credentials.stream().filter(credentialRep -> "otp".equals(credentialRep.getType()))
                .collect(Collectors.toList());

        assertTrue(otpCredentials.size() == 1);

        loginPage.open();
        loginPage.resetPassword();
        resetPasswordPage.changePassword(RESET_OTP_TEST_USER_REMOVE_NONE);

        credentials = realmResource.users().get(userRep.getId()).credentials();
        otpCredentials = credentials.stream().filter(credentialRep -> "otp".equals(credentialRep.getType()))
                .collect(Collectors.toList());

        assertTrue(otpCredentials.size() == 1);
    }

    @Test
    public void noOtpIsRemovedOnResetWithConfig() {
        createOrChangeResetOtpFlowConfig("Remove none");

        var userRep = realmResource.users().search(RESET_OTP_TEST_USER_REMOVE_NONE).get(0);
        var credentials = realmResource.users().get(userRep.getId()).credentials();
        var otpCredentials = credentials.stream().filter(credentialRep -> "otp".equals(credentialRep.getType()))
                .collect(Collectors.toList());

        assertTrue(otpCredentials.size() == 1);

        loginPage.open();
        loginPage.resetPassword();
        resetPasswordPage.changePassword(RESET_OTP_TEST_USER_REMOVE_NONE);

        credentials = realmResource.users().get(userRep.getId()).credentials();
        otpCredentials = credentials.stream().filter(credentialRep -> "otp".equals(credentialRep.getType()))
                .collect(Collectors.toList());

        assertTrue(otpCredentials.size() == 1);
    }

    @Test
    public void removeOneSpecificOtpOnReset() {
        createOrChangeResetOtpFlowConfig("Remove one");

        var userRep = realmResource.users().search(RESET_OTP_TEST_USER_REMOVE_ONE).get(0);
        var credentials = realmResource.users().get(userRep.getId()).credentials();
        var otpCredentials = credentials.stream().filter(credentialRep -> "otp".equals(credentialRep.getType()))
                .collect(Collectors.toList());

        assertTrue(otpCredentials.size() == 2);

        loginPage.open();
        loginPage.resetPassword();
        resetPasswordPage.changePassword(RESET_OTP_TEST_USER_REMOVE_ONE);
        resetOtpPage.selectOtp(1);
        resetOtpPage.submitOtpReset();

        credentials = realmResource.users().get(userRep.getId()).credentials();
        otpCredentials = credentials.stream().filter(credentialRep -> "otp".equals(credentialRep.getType()))
                .collect(Collectors.toList());

        assertTrue(otpCredentials.size() == 1);
        // Since we selected to remove the second OTP, the first one should still be there.
        assertTrue("Otp1-RemoveOne".equals(otpCredentials.get(0).getId()));

        loginPage.open();
        loginPage.resetPassword();
        resetPasswordPage.changePassword(RESET_OTP_TEST_USER_REMOVE_ONE);
        // Here the last remaining OTP is selected automatically.
        resetOtpPage.isCurrent();
        resetOtpPage.submitOtpReset();

        credentials = realmResource.users().get(userRep.getId()).credentials();
        otpCredentials = credentials.stream().filter(credentialRep -> "otp".equals(credentialRep.getType()))
                .collect(Collectors.toList());

        assertTrue(otpCredentials.isEmpty());
    }

    @Test
    public void removeAllOtpsOnReset() {
        createOrChangeResetOtpFlowConfig("Remove all");

        var userRep = realmResource.users().search(RESET_OTP_TEST_USER_REMOVE_ALL).get(0);
        var credentials = realmResource.users().get(userRep.getId()).credentials();
        var otpCredentials = credentials.stream().filter(credentialRep -> "otp".equals(credentialRep.getType()))
                .collect(Collectors.toList());

        assertTrue(otpCredentials.size() == 2);

        loginPage.open();
        loginPage.resetPassword();
        resetPasswordPage.changePassword(RESET_OTP_TEST_USER_REMOVE_ALL);

        credentials = realmResource.users().get(userRep.getId()).credentials();
        otpCredentials = credentials.stream().filter(credentialRep -> "otp".equals(credentialRep.getType()))
                .collect(Collectors.toList());

        assertTrue(otpCredentials.isEmpty());
    }

    private void createOrChangeResetOtpFlowConfig(String configOption) {
        /*
         * You can't do the flow/authenticator setup inside the configureTestRealm() method because the LegacyExportImportManager
         * won't import the default flows if there is already an authentication flow present (no matter which one it is) inside the realm representation
         * (the importAuthenticationFlows() method will skip the migrateFlows() step).
         */
        if (flowId == null) {
            var resetOtpFlow = new AuthenticationFlowRepresentation();

            resetOtpFlow.setAlias(FLOW_ALIAS);
            resetOtpFlow.setProviderId("basic-flow");
            resetOtpFlow.setBuiltIn(false);
            resetOtpFlow.setTopLevel(true);

            flowId = CreatedResponseUtil.getCreatedId(realmResource.flows().createFlow(resetOtpFlow));

            var chooseUserExecutionRep = new AuthenticationExecutionRepresentation();
            chooseUserExecutionRep.setParentFlow(flowId);
            chooseUserExecutionRep.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED.name());
            chooseUserExecutionRep.setAuthenticator("reset-credentials-choose-user");

            var conditionalUserConfiguredExecutionRep = new AuthenticationExecutionRepresentation();
            conditionalUserConfiguredExecutionRep.setParentFlow(flowId);
            conditionalUserConfiguredExecutionRep.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED.name());
            conditionalUserConfiguredExecutionRep.setAuthenticator("conditional-user-configured");

            var resetOtpExecutionRep = new AuthenticationExecutionRepresentation();
            resetOtpExecutionRep.setParentFlow(flowId);
            resetOtpExecutionRep.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED.name());
            resetOtpExecutionRep.setAuthenticator("reset-otp");

            realmResource.flows().addExecution(chooseUserExecutionRep);
            realmResource.flows().addExecution(conditionalUserConfiguredExecutionRep);

            resetOtpExecutionId = CreatedResponseUtil.getCreatedId(realmResource.flows().addExecution(resetOtpExecutionRep));

            var realmRep = realmResource.toRepresentation();
            realmRep.setResetCredentialsFlow(FLOW_ALIAS);
            realmRep.setResetPasswordAllowed(true);

            realmResource.update(realmRep);
        }

        var resetOtpAuthConfigRep = new AuthenticatorConfigRepresentation();
        resetOtpAuthConfigRep.setAlias("ResetOtpConfig");

        if (configOption == null) {
            if (resetOtpConfigId != null) {
                realmResource.flows().removeAuthenticatorConfig(resetOtpConfigId);
            }
            return;
        }

        resetOtpAuthConfigRep.setConfig(Map.of("action_on_otp_reset_flag", configOption));

        if(resetOtpConfigId == null) {
            resetOtpConfigId = CreatedResponseUtil
                    .getCreatedId(realmResource.flows().newExecutionConfig(resetOtpExecutionId, resetOtpAuthConfigRep));
        }
        else {
            realmResource.flows().updateAuthenticatorConfig(resetOtpConfigId, resetOtpAuthConfigRep);
        }

        getCleanup().addAuthenticationFlowId(flowId);
    }
}
