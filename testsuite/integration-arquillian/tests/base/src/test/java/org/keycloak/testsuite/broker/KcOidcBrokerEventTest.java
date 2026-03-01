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
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.userprofile.UserProfileContext;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

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

    private void checkFirstLoginEvents(RealmResource providerRealm, RealmResource consumerRealm, String providerUserId, String consumerUserId) {
        events.expect(EventType.LOGIN)
                .realm(providerRealm.toRepresentation().getId())
                .user(providerUserId)
                .client(bc.getIDPClientIdInProviderRealm())
                .session(Matchers.any(String.class))
                .detail(Details.USERNAME, bc.getUserLogin())
                .assertEvent();

        events.expect(EventType.CODE_TO_TOKEN)
                .session(Matchers.any(String.class))
                .realm(providerRealm.toRepresentation().getId())
                .user(providerUserId)
                .client(bc.getIDPClientIdInProviderRealm())
                .assertEvent();

        events.expect(EventType.USER_INFO_REQUEST)
                .session(Matchers.any(String.class))
                .realm(providerRealm.toRepresentation().getId())
                .user(providerUserId)
                .client(bc.getIDPClientIdInProviderRealm())
                .assertEvent();

        events.expect(EventType.IDENTITY_PROVIDER_FIRST_LOGIN)
                .realm(consumerRealm.toRepresentation().getId())
                .client("broker-app")
                .user((String)null)
                .detail(Details.IDENTITY_PROVIDER, IDP_OIDC_ALIAS)
                .detail(Details.IDENTITY_PROVIDER_USERNAME, bc.getUserLogin())
                .detail(Details.IDENTITY_PROVIDER_BROKER_SESSION_ID,  Matchers.startsWith(bc.getIDPAlias()))
                .assertEvent();

        events.expect(EventType.UPDATE_PROFILE)
                .realm(consumerRealm.toRepresentation().getId())
                .client("broker-app")
                .user((String)null)
                .detail(Details.CONTEXT, UserProfileContext.IDP_REVIEW.name())
                .assertEvent();

        events.expect(EventType.REGISTER)
                .realm(consumerRealm.toRepresentation().getId())
                .client("broker-app")
                .user(consumerUserId == null? Matchers.any(String.class) : Matchers.is(consumerUserId))
                .session((String) null)
                .detail(Details.USERNAME, bc.getUserLogin())
                .detail(Details.IDENTITY_PROVIDER_USERNAME, bc.getUserLogin())
                .detail(Details.IDENTITY_PROVIDER, bc.getIDPAlias())
                .assertEvent();

        events.expect(EventType.LOGIN)
                .realm(consumerRealm.toRepresentation().getId())
                .client("broker-app")
                .user(consumerUserId == null? Matchers.any(String.class) : Matchers.is(consumerUserId))
                .session(Matchers.any(String.class))
                .detail(Details.USERNAME, bc.getUserLogin())
                .detail(Details.IDENTITY_PROVIDER_USERNAME, bc.getUserLogin())
                .detail(Details.IDENTITY_PROVIDER, bc.getIDPAlias())
                .assertEvent();
        
        events.clear();
    }

    private void checkLoginEvents(RealmResource providerRealm, RealmResource consumerRealm, String providerUserId, String consumerUserId) {
        events.expect(EventType.LOGIN)
                .realm(providerRealm.toRepresentation().getId())
                .user(providerUserId)
                .client(bc.getIDPClientIdInProviderRealm())
                .session(Matchers.any(String.class))
                .detail(Details.USERNAME, bc.getUserLogin())
                .assertEvent();

        events.expect(EventType.CODE_TO_TOKEN)
                .session(Matchers.any(String.class))
                .realm(providerRealm.toRepresentation().getId())
                .user(providerUserId)
                .client(bc.getIDPClientIdInProviderRealm())
                .assertEvent();

        events.expect(EventType.USER_INFO_REQUEST)
                .session(Matchers.any(String.class))
                .realm(providerRealm.toRepresentation().getId())
                .user(providerUserId)
                .client(bc.getIDPClientIdInProviderRealm())
                .assertEvent();

        events.expect(EventType.LOGIN)
                .realm(consumerRealm.toRepresentation().getId())
                .client("broker-app")
                .user(consumerUserId == null? Matchers.any(String.class) : Matchers.is(consumerUserId))
                .session(Matchers.any(String.class))
                .detail(Details.USERNAME, bc.getUserLogin())
                .detail(Details.IDENTITY_PROVIDER_USERNAME, bc.getUserLogin())
                .detail(Details.IDENTITY_PROVIDER, bc.getIDPAlias())
                .assertEvent();

        events.clear();
    }

    private void doALoginError(RealmResource consumerRealm) {
        events.clear();

        // navigate to the account url of the consumer realm
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        // Do a wrong login with a user that does not exist
        loginPage.login("wrong-user", "wrong-password");

        events.expect(EventType.LOGIN_ERROR)
                .realm(consumerRealm.toRepresentation().getId())
                .user((String) null)
                .client("broker-app")
                .session((String) null)
                .detail(Details.USERNAME, "wrong-user")
                .error("user_not_found")
                .assertEvent();

        events.clear();
    }

    @Override
    protected void loginUser() {
        RealmResource providerRealm = adminClient.realm(bc.providerRealmName());
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());
        UserRepresentation providerUser = providerRealm.users().search(bc.getUserLogin()).iterator().next();
        events.clear();
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        super.loginUser();

        checkFirstLoginEvents(providerRealm, consumerRealm, providerUser.getId(), null);
    }

    private void loginUserAfterError() {
        RealmResource providerRealm = adminClient.realm(bc.providerRealmName());
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());
        UserRepresentation providerUser = providerRealm.users().search(bc.getUserLogin()).iterator().next();

        doALoginError(consumerRealm);

        logInWithBroker(bc);

        BrokerTestTools.waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
        Assert.assertTrue("We must be on correct realm right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));

        log.debug("Updating info on updateAccount page");
        updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail(), "Firstname", "Lastname");

        List<UserRepresentation> users = consumerRealm.users().search(bc.getUserLogin());
        Assert.assertEquals("There must be one user", 1, users.size());
        UserRepresentation consumerUser = users.iterator().next();
        Assert.assertEquals(bc.getUserEmail(), consumerUser.getEmail());

        checkFirstLoginEvents(providerRealm, consumerRealm, providerUser.getId(), consumerUser.getId());
    }

    @Override
    protected void testSingleLogout() {
        RealmResource providerRealm = adminClient.realm(bc.providerRealmName());
        UserRepresentation providerUser = providerRealm.users().search(bc.getUserLogin()).iterator().next();
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
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        logInWithBroker(bc);

        Assert.assertTrue(driver.getCurrentUrl().contains("/auth/realms/master/app"));
        Assert.assertEquals(userCount, adminClient.realm(bc.consumerRealmName()).users().count());

        checkLoginEvents(providerRealm, consumerRealm, providerUser.getId(), consumerUser.getId());
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

        doALoginError(consumerRealm);

        // now perform the login via the broker
        logInWithBroker(bc);

        Assert.assertTrue(driver.getCurrentUrl().contains("/auth/realms/master/app"));
        Assert.assertEquals(userCount, adminClient.realm(bc.consumerRealmName()).users().count());

        checkLoginEvents(providerRealm, consumerRealm, providerUser.getId(), consumerUser.getId());
    }
}
