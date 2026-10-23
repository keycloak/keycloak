/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.login;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.authentication.authenticators.access.AllowAccessAuthenticatorFactory;
import org.keycloak.authentication.authenticators.access.DenyAccessAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.PasswordFormFactory;
import org.keycloak.authentication.authenticators.browser.UsernameFormFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalGroupAuthenticatorFactory;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.GroupBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.LoginUsernamePage;
import org.keycloak.testframework.ui.page.PasswordPage;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.FlowUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.tests.admin.authentication.AbstractAuthenticationTest.findFlowByAlias;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@KeycloakIntegrationTest
public class ConditionalGroupAuthenticatorTest {

    private static final String FLOW_ALIAS = "browser - conditional group";
    private static final String ERROR_MESSAGE = "Access denied by group condition";
    private static final String PASSWORD = "password";

    private static final String MEMBER_USER = "group-member";
    private static final String NON_MEMBER_USER = "group-non-member";

    private static final String GROUP_NAME = "conditional-group";
    private static final String GROUP_PATH = "/" + GROUP_NAME;

    private static final String NESTED_PARENT = "conditional-parent";
    private static final String NESTED_CHILD = "conditional-child";
    private static final String NESTED_CHILD_PATH = "/" + NESTED_PARENT + "/" + NESTED_CHILD;

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectPage
    LoginUsernamePage usernamePage;

    @InjectPage
    PasswordPage passwordPage;

    @InjectPage
    ErrorPage errorPage;

    @InjectEvents
    Events events;

    private String memberUserId;
    private String nonMemberUserId;

    @BeforeEach
    public void setUpUsersAndGroups() {
        memberUserId = createUser(MEMBER_USER);
        nonMemberUserId = createUser(NON_MEMBER_USER);

        String groupId = createGroup(GROUP_NAME);
        realm.admin().users().get(memberUserId).joinGroup(groupId);
    }

    @Test
    public void testDenyAccessWhenUserIsGroupMember() {
        denyAccessWithGroupCondition(false, memberUserId, MEMBER_USER, nonMemberUserId, NON_MEMBER_USER);
    }

    @Test
    public void testDenyAccessWhenUserIsNotGroupMemberWithNegate() {
        denyAccessWithGroupCondition(true, nonMemberUserId, NON_MEMBER_USER, memberUserId, MEMBER_USER);
    }

    @Test
    public void testSkipConditionalSubflowWhenUserIsNotGroupMember() {
        configureBrowserFlowWithDenyAccessInConditionalFlow(GROUP_PATH, false);

        try {
            loginExpectingPasswordStep(NON_MEMBER_USER);
            EventAssertion.expectLoginSuccess(events.poll()).userId(nonMemberUserId).details(Details.USERNAME, NON_MEMBER_USER);
        } finally {
            revertBrowserFlow(FLOW_ALIAS);
        }
    }

    @Test
    public void testSkipConditionalSubflowWhenUserIsGroupMemberWithNegate() {
        configureBrowserFlowWithDenyAccessInConditionalFlow(GROUP_PATH, true);

        try {
            loginExpectingPasswordStep(MEMBER_USER);
            EventAssertion.expectLoginSuccess(events.poll()).userId(memberUserId).details(Details.USERNAME, MEMBER_USER);
        } finally {
            revertBrowserFlow(FLOW_ALIAS);
        }
    }

    @Test
    public void testNestedGroupPathMatchesChildMember() {
        String nestedMemberId = createUser("nested-group-member");
        String nestedGroupId = createNestedGroup();
        realm.admin().users().get(nestedMemberId).joinGroup(nestedGroupId);

        configureBrowserFlowWithDenyAccessInConditionalFlow(NESTED_CHILD_PATH, false);

        try {
            oauth.openLoginForm();
            usernamePage.assertCurrent();
            usernamePage.fillLoginWithUsernameOnly("nested-group-member");
            usernamePage.submit();

            errorPage.assertCurrent();
            assertThat(errorPage.getError(), is(ERROR_MESSAGE));

            EventAssertion.expectLoginError(events.poll())
                    .userId(null)
                    .sessionId(null)
                    .error(Errors.ACCESS_DENIED)
                    .details(Details.USERNAME, "nested-group-member");
        } finally {
            revertBrowserFlow(FLOW_ALIAS);
        }
    }

    @Test
    public void testInvalidGroupPathSkipsConditionalSubflow() {
        configureBrowserFlowWithDenyAccessInConditionalFlow("/non-existing-group", false);

        try {
            loginExpectingPasswordStep(MEMBER_USER);
            EventAssertion.expectLoginSuccess(events.poll()).userId(memberUserId).details(Details.USERNAME, MEMBER_USER);
        } finally {
            revertBrowserFlow(FLOW_ALIAS);
        }
    }

    @Test
    public void testDeletedGroupSkipsConditionalSubflow() {
        String groupId = createGroupId(GROUP_NAME);
        configureBrowserFlowWithDenyAccessInConditionalFlow(GROUP_PATH, false);
        realm.admin().groups().group(groupId).remove();

        try {
            loginExpectingPasswordStep(MEMBER_USER);
            EventAssertion.expectLoginSuccess(events.poll()).userId(memberUserId).details(Details.USERNAME, MEMBER_USER);
        } finally {
            revertBrowserFlow(FLOW_ALIAS);
        }
    }

    @Test
    public void testSkipPasswordWhenUserIsGroupMember() {
        configureBrowserFlowWithSkipExecutionInConditionalFlow(GROUP_PATH, false);

        try {
            oauth.openLoginForm();
            usernamePage.assertCurrent();
            usernamePage.fillLoginWithUsernameOnly(MEMBER_USER);
            usernamePage.submit();

            EventAssertion.expectLoginSuccess(events.poll()).userId(memberUserId).details(Details.USERNAME, MEMBER_USER);
        } finally {
            revertBrowserFlow(FLOW_ALIAS);
        }
    }

    private void denyAccessWithGroupCondition(boolean negateOutput, String deniedUserId, String deniedUsername,
            String allowedUserId, String allowedUsername) {
        configureBrowserFlowWithDenyAccessInConditionalFlow(GROUP_PATH, negateOutput);

        try {
            oauth.openLoginForm();
            usernamePage.assertCurrent();
            usernamePage.fillLoginWithUsernameOnly(deniedUsername);
            usernamePage.submit();

            errorPage.assertCurrent();
            assertThat(errorPage.getError(), is(ERROR_MESSAGE));

            EventAssertion.expectLoginError(events.poll())
                    .userId(null)
                    .sessionId(null)
                    .error(Errors.ACCESS_DENIED)
                    .details(Details.USERNAME, deniedUsername);

            oauth.openLoginForm();
            usernamePage.assertCurrent();
            usernamePage.fillLoginWithUsernameOnly(allowedUsername);
            usernamePage.submit();

            passwordPage.assertCurrent();
            passwordPage.fillPassword(PASSWORD);
            passwordPage.submit();

            EventAssertion.expectLoginSuccess(events.poll()).userId(allowedUserId).details(Details.USERNAME, allowedUsername);
        } finally {
            revertBrowserFlow(FLOW_ALIAS);
        }
    }

    private void loginExpectingPasswordStep(String username) {
        oauth.openLoginForm();
        usernamePage.assertCurrent();
        usernamePage.fillLoginWithUsernameOnly(username);
        usernamePage.submit();
        passwordPage.assertCurrent();
        passwordPage.fillPassword(PASSWORD);
        passwordPage.submit();
    }

    private void configureBrowserFlowWithDenyAccessInConditionalFlow(String groupPath, boolean negateOutput) {
        Map<String, String> conditionConfig = groupConditionConfig(groupPath, negateOutput);
        Map<String, String> denyConfig = Map.of(DenyAccessAuthenticatorFactory.ERROR_MESSAGE, ERROR_MESSAGE);
        configureBrowserFlowWithDenyAccessInConditionalFlow(conditionConfig, denyConfig);
    }

    private void configureBrowserFlowWithDenyAccessInConditionalFlow(Map<String, String> conditionConfig,
            Map<String, String> denyConfig) {
        runOnServer.run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(FLOW_ALIAS));
        runOnServer.run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(FLOW_ALIAS)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(Requirement.REQUIRED, UsernameFormFactory.PROVIDER_ID)
                        .addSubFlowExecution(Requirement.CONDITIONAL, subflow -> subflow
                                .addAuthenticatorExecution(Requirement.REQUIRED,
                                        ConditionalGroupAuthenticatorFactory.PROVIDER_ID,
                                        config -> config.setConfig(conditionConfig))
                                .addAuthenticatorExecution(Requirement.REQUIRED, DenyAccessAuthenticatorFactory.PROVIDER_ID,
                                        config -> config.setConfig(denyConfig)))
                        .addAuthenticatorExecution(Requirement.REQUIRED, PasswordFormFactory.PROVIDER_ID))
                .defineAsBrowserFlow());
    }

    private void configureBrowserFlowWithSkipExecutionInConditionalFlow(String groupPath, boolean negateOutput) {
        Map<String, String> conditionConfig = groupConditionConfig(groupPath, negateOutput);

        runOnServer.run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(FLOW_ALIAS));
        runOnServer.run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(FLOW_ALIAS)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(Requirement.REQUIRED, UsernameFormFactory.PROVIDER_ID)
                        .addSubFlowExecution(Requirement.REQUIRED, subflow -> subflow
                                .addSubFlowExecution(Requirement.ALTERNATIVE, alternativeFlow -> alternativeFlow
                                        .addSubFlowExecution(Requirement.CONDITIONAL, conditionalFlow -> conditionalFlow
                                                .addAuthenticatorExecution(Requirement.REQUIRED,
                                                        ConditionalGroupAuthenticatorFactory.PROVIDER_ID,
                                                        config -> config.setConfig(conditionConfig))
                                                .addAuthenticatorExecution(Requirement.REQUIRED,
                                                        AllowAccessAuthenticatorFactory.PROVIDER_ID)))
                                .addAuthenticatorExecution(Requirement.ALTERNATIVE, PasswordFormFactory.PROVIDER_ID)))
                .defineAsBrowserFlow());
    }

    private Map<String, String> groupConditionConfig(String groupPath, boolean negateOutput) {
        Map<String, String> config = new HashMap<>();
        config.put(ConditionalGroupAuthenticatorFactory.CONDITIONAL_USER_GROUP, groupPath);
        config.put(ConditionalGroupAuthenticatorFactory.CONF_NEGATE, Boolean.toString(negateOutput));
        return config;
    }

    private String createUser(String username) {
        UserRepresentation user = UserBuilder.create()
                .username(username)
                .password(PASSWORD)
                .email(username + "@localhost")
                .name("First", "Last")
                .emailVerified(true)
                .enabled(true)
                .build();
        try (Response response = realm.admin().users().create(user)) {
            return ApiUtil.getCreatedId(response);
        }
    }

    private String createGroup(String name) {
        try (Response response = realm.admin().groups().add(GroupBuilder.create().name(name).build())) {
            return ApiUtil.getCreatedId(response);
        }
    }

    private String createNestedGroup() {
        String parentId = createGroup(NESTED_PARENT);
        GroupRepresentation child = GroupBuilder.create().name(NESTED_CHILD).build();
        try (Response response = realm.admin().groups().group(parentId).subGroup(child)) {
            return ApiUtil.getCreatedId(response);
        }
    }

    private String createGroupId(String name) {
        List<GroupRepresentation> groups = realm.admin().groups().groups(name, 0, 1);
        if (groups.isEmpty()) {
            throw new IllegalStateException("Group not found: " + name);
        }
        return groups.get(0).getId();
    }

    private void revertBrowserFlow(String flowAlias) {
        List<AuthenticationFlowRepresentation> flows = realm.admin().flows().getFlows();
        RealmRepresentation realmRep = realm.admin().toRepresentation();
        realmRep.setBrowserFlow(DefaultAuthenticationFlows.BROWSER_FLOW);
        realm.admin().update(realmRep);

        AuthenticationFlowRepresentation flow = findFlowByAlias(flowAlias, flows);
        if (flow == null) {
            throw new IllegalArgumentException("The flow with alias " + flowAlias + " did not exist");
        }
        realm.admin().flows().deleteFlow(flow.getId());
    }
}
