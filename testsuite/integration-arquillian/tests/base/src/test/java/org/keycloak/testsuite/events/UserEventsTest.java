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
 */

package org.keycloak.testsuite.events;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Test;
import org.keycloak.common.Profile;
import org.keycloak.events.user.UserEventType;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.client.resources.TestingResource;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.rest.representation.TestUserEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.keycloak.testsuite.admin.ApiUtil.removeUserByUsername;
import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
@EnableFeature(value = Profile.Feature.USER_EVENT_SPI, skipRestart = true)
public class UserEventsTest extends AbstractTestRealmKeycloakTest {
    private TestingResource testingResource;

    @Page
    private LoginPage loginPage;

    @Page
    private RegisterPage registerPage;

    private UserRepresentation createUser;

    public UserEventsTest() {
        createUser = new UserRepresentation();
        createUser.setUsername("joneill");
        createUser.setFirstName("Jack");
        createUser.setLastName("O'Neill");
        createUser.setEmail("joneill@sgc.gov");
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.setRegistrationAllowed(true);
    }

    @After
    public void cleanupAfterTest() {
        getTestingResource().clearUserEventQueue();
        removeCreatedUser();
    }

    @Test
    public void adminCreates() {
        assertCreateEvent(UserEventType.CREATE_BY_ADMIN, () -> {
            testRealm().users().create(createUser);
        });
    }

    @Test
    public void userSelfRegisters() {
        loginPage.open();
        loginPage.assertCurrent();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        assertCreateEvent(UserEventType.CREATE_SELF_REGISTER, () -> {
            registerPage.register(createUser.getFirstName(), createUser.getLastName(), createUser.getEmail(), createUser.getUsername(), "pwd", "pwd");
        });
    }

    @Test
    @DisableFeature(value = Profile.Feature.USER_EVENT_SPI, skipRestart = true)
    public void featureDisabled() {
        assertEquals(0, getTestingResource().getUserPreEventCount());
        assertEquals(0, getTestingResource().getUserPostEventCount());
        testRealm().users().create(createUser);
        assertEquals(0, getTestingResource().getUserPreEventCount());
        assertEquals(0, getTestingResource().getUserPostEventCount());
    }

    private void assertCreateEvent(UserEventType expectedEventType, Runnable createAction) {
        assertEquals(0, getTestingResource().getUserPreEventCount());
        assertEquals(0, getTestingResource().getUserPostEventCount());

        createAction.run();

        assertEquals(1, getTestingResource().getUserPreEventCount());
        assertEquals(1, getTestingResource().getUserPostEventCount());

        TestUserEvent preEvent = getTestingResource().getUserPreEvent();
        TestUserEvent postEvent = getTestingResource().getUserPostEvent();

        assertEquals(preEvent, postEvent);

        assertEquals(expectedEventType, preEvent.getEventType());
        assertEquals(createUser.getUsername(), preEvent.getRepresentation().getUsername());
        assertNull(preEvent.getPreviousRepresentation());
    }

    private TestingResource getTestingResource() {
        if (testingResource == null) {
            testingResource = getTestingClient().testing();
        }
        return testingResource;
    }

    private void removeCreatedUser() {
        removeUserByUsername(testRealm(), createUser.getUsername());
    }
}
