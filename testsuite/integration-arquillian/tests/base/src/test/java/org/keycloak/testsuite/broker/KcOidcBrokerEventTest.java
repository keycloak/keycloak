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

package org.keycloak.testsuite.broker;

import java.util.List;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.userprofile.UserProfileContext;

import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_ALIAS;

/**
 * Simple test to check the events after a broker login using OIDC. It also
 * tests that the event username is not wrong after a form login error
 * (Issue #10616).
 *
 * @author rmartinc
 */
public final class KcOidcBrokerEventTest extends AbstractBrokerTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcOidcBrokerConfiguration.INSTANCE;
    }

    private void checkFirstLoginEvents(String providerUserId, String consumerUserId) {
        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGIN)
                .userId(providerUserId)
                .clientId(bc.getIDPClientIdInProviderRealm())
                .hasSessionId()
                .details(Details.USERNAME, bc.getUserLogin());

        EventAssertion.assertSuccess(events.poll()).type(EventType.CODE_TO_TOKEN)
                .hasSessionId()
                .userId(providerUserId)
                .clientId(bc.getIDPClientIdInProviderRealm());

        EventAssertion.assertSuccess(events.poll()).type(EventType.USER_INFO_REQUEST)
                .hasSessionId()
                .userId(providerUserId)
                .clientId(bc.getIDPClientIdInProviderRealm());

        EventRepresentation eventRep = EventAssertion.assertSuccess(events.poll()).type(EventType.IDENTITY_PROVIDER_FIRST_LOGIN)
                .clientId("broker-app")
                .userId(null)
                .details(Details.IDENTITY_PROVIDER, IDP_OIDC_ALIAS)
                .details(Details.IDENTITY_PROVIDER_USERNAME, bc.getUserLogin()).getEvent();
        Assertions.assertTrue(eventRep.getDetails().get(Details.IDENTITY_PROVIDER_BROKER_SESSION_ID).startsWith(bc.getIDPAlias()));

        EventAssertion.assertSuccess(events.poll()).type(EventType.UPDATE_PROFILE)
                .clientId("broker-app")
                .userId(null)
                .details(Details.CONTEXT, UserProfileContext.IDP_REVIEW.name());

        EventRepresentation eventRep2 = EventAssertion.assertSuccess(events.poll()).type(EventType.REGISTER)
                .clientId("broker-app")
                .sessionId(null)
                .hasUserId()
                .details(Details.USERNAME, bc.getUserLogin())
                .details(Details.IDENTITY_PROVIDER_USERNAME, bc.getUserLogin())
                .details(Details.IDENTITY_PROVIDER, bc.getIDPAlias()).getEvent();
        if (consumerUserId != null) {
            Assertions.assertEquals(eventRep2.getUserId(), consumerUserId);
        }

        EventRepresentation eventRep3 = EventAssertion.assertSuccess(events.poll()).type(EventType.LOGIN)
                .clientId("broker-app")
                .hasSessionId()
                .hasUserId()
                .details(Details.USERNAME, bc.getUserLogin())
                .details(Details.IDENTITY_PROVIDER_USERNAME, bc.getUserLogin())
                .details(Details.IDENTITY_PROVIDER, bc.getIDPAlias()).getEvent();
        if (consumerUserId != null) {
            Assertions.assertEquals(eventRep3.getUserId(), consumerUserId);
        }
        
        events.clear();
    }

    private void checkLoginEvents(String providerUserId, String consumerUserId) {
        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGIN)
                .userId(providerUserId)
                .clientId(bc.getIDPClientIdInProviderRealm())
                .hasSessionId()
                .details(Details.USERNAME, bc.getUserLogin());

        EventAssertion.assertSuccess(events.poll()).type(EventType.CODE_TO_TOKEN)
                .hasSessionId()
                .userId(providerUserId)
                .clientId(bc.getIDPClientIdInProviderRealm());

        EventAssertion.assertSuccess(events.poll()).type(EventType.USER_INFO_REQUEST)
                .hasSessionId()
                .userId(providerUserId)
                .clientId(bc.getIDPClientIdInProviderRealm());

        EventRepresentation eventRep = EventAssertion.assertSuccess(events.poll()).type(EventType.LOGIN)
                .clientId("broker-app")
                .hasSessionId()
                .details(Details.USERNAME, bc.getUserLogin())
                .details(Details.IDENTITY_PROVIDER_USERNAME, bc.getUserLogin())
                .details(Details.IDENTITY_PROVIDER, bc.getIDPAlias()).getEvent();
        Assertions.assertTrue(eventRep.getUserId() == null || eventRep.getUserId().equals(consumerUserId));

        events.clear();
    }

    private void doALoginError() {
        events.clear();

        // navigate to the account url of the consumer realm
        oauth.client("broker-app");
        loginPage.open(bc.consumerRealmName());

        // Do a wrong login with a user that does not exist
        loginPage.login("wrong-user", "wrong-password");

        EventAssertion.assertError(events.poll()).type(EventType.LOGIN_ERROR)
                .userId(null)
                .clientId("broker-app")
                .sessionId(null)
                .details(Details.USERNAME, "wrong-user")
                .error("user_not_found");

        events.clear();
    }

    @Override
    protected void loginUser() {
        RealmResource providerRealm = adminClient.realm(bc.providerRealmName());
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());
        UserRepresentation providerUser = providerRealm.users().search(bc.getUserLogin()).iterator().next();
        events.clear();
        oauth.client("broker-app");
        loginPage.open(bc.consumerRealmName());

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
        Assertions.assertTrue(driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"),
                "We must be on correct realm right now");

        log.debug("Updating info on updateAccount page");
        updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail(), "Firstname", "Lastname");

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
        events.clear();

        super.testSingleLogout();

        events.clear();
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
        loginPage.open(bc.consumerRealmName());
        logInWithBroker(bc);

        Assertions.assertTrue(driver.getCurrentUrl().contains("/auth/realms/master/app"));
        Assertions.assertEquals(userCount, adminClient.realm(bc.consumerRealmName()).users().count());

        checkLoginEvents(providerUser.getId(), consumerUser.getId());
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

        Assertions.assertTrue(driver.getCurrentUrl().contains("/auth/realms/master/app"));
        Assertions.assertEquals(userCount, adminClient.realm(bc.consumerRealmName()).users().count());

        checkLoginEvents(providerUser.getId(), consumerUser.getId());
    }
}
