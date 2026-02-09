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

package org.keycloak.tests.admin.authentication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.utils.KerberosUtils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@KeycloakIntegrationTest
public class InitialFlowsTest extends AbstractAuthenticationTest {

    private HashMap<String, AuthenticatorConfigRepresentation> configs = new HashMap<>();
    private HashMap<String, AuthenticatorConfigRepresentation> expectedConfigs = new HashMap<>();

    {
        expectedConfigs.put("idp-review-profile", newConfig("review profile config", new String[]{"update.profile.on.first.login", "missing"}));
        expectedConfigs.put("idp-create-user-if-unique", newConfig("create unique user config", new String[]{"require.password.update.after.registration", "false"}));
    }

    @Test
    public void testInitialFlows() {

        List<FlowExecutions> result = new LinkedList<>();

        // get all flows
        List<AuthenticationFlowRepresentation> flows = authMgmtResource.getFlows();
        for (AuthenticationFlowRepresentation flow : flows) {
            // get all executions for flow
            List<AuthenticationExecutionInfoRepresentation> executionReps = authMgmtResource.getExecutions(flow.getAlias());

            for (AuthenticationExecutionInfoRepresentation exec : executionReps) {
                // separately load referenced configurations
                String configId = exec.getAuthenticationConfig();
                if (configId != null && !configs.containsKey(configId)) {
                    configs.put(configId, authMgmtResource.getAuthenticatorConfig(configId));
                }
            }
            result.add(new FlowExecutions(flow, executionReps));
        }

        // make sure received flows and their details are as expected
        compare(expectedFlows(), orderAlphabetically(result));
    }

    private void compare(List<FlowExecutions> expected, List<FlowExecutions> actual) {
        //Assert.assertEquals("Flow count", expected.size(), actual.size());
        Iterator<FlowExecutions> it1 = expected.iterator();
        Iterator<FlowExecutions> it2 = actual.iterator();
        while (it1.hasNext()) {
            FlowExecutions fe1 = it1.next();
            FlowExecutions fe2 = it2.next();

            expectedConfigs.put("conditional-credential", newConfig(
                    "browser".equals(fe1.flow.getAlias()) ? "browser-conditional-credential" : "first-broker-login-conditional-credential",
                    new String[]{"credentials", WebAuthnCredentialModel.TYPE_PASSWORDLESS}));
            compareFlows(fe1.flow, fe2.flow);
            compareExecutionsInfo(fe1.executions, fe2.executions);
        }
    }


    private void compareExecutionsInfo(List<AuthenticationExecutionInfoRepresentation> expected, List<AuthenticationExecutionInfoRepresentation> actual) {
        Assertions.assertEquals(expected.size(), actual.size(), "Executions count");
        Iterator<AuthenticationExecutionInfoRepresentation> it1 = expected.iterator();
        Iterator<AuthenticationExecutionInfoRepresentation> it2 = actual.iterator();
        while (it1.hasNext()) {
            AuthenticationExecutionInfoRepresentation exe1 = it1.next();
            AuthenticationExecutionInfoRepresentation exe2 = it2.next();

            compareExecutionWithConfig(exe1, exe2);
        }
    }

    private void compareExecutionWithConfig(AuthenticationExecutionInfoRepresentation expected, AuthenticationExecutionInfoRepresentation actual) {
        super.compareExecution(expected, actual);
        compareAuthConfig(expected, actual);
    }

    private void compareAuthConfig(AuthenticationExecutionInfoRepresentation expected, AuthenticationExecutionInfoRepresentation actual) {
        AuthenticatorConfigRepresentation cfg1 = expectedConfigs.get(expected.getProviderId());
        AuthenticatorConfigRepresentation cfg2 = configs.get(actual.getAuthenticationConfig());

        if (cfg1 == null && cfg2 == null) {
            return;
        }
        Assertions.assertEquals(cfg1.getAlias(), cfg2.getAlias(), "Execution configuration alias");
        Assertions.assertEquals(cfg1.getConfig(), cfg2.getConfig(), "Execution configuration params");
    }

    private List<FlowExecutions> orderAlphabetically(List<FlowExecutions> result) {
        List<FlowExecutions> sorted = new ArrayList<>(result);
        Collections.sort(sorted);
        return sorted;
    }

    private LinkedList<FlowExecutions> expectedFlows() {
        LinkedList<FlowExecutions> expected = new LinkedList<>();
        String[] kerberosAuthExpectedChoices = KerberosUtils.isKerberosSupportExpected() ? new String[]{REQUIRED, ALTERNATIVE, DISABLED} : new String[]{DISABLED};

        AuthenticationFlowRepresentation flow = newFlow("browser", "Browser based authentication", "basic-flow", true, true);
        addExecExport(flow, null, false, "auth-cookie", false, null, ALTERNATIVE, 10);
        addExecExport(flow, null, false, "auth-spnego", false, null, DISABLED, 20);
        addExecExport(flow, null, false, "identity-provider-redirector", false, null, ALTERNATIVE, 25);
        addExecExport(flow, "Organization", false, null, true, null, ALTERNATIVE, 26);
        addExecExport(flow, "forms", false, null, true, null, ALTERNATIVE, 30);

        List<AuthenticationExecutionInfoRepresentation> execs = new LinkedList<>();
        addExecInfo(execs, "Cookie", "auth-cookie", false, 0, 0, ALTERNATIVE, null, new String[]{REQUIRED, ALTERNATIVE, DISABLED}, 10);
        addExecInfo(execs, "Kerberos", "auth-spnego", false, 0, 1, DISABLED, null, kerberosAuthExpectedChoices, 20);
        addExecInfo(execs, "Identity Provider Redirector", "identity-provider-redirector", true, 0, 2, ALTERNATIVE, null, new String[]{REQUIRED, ALTERNATIVE, DISABLED}, 25);
        addExecInfo(execs, "Organization", null, false, 0, 3, ALTERNATIVE, true, new String[]{REQUIRED, ALTERNATIVE, DISABLED, CONDITIONAL}, 26);
        addExecInfo(execs, "Browser - Conditional Organization", null, false, 1, 0, CONDITIONAL, true, new String[]{REQUIRED, ALTERNATIVE, DISABLED, CONDITIONAL}, 10);
        addExecInfo(execs, "Condition - user configured", "conditional-user-configured", false, 2, 0, REQUIRED, null, new String[]{REQUIRED, DISABLED}, 10);
        addExecInfo(execs, "Organization Identity-First Login", "organization", true, 2, 1, ALTERNATIVE, null, new String[]{REQUIRED, ALTERNATIVE, DISABLED}, 20);
        addExecInfo(execs, "forms", null, false, 0, 4, ALTERNATIVE, true, new String[]{REQUIRED, ALTERNATIVE, DISABLED, CONDITIONAL}, 30);
        addExecInfo(execs, "Username Password Form", "auth-username-password-form", false, 1, 0, REQUIRED, null, new String[]{REQUIRED}, 10);
        addExecInfo(execs, "Browser - Conditional 2FA", null, false, 1, 1, CONDITIONAL, true, new String[]{REQUIRED, ALTERNATIVE, DISABLED, CONDITIONAL}, 20);
        addExecInfo(execs, "Condition - user configured", "conditional-user-configured", false, 2, 0, REQUIRED, null, new String[]{REQUIRED, DISABLED}, 10);
        addExecInfo(execs, "Condition - credential", "conditional-credential", true, 2, 1, REQUIRED, null, new String[]{REQUIRED, DISABLED}, 20);
        addExecInfo(execs, "OTP Form", "auth-otp-form", false, 2, 2, ALTERNATIVE, null, new String[]{REQUIRED, ALTERNATIVE, DISABLED}, 30);
        addExecInfo(execs, "WebAuthn Authenticator", "webauthn-authenticator", false, 2, 3, DISABLED, null, new String[]{REQUIRED, ALTERNATIVE, DISABLED}, 40);
        addExecInfo(execs, "Recovery Authentication Code Form", "auth-recovery-authn-code-form", false, 2, 4, DISABLED, null, new String[]{REQUIRED, ALTERNATIVE, DISABLED}, 50);
        expected.add(new FlowExecutions(flow, execs));

        flow = newFlow("clients", "Base authentication for clients", "client-flow", true, true);
        addExecExport(flow, null, false, "client-secret", false, null, ALTERNATIVE, 10);
        addExecExport(flow, null, false, "client-jwt", false, null, ALTERNATIVE, 20);
        addExecExport(flow, null, false, "client-secret-jwt", false, null, ALTERNATIVE, 30);
        addExecExport(flow, null, false, "client-x509", false, null, ALTERNATIVE, 40);

        execs = new LinkedList<>();
        addExecInfo(execs, "Client Id and Secret", "client-secret", false, 0, 0, ALTERNATIVE, null, new String[]{REQUIRED, ALTERNATIVE, DISABLED}, 10);
        addExecInfo(execs, "Signed JWT", "client-jwt", false, 0, 1, ALTERNATIVE, null, new String[]{REQUIRED, ALTERNATIVE, DISABLED}, 20);
        addExecInfo(execs, "Signed JWT with Client Secret", "client-secret-jwt", false, 0, 2, ALTERNATIVE, null, new String[]{REQUIRED, ALTERNATIVE, DISABLED}, 30);
        addExecInfo(execs, "X509 Certificate", "client-x509", false, 0, 3, ALTERNATIVE, null, new String[]{REQUIRED, ALTERNATIVE, DISABLED}, 40);
        expected.add(new FlowExecutions(flow, execs));

        flow = newFlow("direct grant", "OpenID Connect Resource Owner Grant", "basic-flow", true, true);
        addExecExport(flow, null, false, "direct-grant-validate-username", false, null, REQUIRED, 10);
        addExecExport(flow, null, false, "direct-grant-validate-password", false, null, REQUIRED, 20);
        addExecExport(flow, "Direct Grant - Conditional OTP", false, null, true, null, CONDITIONAL, 30);

        execs = new LinkedList<>();
        addExecInfo(execs, "Username Validation", "direct-grant-validate-username", false, 0, 0, REQUIRED, null, new String[]{REQUIRED}, 10);
        addExecInfo(execs, "Password", "direct-grant-validate-password", false, 0, 1, REQUIRED, null, new String[]{REQUIRED, ALTERNATIVE,DISABLED}, 20);
        addExecInfo(execs, "Direct Grant - Conditional OTP", null, false, 0, 2, CONDITIONAL, true, new String[]{REQUIRED, ALTERNATIVE, DISABLED, CONDITIONAL}, 30);
        addExecInfo(execs, "Condition - user configured", "conditional-user-configured", false, 1, 0, REQUIRED, null, new String[]{REQUIRED, DISABLED}, 10);
        addExecInfo(execs, "OTP", "direct-grant-validate-otp", false, 1, 1, REQUIRED, null, new String[]{REQUIRED, ALTERNATIVE, DISABLED}, 20);
        expected.add(new FlowExecutions(flow, execs));

        flow = newFlow("docker auth", "Used by Docker clients to authenticate against the IDP", "basic-flow", true, true);
        addExecExport(flow, null, false, "docker-http-basic-authenticator", false, null, REQUIRED, 10);

        execs = new LinkedList<>();
        addExecInfo(execs, "Docker Authenticator", "docker-http-basic-authenticator", false, 0, 0, REQUIRED, null, new String[]{REQUIRED}, 10);
        expected.add(new FlowExecutions(flow, execs));

        flow = newFlow("first broker login", "Actions taken after first broker login with identity provider account, which is not yet linked to any Keycloak account",
                "basic-flow", true, true);
        addExecExport(flow, null, false, "idp-review-profile", false, "review profile config", REQUIRED, 10);
        addExecExport(flow, "User creation or linking", false, null, true, null, REQUIRED, 20);
        addExecExport(flow, "First Broker Login - Conditional Organization", false, null, true, null, CONDITIONAL, 60);

        execs = new LinkedList<>();
        addExecInfo(execs, "Review Profile", "idp-review-profile", true, 0, 0, REQUIRED, null, new String[]{REQUIRED, ALTERNATIVE, DISABLED}, 10);
        addExecInfo(execs, "User creation or linking", null, false, 0, 1, REQUIRED, true, new String[]{REQUIRED, ALTERNATIVE, DISABLED, CONDITIONAL}, 20);
        addExecInfo(execs, "Create User If Unique", "idp-create-user-if-unique", true, 1, 0, ALTERNATIVE, null, new String[]{REQUIRED, ALTERNATIVE,  DISABLED}, 10);
        addExecInfo(execs, "Handle Existing Account", null, false, 1, 1, ALTERNATIVE, true, new String[]{REQUIRED, ALTERNATIVE, DISABLED, CONDITIONAL}, 20);
        addExecInfo(execs, "Confirm link existing account", "idp-confirm-link", false, 2, 0, REQUIRED, null, new String[]{REQUIRED, ALTERNATIVE, DISABLED}, 10);
        addExecInfo(execs, "Account verification options", null, false, 2, 1, REQUIRED, true, new String[]{REQUIRED, ALTERNATIVE, DISABLED, CONDITIONAL}, 20);
        addExecInfo(execs, "Verify existing account by Email", "idp-email-verification", false, 3, 0, ALTERNATIVE, null, new String[]{REQUIRED, ALTERNATIVE, DISABLED}, 10);
        addExecInfo(execs, "Verify Existing Account by Re-authentication", null, false, 3, 1, ALTERNATIVE, true, new String[]{REQUIRED, ALTERNATIVE, DISABLED, CONDITIONAL}, 20);
        addExecInfo(execs, "Username Password Form for identity provider reauthentication", "idp-username-password-form", false, 4, 0, REQUIRED, null, new String[]{REQUIRED}, 10);
        addExecInfo(execs, "First broker login - Conditional 2FA", null, false, 4, 1, CONDITIONAL, true, new String[]{REQUIRED, ALTERNATIVE, DISABLED, CONDITIONAL}, 20);
        addExecInfo(execs, "Condition - user configured", "conditional-user-configured", false, 5, 0, REQUIRED, null, new String[]{REQUIRED, DISABLED}, 10);
        addExecInfo(execs, "Condition - credential", "conditional-credential", true, 5, 1, REQUIRED, null, new String[]{REQUIRED, DISABLED}, 20);
        addExecInfo(execs, "OTP Form", "auth-otp-form", false, 5, 2, ALTERNATIVE, null, new String[]{REQUIRED, ALTERNATIVE, DISABLED}, 30);
        addExecInfo(execs, "WebAuthn Authenticator", "webauthn-authenticator", false, 5, 3, DISABLED, null, new String[]{REQUIRED, ALTERNATIVE, DISABLED}, 40);
        addExecInfo(execs, "Recovery Authentication Code Form", "auth-recovery-authn-code-form", false, 5, 4, DISABLED, null, new String[]{REQUIRED, ALTERNATIVE, DISABLED}, 50);
        addExecInfo(execs, "First Broker Login - Conditional Organization", null, false, 0, 2, CONDITIONAL, true, new String[]{REQUIRED, ALTERNATIVE, DISABLED, CONDITIONAL}, 60);
        addExecInfo(execs, "Condition - user configured", "conditional-user-configured", false, 1, 0, REQUIRED, null, new String[]{REQUIRED, DISABLED}, 10);
        addExecInfo(execs, "Organization Member Onboard", "idp-add-organization-member", false, 1, 1, REQUIRED, null, new String[]{REQUIRED, ALTERNATIVE, DISABLED}, 20);
        expected.add(new FlowExecutions(flow, execs));

        flow = newFlow("registration", "Registration flow", "basic-flow", true, true);
        addExecExport(flow, "registration form", false, "registration-page-form", true, null, REQUIRED, 10);

        execs = new LinkedList<>();
        addExecInfo(execs, "registration form", "registration-page-form", false, 0, 0, REQUIRED, true, new String[]{REQUIRED, DISABLED}, 10);
        addExecInfo(execs, "Registration User Profile Creation", "registration-user-creation", false, 1, 0, REQUIRED, null, new String[]{REQUIRED, DISABLED}, 20);
        addExecInfo(execs, "Password Validation", "registration-password-action", false, 1, 1, REQUIRED, null, new String[]{REQUIRED, DISABLED}, 50);
        addExecInfo(execs, "reCAPTCHA", "registration-recaptcha-action", true, 1, 2, DISABLED, null, new String[]{REQUIRED, DISABLED}, 60);
        addExecInfo(execs, "Terms and conditions", "registration-terms-and-conditions", false, 1, 3, DISABLED, null, new String[]{REQUIRED, DISABLED}, 70);
        expected.add(new FlowExecutions(flow, execs));

        flow = newFlow("reset credentials", "Reset credentials for a user if they forgot their password or something", "basic-flow", true, true);
        addExecExport(flow, null, false, "reset-credentials-choose-user", false, null, REQUIRED, 10);
        addExecExport(flow, null, false, "reset-credential-email", false, null, REQUIRED, 20);
        addExecExport(flow, null, false, "reset-password", false, null, REQUIRED, 30);
        addExecExport(flow, "Reset - Conditional OTP", false, null, true, null, CONDITIONAL, 40);

        execs = new LinkedList<>();
        addExecInfo(execs, "Choose User", "reset-credentials-choose-user", false, 0, 0, REQUIRED, null, new String[]{REQUIRED}, 10);
        addExecInfo(execs, "Send Reset Email", "reset-credential-email", true, 0, 1, REQUIRED, null, new String[]{REQUIRED}, 20);
        addExecInfo(execs, "Reset Password", "reset-password", false, 0, 2, REQUIRED, null, new String[]{REQUIRED, ALTERNATIVE, DISABLED}, 30);
        addExecInfo(execs, "Reset - Conditional OTP", null, false, 0, 3, CONDITIONAL, true, new String[]{REQUIRED, ALTERNATIVE, DISABLED, CONDITIONAL}, 40);
        addExecInfo(execs, "Condition - user configured", "conditional-user-configured", false, 1, 0, REQUIRED, null, new String[]{REQUIRED, DISABLED}, 10);
        addExecInfo(execs, "Reset OTP", "reset-otp", true, 1, 1, REQUIRED, null, new String[]{REQUIRED, ALTERNATIVE, DISABLED}, 20);
        expected.add(new FlowExecutions(flow, execs));

        return expected;
    }

    private void addExecExport(AuthenticationFlowRepresentation flow, String flowAlias, boolean userSetupAllowed,
                               String authenticator, boolean authenticatorFlow, String authenticatorConfig,
                               String requirement, int priority) {

        AuthenticationExecutionExportRepresentation rep = newExecutionExportRepresentation(flowAlias, userSetupAllowed,
                authenticator, authenticatorFlow, authenticatorConfig, requirement, priority);

        List<AuthenticationExecutionExportRepresentation> execs = flow.getAuthenticationExecutions();
        if (execs == null) {
            execs = new ArrayList<>();
            flow.setAuthenticationExecutions(execs);
        }
        execs.add(rep);
    }

    private AuthenticationExecutionExportRepresentation newExecutionExportRepresentation(String flowAlias, boolean userSetupAllowed, String authenticator, boolean authenticatorFlow, String authenticatorConfig, String requirement, int priority) {
        AuthenticationExecutionExportRepresentation rep = new AuthenticationExecutionExportRepresentation();
        rep.setFlowAlias(flowAlias);
        rep.setUserSetupAllowed(userSetupAllowed);
        rep.setAuthenticator(authenticator);
        rep.setAuthenticatorFlow(authenticatorFlow);
        rep.setAuthenticatorConfig(authenticatorConfig);
        rep.setRequirement(requirement);
        rep.setPriority(priority);
        return rep;
    }

    private static class FlowExecutions implements Comparable<FlowExecutions> {
        AuthenticationFlowRepresentation flow;
        List<AuthenticationExecutionInfoRepresentation> executions;

        FlowExecutions(AuthenticationFlowRepresentation flow, List<AuthenticationExecutionInfoRepresentation> executions) {
            this.flow = flow;
            this.executions = executions;
        }

        @Override
        public int compareTo(FlowExecutions o) {
            return flow.getAlias().compareTo(o.flow.getAlias());
        }
    }
}
