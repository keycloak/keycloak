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

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.util.ExecutionBuilder;
import org.keycloak.testsuite.util.FlowBuilder;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class CustomRegistrationFlowTest extends AbstractFlowTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Before
    public void configureFlow() {
        AuthenticationFlowRepresentation flow = FlowBuilder.create()
                                                           .alias("dummy registration")
                                                           .description("dummy pass through registration")
                                                           .providerId("basic-flow")
                                                           .topLevel(true)
                                                           .builtIn(false)
                                                           .build();
        testRealm().flows().createFlow(flow);

        setRegistrationFlow(flow);

        // refresh flow to find its id
        flow = findFlowByAlias(flow.getAlias());

        AuthenticationExecutionRepresentation execution = ExecutionBuilder.create()
                                                            .parentFlow(flow.getId())
                                                            .requirement(AuthenticationExecutionModel.Requirement.REQUIRED.toString())
                                                            .authenticator(PassThroughRegistration.PROVIDER_ID)
                                                            .priority(10)
                                                            .authenticatorFlow(false)
                                                            .build();

        testRealm().flows().addExecution(execution);
    }

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected ErrorPage errorPage;

    @Page
    protected LoginPasswordUpdatePage updatePasswordPage;

    @Page
    protected RegisterPage registerPage;

    @Test
    public void registerUserSuccess() {
        loginPage.open();
        loginPage.clickRegister();

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        String userId = events.expectRegister(PassThroughRegistration.username, PassThroughRegistration.email).assertEvent().getUserId();
        events.expectLogin().detail("username", PassThroughRegistration.username).user(userId).assertEvent();
    }




}
