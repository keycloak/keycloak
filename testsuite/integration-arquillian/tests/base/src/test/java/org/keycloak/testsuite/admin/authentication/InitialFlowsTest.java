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

package org.keycloak.testsuite.admin.authentication;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
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
            Response executions = authMgmtResource.getExecutions(flow.getAlias());
            List<AuthenticationExecutionInfoRepresentation> executionReps = executions.readEntity(new GenericType<List<AuthenticationExecutionInfoRepresentation>>() {
            });

            for (AuthenticationExecutionInfoRepresentation exec : executionReps) {
                // separately load referenced configurations
                String configId = exec.getAuthenticationConfig();
                if (configId != null && !configs.containsKey(configId)) {
                    configs.put(configId, authMgmtResource.getAuthenticatorConfig(exec.getId(), configId));
                }
            }
            result.add(new FlowExecutions(flow, executionReps));
        }

        // make sure received flows and their details are as expected
        compare(expectedFlows(), orderAlphabetically(result));
    }

    private void compare(List<FlowExecutions> expected, List<FlowExecutions> actual) {
        Assert.assertEquals("Flow count", expected.size(), actual.size());
        Iterator<FlowExecutions> it1 = expected.iterator();
        Iterator<FlowExecutions> it2 = actual.iterator();
        while (it1.hasNext()) {
            FlowExecutions fe1 = it1.next();
            FlowExecutions fe2 = it2.next();

            compareFlows(fe1.flow, fe2.flow);
            compareExecutions(fe1.executions, fe2.executions);
        }
    }


    private void compareExecutions(List<AuthenticationExecutionInfoRepresentation> expected, List<AuthenticationExecutionInfoRepresentation> actual) {
        Assert.assertEquals("Executions count", expected.size(), actual.size());
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
        Assert.assertEquals("Execution configuration alias", cfg1.getAlias(), cfg2.getAlias());
        Assert.assertEquals("Execution configuration params", cfg1.getConfig(), cfg2.getConfig());
    }

    private List<FlowExecutions> orderAlphabetically(List<FlowExecutions> result) {
        List<FlowExecutions> sorted = new ArrayList<>(result);
        Collections.sort(sorted);
        return sorted;
    }

    private LinkedList<FlowExecutions> expectedFlows() {
        LinkedList<FlowExecutions> expected = new LinkedList<>();

        AuthenticationFlowRepresentation flow = newFlow("browser", "browser based authentication", "basic-flow", true, true);
        List<AuthenticationExecutionInfoRepresentation> executions = new LinkedList<>();
        executions.add(newExecution("Cookie", "auth-cookie", false, 0, 0, ALTERNATIVE, null, new String[]{ALTERNATIVE, DISABLED}));
        executions.add(newExecution("Kerberos", "auth-spnego", false, 0, 1, DISABLED, null, new String[]{ALTERNATIVE, REQUIRED, DISABLED}));
        executions.add(newExecution("forms", null, false, 0, 2, ALTERNATIVE, true, new String[]{ALTERNATIVE, REQUIRED, DISABLED}));
        executions.add(newExecution("Username Password Form", "auth-username-password-form", false, 1, 0, REQUIRED, null, new String[]{REQUIRED}));
        executions.add(newExecution("OTP Form", "auth-otp-form", false, 1, 1, OPTIONAL, null, new String[]{REQUIRED, OPTIONAL, DISABLED}));
        expected.add(new FlowExecutions(flow, executions));

        flow = newFlow("clients", "Base authentication for clients", "client-flow", true, true);
        executions = new LinkedList<>();
        executions.add(newExecution("Client Id and Secret", "client-secret", false, 0, 0, ALTERNATIVE, null, new String[]{ALTERNATIVE, DISABLED}));
        executions.add(newExecution("Signed Jwt", "client-jwt", false, 0, 1, ALTERNATIVE, null, new String[]{ALTERNATIVE, DISABLED}));
        expected.add(new FlowExecutions(flow, executions));

        flow = newFlow("direct grant", "OpenID Connect Resource Owner Grant", "basic-flow", true, true);
        executions = new LinkedList<>();
        executions.add(newExecution("Username Validation", "direct-grant-validate-username", false, 0, 0, REQUIRED, null, new String[]{REQUIRED}));
        executions.add(newExecution("Password", "direct-grant-validate-password", false, 0, 1, REQUIRED, null, new String[]{REQUIRED, DISABLED}));
        executions.add(newExecution("OTP", "direct-grant-validate-otp", false, 0, 2, OPTIONAL, null, new String[]{REQUIRED, OPTIONAL, DISABLED}));
        expected.add(new FlowExecutions(flow, executions));

        flow = newFlow("first broker login", "Actions taken after first broker login with identity provider account, which is not yet linked to any Keycloak account",
                "basic-flow", true, true);
        executions = new LinkedList<>();
        executions.add(newExecution("Review Profile", "idp-review-profile", true, 0, 0, REQUIRED, null, new String[]{REQUIRED, DISABLED}));
        executions.add(newExecution("Create User If Unique", "idp-create-user-if-unique", true, 0, 1, ALTERNATIVE, null, new String[]{ALTERNATIVE, REQUIRED, DISABLED}));
        executions.add(newExecution("Handle Existing Account", null, false, 0, 2, ALTERNATIVE, true, new String[]{ALTERNATIVE, REQUIRED, DISABLED}));
        executions.add(newExecution("Confirm link existing account", "idp-confirm-link", false, 1, 0, REQUIRED, null, new String[]{REQUIRED, DISABLED}));
        executions.add(newExecution("Verify existing account by Email", "idp-email-verification", false, 1, 1, ALTERNATIVE, null, new String[]{ALTERNATIVE, REQUIRED, DISABLED}));
        executions.add(newExecution("Verify Existing Account by Re-authentication", null, false, 1, 2, ALTERNATIVE, true, new String[]{ALTERNATIVE, REQUIRED, DISABLED}));
        executions.add(newExecution("Username Password Form for identity provider reauthentication", "idp-username-password-form", false, 2, 0, REQUIRED, null, new String[]{REQUIRED}));
        executions.add(newExecution("OTP Form", "auth-otp-form", false, 2, 1, OPTIONAL, null, new String[]{REQUIRED, OPTIONAL, DISABLED}));
        expected.add(new FlowExecutions(flow, executions));

        flow = newFlow("registration", "registration flow", "basic-flow", true, true);
        executions = new LinkedList<>();
        executions.add(newExecution("registration form", "registration-page-form", false, 0, 0, REQUIRED, true, new String[]{REQUIRED, DISABLED}));
        executions.add(newExecution("Registration User Creation", "registration-user-creation", false, 1, 0, REQUIRED, null, new String[]{REQUIRED, DISABLED}));
        executions.add(newExecution("Profile Validation", "registration-profile-action", false, 1, 1, REQUIRED, null, new String[]{REQUIRED, DISABLED}));
        executions.add(newExecution("Password Validation", "registration-password-action", false, 1, 2, REQUIRED, null, new String[]{REQUIRED, DISABLED}));
        executions.add(newExecution("Recaptcha", "registration-recaptcha-action", true, 1, 3, DISABLED, null, new String[]{REQUIRED, DISABLED}));
        expected.add(new FlowExecutions(flow, executions));

        flow = newFlow("reset credentials", "Reset credentials for a user if they forgot their password or something", "basic-flow", true, true);
        executions = new LinkedList<>();
        executions.add(newExecution("Choose User", "reset-credentials-choose-user", false, 0, 0, REQUIRED, null, new String[]{REQUIRED}));
        executions.add(newExecution("Send Reset Email", "reset-credential-email", false, 0, 1, REQUIRED, null, new String[]{REQUIRED}));
        executions.add(newExecution("Reset Password", "reset-password", false, 0, 2, REQUIRED, null, new String[]{REQUIRED, OPTIONAL, DISABLED}));
        executions.add(newExecution("Reset OTP", "reset-otp", false, 0, 3, OPTIONAL, null, new String[]{REQUIRED, OPTIONAL, DISABLED}));
        expected.add(new FlowExecutions(flow, executions));

        flow = newFlow("saml ecp", "SAML ECP Profile Authentication Flow", "basic-flow", true, true);
        executions = new LinkedList<>();
        executions.add(newExecution(null, "http-basic-authenticator", false, 0, 0, REQUIRED, null, new String[]{}));
        expected.add(new FlowExecutions(flow, executions));

        return expected;
    }

    static class FlowExecutions implements Comparable<FlowExecutions> {
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
