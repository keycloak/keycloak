/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.broker;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.userprofile.UserProfileContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.tests.broker.BrokerTestConstants.IDP_OIDC_ALIAS;

/**
 * Simple test to check the events after a broker login using OIDC. It also
 * tests that the event username is not wrong after a form login error
 * (Issue #10616).
 *
 * @author rmartinc
 */
@KeycloakIntegrationTest(config = org.keycloak.tests.broker.BrokerServerConfig.class)
public final class KcOidcBrokerEventTest extends AbstractBrokerTest {

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient
    OAuthClient oauth;

    private final List<EventRepresentation> providerEventBuffer = new ArrayList<>();
    private final List<EventRepresentation> consumerEventBuffer = new ArrayList<>();

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcOidcBrokerConfiguration.INSTANCE;
    }

    private void checkFirstLoginEvents(String providerUserId, String consumerUserId) {
        assertProviderLoginEvents(providerUserId);

        EventRepresentation eventRep = EventAssertion.assertSuccess(pollConsumerEvent()).type(EventType.IDENTITY_PROVIDER_FIRST_LOGIN)
                .clientId("broker-app")
                .userId(null)
                .details(Details.IDENTITY_PROVIDER, IDP_OIDC_ALIAS)
                .details(Details.IDENTITY_PROVIDER_USERNAME, bc.getUserLogin()).getEvent();
        Assertions.assertTrue(eventRep.getDetails().get(Details.IDENTITY_PROVIDER_BROKER_SESSION_ID).startsWith(bc.getIDPAlias()));

        EventRepresentation maybeUpdateProfile = pollConsumerEvent();
        if (EventType.UPDATE_PROFILE.name().equals(maybeUpdateProfile.getType())) {
            EventAssertion.assertSuccess(maybeUpdateProfile).type(EventType.UPDATE_PROFILE)
                    .clientId("broker-app")
                    .userId(null)
                    .details(Details.CONTEXT, UserProfileContext.IDP_REVIEW.name());
            maybeUpdateProfile = pollConsumerEvent();
        }

        EventRepresentation eventRep2 = EventAssertion.assertSuccess(maybeUpdateProfile).type(EventType.REGISTER)
                .clientId("broker-app")
                .sessionId(null)
                .hasUserId()
                .details(Details.USERNAME, bc.getUserLogin())
                .details(Details.IDENTITY_PROVIDER_USERNAME, bc.getUserLogin())
                .details(Details.IDENTITY_PROVIDER, bc.getIDPAlias()).getEvent();
        if (consumerUserId != null) {
            Assertions.assertEquals(eventRep2.getUserId(), consumerUserId);
        }

        EventRepresentation eventRep3 = EventAssertion.assertSuccess(pollConsumerEvent()).type(EventType.LOGIN)
                .clientId("broker-app")
                .hasSessionId()
                .hasUserId()
                .details(Details.USERNAME, bc.getUserLogin())
                .details(Details.IDENTITY_PROVIDER_USERNAME, bc.getUserLogin())
                .details(Details.IDENTITY_PROVIDER, bc.getIDPAlias()).getEvent();
        if (consumerUserId != null) {
            Assertions.assertEquals(eventRep3.getUserId(), consumerUserId);
        }

        clearAllEvents();
    }

    private void checkLoginEvents(String providerUserId, String consumerUserId) {
        assertProviderLoginEvents(providerUserId);

        EventRepresentation eventRep = EventAssertion.assertSuccess(pollConsumerEvent()).type(EventType.LOGIN)
                .clientId("broker-app")
                .hasSessionId()
                .details(Details.USERNAME, bc.getUserLogin())
                .details(Details.IDENTITY_PROVIDER_USERNAME, bc.getUserLogin())
                .details(Details.IDENTITY_PROVIDER, bc.getIDPAlias()).getEvent();
        Assertions.assertTrue(eventRep.getUserId() == null || eventRep.getUserId().equals(consumerUserId));

        clearAllEvents();
    }

    private void doALoginError() {
        clearAllEvents();

        // navigate to the account url of the consumer realm
        oauth.client("broker-app");
        oauth.realm(bc.consumerRealmName());
        oauth.openLoginForm();

        // Do a wrong login with a user that does not exist
        loginPage.login("wrong-user", "wrong-password");

        EventAssertion.assertError(pollConsumerEvent()).type(EventType.LOGIN_ERROR)
                .userId(null)
                .clientId("broker-app")
                .sessionId(null)
                .details(Details.USERNAME, "wrong-user")
                .error("user_not_found");

        clearAllEvents();
    }

    @Override
    protected void loginUser() {
        RealmResource providerRealm = adminClient.realm(bc.providerRealmName());
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());
        UserRepresentation providerUser = providerRealm.users().search(bc.getUserLogin()).iterator().next();
        clearAllEvents();
        oauth.client("broker-app");
        oauth.realm(bc.consumerRealmName());
        oauth.openLoginForm();

        super.loginUser();

        checkFirstLoginEvents(providerUser.getId(), null);
    }

    private void loginUserAfterError() {
        RealmResource providerRealm = adminClient.realm(bc.providerRealmName());
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());
        UserRepresentation providerUser = providerRealm.users().search(bc.getUserLogin()).iterator().next();

        doALoginError();

        logInWithBroker(bc);

        BrokerTestTools.waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
        String currentUrl = driver.getCurrentUrl();
        String consumerRealmPath = "/realms/" + bc.consumerRealmName() + "/";
        String providerRequiredActionPath = "/realms/" + bc.providerRealmName() + "/login-actions/required-action";
        Assertions.assertTrue(currentUrl.contains(consumerRealmPath) || currentUrl.contains(providerRequiredActionPath),
                "Unexpected realm for profile update page. currentUrl=" + currentUrl);

        log.debug("Updating info on updateAccount page");
        if (updateAccountInformationPage.isUsernamePresent()) {
            updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail(), "Firstname", "Lastname");
        } else {
            updateAccountInformationPage.updateAccountInformation(bc.getUserEmail(), "Firstname", "Lastname");
        }

        List<UserRepresentation> users = consumerRealm.users().search(bc.getUserLogin());
        Assertions.assertEquals(1, users.size(), "There must be one user");
        UserRepresentation consumerUser = users.iterator().next();
        Assertions.assertEquals(bc.getUserEmail(), consumerUser.getEmail());

        checkFirstLoginEvents(providerUser.getId(), consumerUser.getId());
    }

    @Override
    protected void testSingleLogout() {
        RealmResource providerRealm = adminClient.realm(bc.providerRealmName());
        providerRealm.users().search(bc.getUserLogin()).iterator().next();
        clearAllEvents();

        super.testSingleLogout();

        clearAllEvents();
    }

    @Test
    @Override
    public void loginWithExistingUser() {
        // first login to execute the first login flow and create/link the user
        testLogInAsUserInIDP();

        RealmResource providerRealm = adminClient.realm(bc.providerRealmName());
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());
        UserRepresentation providerUser = providerRealm.users().search(bc.getUserLogin()).iterator().next();
        UserRepresentation consumerUser = consumerRealm.users().search(bc.getUserLogin()).iterator().next();
        Integer userCount = adminClient.realm(bc.consumerRealmName()).users().count();

        // now do the second login
        oauth.client("broker-app");
        oauth.realm(bc.consumerRealmName());
        oauth.openLoginForm();
        logInWithBroker(bc);

        Assertions.assertTrue(driver.getCurrentUrl().contains("/realms/" + bc.consumerRealmName() + "/app"));
        Assertions.assertEquals(userCount, adminClient.realm(bc.consumerRealmName()).users().count());

        checkLoginEvents(providerUser.getId(), consumerUser.getId());
    }

    private void assertProviderLoginEvents(String providerUserId) {
        EventRepresentation firstEvent = pollProviderEvent();
        while (EventType.VERIFY_PROFILE.name().equals(firstEvent.getType())
                || EventType.UPDATE_PROFILE.name().equals(firstEvent.getType())) {
            EventAssertion.assertSuccess(firstEvent)
                    .type(EventType.valueOf(firstEvent.getType()));
            firstEvent = pollProviderEvent();
        }

        EventAssertion.assertSuccess(firstEvent).type(EventType.LOGIN)
                .userId(providerUserId)
                .clientId(bc.getIDPClientIdInProviderRealm())
                .hasSessionId()
                .details(Details.USERNAME, bc.getUserLogin());

        EventRepresentation codeToToken = pollProviderEventOptional();
        if (codeToToken == null) {
            return;
        }

        EventAssertion.assertSuccess(codeToToken).type(EventType.CODE_TO_TOKEN)
                .hasSessionId()
                .userId(providerUserId)
                .clientId(bc.getIDPClientIdInProviderRealm());

        EventRepresentation userInfoRequest = pollProviderEventOptional();
        if (userInfoRequest == null) {
            return;
        }

        EventAssertion.assertSuccess(userInfoRequest).type(EventType.USER_INFO_REQUEST)
                .hasSessionId()
                .userId(providerUserId)
                .clientId(bc.getIDPClientIdInProviderRealm());
    }

    @Test
    public void testLogInAsUserInIDPAfterError() {
        loginUserAfterError();
        testSingleLogout();
    }

    @Test
    public void loginWithExistingUserAfterError() {
        // first login to execute the first login flow and create/link the user
        testLogInAsUserInIDP();

        RealmResource providerRealm = adminClient.realm(bc.providerRealmName());
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());
        UserRepresentation providerUser = providerRealm.users().search(bc.getUserLogin()).iterator().next();
        UserRepresentation consumerUser = consumerRealm.users().search(bc.getUserLogin()).iterator().next();
        Integer userCount = adminClient.realm(bc.consumerRealmName()).users().count();

        doALoginError();

        // now perform the login via the broker
        logInWithBroker(bc);

        Assertions.assertTrue(driver.getCurrentUrl().contains("/realms/" + bc.consumerRealmName() + "/app"));
        Assertions.assertEquals(userCount, adminClient.realm(bc.consumerRealmName()).users().count());

        checkLoginEvents(providerUser.getId(), consumerUser.getId());
    }

    private EventRepresentation pollProviderEvent() {
        return pollEvent(adminClient.realm(bc.providerRealmName()), providerEventBuffer, bc.providerRealmName());
    }

    private EventRepresentation pollProviderEventOptional() {
        return pollEventOptional(adminClient.realm(bc.providerRealmName()), providerEventBuffer);
    }

    private EventRepresentation pollConsumerEvent() {
        return pollEvent(adminClient.realm(bc.consumerRealmName()), consumerEventBuffer, bc.consumerRealmName());
    }

    private EventRepresentation pollEvent(RealmResource realm, List<EventRepresentation> buffer, String realmName) {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (!buffer.isEmpty()) {
                return buffer.remove(0);
            }

            List<EventRepresentation> events = realm.getEvents(null, null, null, null, null, null, null, null, "asc");
            if (!events.isEmpty()) {
                buffer.addAll(events);
                realm.clearEvents();
                continue;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        Assertions.fail("No events available for realm " + realmName);
        return null;
    }

    private EventRepresentation pollEventOptional(RealmResource realm, List<EventRepresentation> buffer) {
        long deadline = System.currentTimeMillis() + 1500;
        while (System.currentTimeMillis() < deadline) {
            if (!buffer.isEmpty()) {
                return buffer.remove(0);
            }

            List<EventRepresentation> events = realm.getEvents(null, null, null, null, null, null, null, null, "asc");
            if (!events.isEmpty()) {
                buffer.addAll(events);
                realm.clearEvents();
                continue;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return null;
    }

    private void clearAllEvents() {
        providerEventBuffer.clear();
        consumerEventBuffer.clear();
        adminClient.realm(bc.providerRealmName()).clearEvents();
        adminClient.realm(bc.consumerRealmName()).clearEvents();
    }
}
