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
package org.keycloak.testsuite.forms;

import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.authentication.authenticators.browser.DisplayInfoScreenAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.events.Details;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.pages.InfoPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.ExecutionBuilder;
import org.keycloak.testsuite.util.FlowBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.keycloak.authentication.authenticators.browser.DisplayInfoScreenAuthenticatorFactory.CONFIG_KEY_BODY_LOCALIZATION_KEY;
import static org.keycloak.authentication.authenticators.browser.DisplayInfoScreenAuthenticatorFactory.CONFIG_KEY_HEADER_LOCALIZATION_KEY;
import static org.keycloak.common.Profile.Feature.AUTHORIZATION;

/**
 * Tests for {@link org.keycloak.authentication.authenticators.browser.DisplayInfoScreenAuthenticator}
 */
public class DisplayInfoScreenAuthenticatorTest extends AbstractFlowTest {

    @Page
    protected LoginPage loginPage;

    @Page
    protected InfoPage infoPage;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    private static String userId;

    private static final String EXECUTION_ID = "displayInfoScreen";
    private static final String USERNAME = "user";
    private static final String PASSWORD = "password";

    private static final String LOCALIZATION_KEY_HEADER = "header";
    private static final String LOCALIZATION_KEY_BODY = "body";

    @BeforeClass
    public static void enabled() {
        ProfileAssume.assumeFeatureEnabled(AUTHORIZATION);
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        final var user = UserBuilder.create()
                .id(UUID.randomUUID().toString())
                .username(USERNAME)
                .email(USERNAME + "@test.com")
                .enabled(true)
                .password(PASSWORD)
                .build();

        RealmBuilder.edit(testRealm)
                .user(user)
                .addLocalizationText("en", LOCALIZATION_KEY_HEADER, "Welcome!")
                .addLocalizationText("en", LOCALIZATION_KEY_BODY, "We are happy that <b>you</b> are here.");
    }

    @Override
    public void importTestRealms() {
        super.importTestRealms();
        userId = adminClient.realm(TEST_REALM_NAME).users().search(USERNAME, true).get(0).getId();
    }

    @Before
    public void configureFlows() {
        final var infoScreenBrowserFlowAlias = "infoScreenBrowserFlow";

        if (testContext.isInitialized()) {
            return;
        }

        final var infoScreenBrowserFlow = FlowBuilder.create()
                .alias(infoScreenBrowserFlowAlias)
                .description("Browser flow with info screen")
                .providerId("basic-flow")
                .topLevel(true)
                .builtIn(false)
                .build();

        final var createFlowResponse = testRealm().flows().createFlow(infoScreenBrowserFlow);
        Assert.assertEquals(201, createFlowResponse.getStatus());

        final var realm = testRealm().toRepresentation();
        realm.setBrowserFlow(infoScreenBrowserFlowAlias);
        realm.setDirectGrantFlow(infoScreenBrowserFlowAlias);
        testRealm().update(realm);

        final var flow = findFlowByAlias(infoScreenBrowserFlowAlias);

        final var usernamePasswordFormExecution = ExecutionBuilder.create()
                .id("username password form")
                .parentFlow(flow.getId())
                .requirement(AuthenticationExecutionModel.Requirement.REQUIRED.name())
                .authenticator(UsernamePasswordFormFactory.PROVIDER_ID)
                .build();

        final var displayInfoScreenExecution = ExecutionBuilder.create()
                .id(EXECUTION_ID)
                .parentFlow(flow.getId())
                .requirement(AuthenticationExecutionModel.Requirement.REQUIRED.name())
                .authenticator(DisplayInfoScreenAuthenticatorFactory.PROVIDER_ID)
                .build();

        var addExecutionResponse = testRealm().flows().addExecution(usernamePasswordFormExecution);
        Assert.assertEquals(201, addExecutionResponse.getStatus());
        addExecutionResponse.close();

        addExecutionResponse = testRealm().flows().addExecution(displayInfoScreenExecution);
        Assert.assertEquals(201, addExecutionResponse.getStatus());
        addExecutionResponse.close();


        AuthenticatorConfigRepresentation config = new AuthenticatorConfigRepresentation();
        config.setAlias("display-info-config");
        Map<String, String> cfgMap = new HashMap<>();
        cfgMap.put(CONFIG_KEY_HEADER_LOCALIZATION_KEY, LOCALIZATION_KEY_HEADER);
        cfgMap.put(CONFIG_KEY_BODY_LOCALIZATION_KEY, LOCALIZATION_KEY_BODY);
        config.setConfig(cfgMap);

        try (Response resp = testRealm().flows().newExecutionConfig(EXECUTION_ID, config)) {
            Assert.assertEquals(201, resp.getStatus());
        }

        testContext.setInitialized(true);
    }

    @Test
    public void InfoScreenIsDisplayed() {
        loginPage.open();

        loginPage.login(USERNAME, PASSWORD);

        infoPage.assertCurrent();
        Assert.assertEquals("Welcome!", infoPage.getTitle());
        Assert.assertEquals("We are happy that you are here.", infoPage.getInfo());
        infoPage.clickToContinue();

        events.expectLogin().user(userId).detail(Details.USERNAME, USERNAME).assertEvent();
    }
}

