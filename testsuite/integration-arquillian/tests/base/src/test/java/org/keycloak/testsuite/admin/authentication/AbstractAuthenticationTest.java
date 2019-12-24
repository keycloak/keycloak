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
import org.junit.Before;
import org.junit.Rule;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.AdminEventPaths;
import org.keycloak.testsuite.util.AssertAdminEvents;
import org.keycloak.testsuite.util.RealmBuilder;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;


/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public abstract class AbstractAuthenticationTest extends AbstractKeycloakTest {

    static final String REALM_NAME = "test";

    static final String REQUIRED = "REQUIRED";
    static final String CONDITIONAL = "CONDITIONAL";
    static final String DISABLED = "DISABLED";
    static final String ALTERNATIVE = "ALTERNATIVE";

    RealmResource realmResource;
    AuthenticationManagementResource authMgmtResource;

    @Rule
    public AssertAdminEvents assertAdminEvents = new AssertAdminEvents(this);

    @Before
    public void before() {
        realmResource = adminClient.realms().realm(REALM_NAME);
        authMgmtResource = realmResource.flows();
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealmRep = RealmBuilder.create().name(REALM_NAME).testEventListener().build();
        testRealmRep.setId(REALM_NAME);
        testRealms.add(testRealmRep);
    }


    public static AuthenticationExecutionInfoRepresentation findExecutionByProvider(String provider, List<AuthenticationExecutionInfoRepresentation> executions) {
        for (AuthenticationExecutionInfoRepresentation exec : executions) {
            if (provider.equals(exec.getProviderId())) {
                return exec;
            }
        }
        return null;
    }


    public static AuthenticationFlowRepresentation findFlowByAlias(String alias, List<AuthenticationFlowRepresentation> flows) {
        for (AuthenticationFlowRepresentation flow : flows) {
            if (alias.equals(flow.getAlias())) {
                return flow;
            }
        }
        return null;
    }

    void compareExecution(AuthenticationExecutionInfoRepresentation expected, AuthenticationExecutionInfoRepresentation actual) {
        Assert.assertEquals("Execution requirement - " + actual.getProviderId(), expected.getRequirement(), actual.getRequirement());
        Assert.assertEquals("Execution display name - " + actual.getProviderId(), expected.getDisplayName(), actual.getDisplayName());
        Assert.assertEquals("Execution configurable - " + actual.getProviderId(), expected.getConfigurable(), actual.getConfigurable());
        Assert.assertEquals("Execution provider id - " + actual.getProviderId(), expected.getProviderId(), actual.getProviderId());
        Assert.assertEquals("Execution level - " + actual.getProviderId(), expected.getLevel(), actual.getLevel());
        Assert.assertEquals("Execution index - " + actual.getProviderId(), expected.getIndex(), actual.getIndex());
        Assert.assertEquals("Execution authentication flow - " + actual.getProviderId(), expected.getAuthenticationFlow(), actual.getAuthenticationFlow());
        Assert.assertEquals("Execution requirement choices - " + actual.getProviderId(), expected.getRequirementChoices(), actual.getRequirementChoices());
    }

    void compareExecution(AuthenticationExecutionExportRepresentation expected, AuthenticationExecutionExportRepresentation actual) {
        Assert.assertEquals("Execution flowAlias - " + actual.getFlowAlias(), expected.getFlowAlias(), actual.getFlowAlias());
        Assert.assertEquals("Execution authenticator - " + actual.getAuthenticator(), expected.getAuthenticator(), actual.getAuthenticator());
        Assert.assertEquals("Execution userSetupAllowed - " + actual.getAuthenticator(), expected.isUserSetupAllowed(), actual.isUserSetupAllowed());
        Assert.assertEquals("Execution authenticatorFlow - " + actual.getAuthenticator(), expected.isAutheticatorFlow(), actual.isAutheticatorFlow());
        Assert.assertEquals("Execution authenticatorConfig - " + actual.getAuthenticator(), expected.getAuthenticatorConfig(), actual.getAuthenticatorConfig());
        Assert.assertEquals("Execution priority - " + actual.getAuthenticator(), expected.getPriority(), actual.getPriority());
        Assert.assertEquals("Execution requirement - " + actual.getAuthenticator(), expected.getRequirement(), actual.getRequirement());
    }

    void compareExecutions(List<AuthenticationExecutionExportRepresentation> expected, List<AuthenticationExecutionExportRepresentation> actual) {
        Assert.assertNotNull("Executions should not be null", actual);
        Assert.assertEquals("Size", expected.size(), actual.size());

        for (int i = 0; i < expected.size(); i++) {
            compareExecution(expected.get(i), actual.get(i));
        }
    }

    void compareFlows(AuthenticationFlowRepresentation expected, AuthenticationFlowRepresentation actual) {
        Assert.assertEquals("Flow alias", expected.getAlias(), actual.getAlias());
        Assert.assertEquals("Flow description", expected.getDescription(), actual.getDescription());
        Assert.assertEquals("Flow providerId", expected.getProviderId(), actual.getProviderId());
        Assert.assertEquals("Flow top level", expected.isTopLevel(), actual.isTopLevel());
        Assert.assertEquals("Flow built-in", expected.isBuiltIn(), actual.isBuiltIn());

        List<AuthenticationExecutionExportRepresentation> expectedExecs = expected.getAuthenticationExecutions();
        List<AuthenticationExecutionExportRepresentation> actualExecs = actual.getAuthenticationExecutions();

        if (expectedExecs == null) {
            Assert.assertTrue("Executions should be null or empty", actualExecs == null || actualExecs.size() == 0);
        } else {
            compareExecutions(expectedExecs, actualExecs);
        }
    }

    AuthenticationFlowRepresentation newFlow(String alias, String description,
                                                       String providerId, boolean topLevel, boolean builtIn) {
        AuthenticationFlowRepresentation flow = new AuthenticationFlowRepresentation();
        flow.setAlias(alias);
        flow.setDescription(description);
        flow.setProviderId(providerId);
        flow.setTopLevel(topLevel);
        flow.setBuiltIn(builtIn);
        return flow;
    }

    AuthenticationExecutionInfoRepresentation newExecInfo(String displayName, String providerId, Boolean configurable,
                                                          int level, int index, String requirement, Boolean authFlow, String[] choices) {

        AuthenticationExecutionInfoRepresentation execution = new AuthenticationExecutionInfoRepresentation();
        execution.setRequirement(requirement);
        execution.setDisplayName(displayName);
        execution.setConfigurable(configurable);
        execution.setProviderId(providerId);
        execution.setLevel(level);
        execution.setIndex(index);
        execution.setAuthenticationFlow(authFlow);
        if (choices != null) {
            execution.setRequirementChoices(Arrays.asList(choices));
        }
        return execution;
    }

    void addExecInfo(List<AuthenticationExecutionInfoRepresentation> target, String displayName, String providerId, Boolean configurable,
                 int level, int index, String requirement, Boolean authFlow, String[] choices) {

        AuthenticationExecutionInfoRepresentation exec = newExecInfo(displayName, providerId, configurable, level, index, requirement, authFlow, choices);
        target.add(exec);
    }

    AuthenticatorConfigRepresentation newConfig(String alias, String[] keyvalues) {
        AuthenticatorConfigRepresentation config = new AuthenticatorConfigRepresentation();
        config.setAlias(alias);

        if (keyvalues == null) {
            throw new IllegalArgumentException("keyvalues == null");
        }
        if (keyvalues.length % 2 != 0) {
            throw new IllegalArgumentException("keyvalues should have even number of elements");
        }

        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        for (int i = 0; i < keyvalues.length; i += 2) {
            params.put(keyvalues[i], keyvalues[i + 1]);
        }
        config.setConfig(params);
        return config;
    }

    void createFlow(AuthenticationFlowRepresentation flowRep) {
        Response response = authMgmtResource.createFlow(flowRep);
        org.keycloak.testsuite.Assert.assertEquals(201, response.getStatus());
        response.close();
        String flowId = ApiUtil.getCreatedId(response);
        getCleanup().addAuthenticationFlowId(flowId);
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.CREATE, AssertAdminEvents.isExpectedPrefixFollowedByUuid(AdminEventPaths.authFlowsPath()), flowRep, ResourceType.AUTH_FLOW);
    }
}
