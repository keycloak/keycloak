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
package org.keycloak.testsuite.script;

import static org.junit.Assert.assertFalse;
import static org.keycloak.common.Profile.Feature.SCRIPTS;
import static org.keycloak.testsuite.arquillian.DeploymentTargetModifier.AUTH_SERVER_CURRENT;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.authentication.authenticators.browser.ScriptBasedAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.provider.ScriptProviderDescriptor;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.AbstractAuthenticationTest;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.forms.AbstractFlowTest;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.testsuite.util.ExecutionBuilder;
import org.keycloak.testsuite.util.FlowBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@EnableFeature(value = SCRIPTS, skipRestart = true)
public class DeployedScriptAuthenticatorTest extends AbstractFlowTest {

    public static final String EXECUTION_ID = "scriptAuth";
    private static final String SCRIPT_DEPLOYMENT_NAME = "scripts.jar";

    // Managed to make sure that archive is deployed once in @BeforeClass stage and undeployed once in @AfterClass stage
    @Deployment(name = SCRIPT_DEPLOYMENT_NAME, managed = true, testable = false)
    @TargetsContainer(AUTH_SERVER_CURRENT)
    public static JavaArchive deploy() throws IOException {
        ScriptProviderDescriptor representation = new ScriptProviderDescriptor();

        representation.addAuthenticator("My Authenticator", "authenticator-a.js");

        return ShrinkWrap.create(JavaArchive.class, SCRIPT_DEPLOYMENT_NAME)
                .addAsManifestResource(new StringAsset(JsonSerialization.writeValueAsPrettyString(representation)),
                        "keycloak-scripts.json")
                .addAsResource("scripts/authenticator-example.js", "authenticator-a.js");
    }

    @BeforeClass
    public static void verifyEnvironment() {
        ContainerAssume.assumeNotAuthServerUndertow();
    }

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected LoginPage loginPage;

    private AuthenticationFlowRepresentation flow;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        UserRepresentation failUser = UserBuilder.create()
                .id("fail")
                .username("fail")
                .email("fail@test.com")
                .enabled(true)
                .password("password")
                .build();

        UserRepresentation okayUser = UserBuilder.create()
                .id("user")
                .username("user")
                .email("user@test.com")
                .enabled(true)
                .password("password")
                .build();

        RealmBuilder.edit(testRealm)
                .user(failUser)
                .user(okayUser);
    }

    public void configureFlows() throws Exception {
        if (testContext.isInitialized()) {
            return;
        }

        String scriptFlow = "scriptBrowser";

        AuthenticationFlowRepresentation scriptBrowserFlow = FlowBuilder.create()
                .alias(scriptFlow)
                .description("dummy pass through registration")
                .providerId("basic-flow")
                .topLevel(true)
                .builtIn(false)
                .build();

        Response createFlowResponse = adminClient.realm(TEST_REALM_NAME).flows().createFlow(scriptBrowserFlow);
        Assert.assertEquals(201, createFlowResponse.getStatus());

        RealmRepresentation realm = adminClient.realm(TEST_REALM_NAME).toRepresentation();
        realm.setBrowserFlow(scriptFlow);
        realm.setDirectGrantFlow(scriptFlow);
        testRealm().update(realm);

        this.flow = findFlowByAlias(scriptFlow);

        AuthenticationExecutionRepresentation usernamePasswordFormExecution = ExecutionBuilder.create()
                .id("username password form")
                .parentFlow(this.flow.getId())
                .requirement(AuthenticationExecutionModel.Requirement.REQUIRED.name())
                .authenticator(UsernamePasswordFormFactory.PROVIDER_ID)
                .build();

        AuthenticationExecutionRepresentation authScriptExecution = ExecutionBuilder.create()
                .id(EXECUTION_ID)
                .parentFlow(this.flow.getId())
                .requirement(AuthenticationExecutionModel.Requirement.REQUIRED.name())
                .authenticator("script-authenticator-a.js")
                .build();

        Response addExecutionResponse = testRealm().flows().addExecution(usernamePasswordFormExecution);
        Assert.assertEquals(201, addExecutionResponse.getStatus());
        addExecutionResponse.close();

        addExecutionResponse = testRealm().flows().addExecution(authScriptExecution);
        Assert.assertEquals(201, addExecutionResponse.getStatus());
        addExecutionResponse.close();

        testContext.setInitialized(true);
    }

    /**
     * KEYCLOAK-3491
     */
    @Test
    public void loginShouldWorkWithScriptAuthenticator() throws Exception {
        configureFlows();

        loginPage.open();

        loginPage.login("user", "password");

        events.expectLogin().user(okayUser()).detail(Details.USERNAME, "user").assertEvent();
    }

    // Issue 20005
    @Test
    public void testManyScriptAuthenticatorInstances() throws Exception {
        configureFlows();
        AuthenticationManagementResource authMgmtResource = adminClient.realm(TEST_REALM_NAME).flows();

        // Endpoint used by admin console
        Map<String, Object> scriptExecution = new HashMap<>();
        scriptExecution.put("provider", "script-authenticator-a.js");

        // It should be possible to add another script-authenticator to the flow
        authMgmtResource.addExecution("scriptBrowser", scriptExecution);

        List<AuthenticationExecutionInfoRepresentation> executions = authMgmtResource.getExecutions("scriptBrowser");
        List<AuthenticationExecutionInfoRepresentation> scriptExecutions = executions.stream()
                .filter(execution -> execution.getDisplayName().equals("My Authenticator"))
                .collect(Collectors.toList());

        // Both executions refers to same config of deployed script provider
        Assert.assertEquals(2, scriptExecutions.size());
        for (AuthenticationExecutionInfoRepresentation execution : scriptExecutions) {
            Assert.assertEquals(execution.getAuthenticationConfig(), "script-authenticator-a.js");
        }

        // Assert updating config should fail due it's read-only
        AuthenticatorConfigRepresentation configRep = authMgmtResource.getAuthenticatorConfig("script-authenticator-a.js");
        configRep.getConfig().put("scriptCode", "Something");
        try {
            authMgmtResource.updateAuthenticatorConfig("script-authenticator-a.js", configRep);
            Assert.fail("Update of configuration should have failed");
        } catch (BadRequestException bre) {
            // Expected
        }

        // Test copy flow is OK
        Map<String, Object> newFlow = new HashMap<>();
        newFlow.put("newName", "Copy of script flow");
        Response resp = authMgmtResource.copy("scriptBrowser", newFlow);
        Assert.assertEquals(201, resp.getStatus());
        resp.close();
        AuthenticationFlowRepresentation copiedFlow = AbstractAuthenticationTest.findFlowByAlias("Copy of script flow", authMgmtResource.getFlows());

        // Cleanup
        authMgmtResource.deleteFlow(copiedFlow.getId());
        authMgmtResource.removeExecution(scriptExecutions.get(1).getId());
    }

    private UserRepresentation okayUser() {
        return adminClient.realm(TEST_REALM_NAME).users().search("user", true).get(0);
    }

    /**
     * KEYCLOAK-3491
     */
    @Test
    public void loginShouldFailWithScriptAuthenticator() throws Exception {
        configureFlows();

        loginPage.open();

        loginPage.login("fail", "password");

        events.expect(EventType.LOGIN_ERROR).user((String) null).error(Errors.USER_NOT_FOUND).assertEvent();
    }

    @Test
    @DisableFeature(value = SCRIPTS, executeAsLast = false, skipRestart = true)
    public void testScriptAuthenticatorNotAvailable() {
        assertFalse(testRealm().flows().getAuthenticatorProviders().stream().anyMatch(
                provider -> ScriptBasedAuthenticatorFactory.PROVIDER_ID.equals(provider.get("id"))));
    }
}
