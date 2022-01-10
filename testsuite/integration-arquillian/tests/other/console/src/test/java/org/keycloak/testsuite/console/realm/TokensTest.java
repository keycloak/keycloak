/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.console.realm;

import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.authentication.actiontoken.resetcred.ResetCredentialsActionToken;
import org.keycloak.authentication.actiontoken.verifyemail.VerifyEmailActionToken;
import org.keycloak.models.ParConfig;
import org.keycloak.models.jpa.entities.RealmAttributes;
import org.keycloak.testsuite.auth.page.account.Account;
import org.keycloak.testsuite.console.page.realm.TokenSettings;
import org.keycloak.testsuite.console.page.users.UserAttributes;
import org.keycloak.testsuite.pages.VerifyEmailPage;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.util.UIUtils.refreshPageAndWaitForLoad;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWithLoginUrlOf;

/**
 *
 * @author Petr Mensik
 */
public class TokensTest extends AbstractRealmTest {

    @Page
    private TokenSettings tokenSettingsPage;

    @Page
    private UserAttributes userAttributesPage;

    @Page
    protected VerifyEmailPage verifyEmailPage;

    @Page
    private Account testRealmAccountPage;

    private static final int TIMEOUT = 1;
    private static final TimeUnit TIME_UNIT = TimeUnit.MINUTES;

    @Before
    public void beforeTokensTest() {
        tokenSettingsPage.navigateTo();
    }

    @Test
    public void testTimeoutForRealmSession() throws InterruptedException {
        tokenSettingsPage.form().setSessionTimeout(TIMEOUT, TIME_UNIT);
        tokenSettingsPage.form().save();

        loginToTestRealmConsoleAs(testUser);
        waitForTimeout(TIMEOUT + 2);

        refreshPageAndWaitForLoad();

        log.debug(driver.getCurrentUrl());
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage);
    }

    @Test
    public void testLifespanOfRealmSession() throws InterruptedException {
        tokenSettingsPage.form().setSessionTimeoutLifespan(TIMEOUT, TIME_UNIT);
        tokenSettingsPage.form().save();

        loginToTestRealmConsoleAs(testUser);
        waitForTimeout(TIMEOUT / 2);

        refreshPageAndWaitForLoad();
        assertCurrentUrlStartsWith(testRealmAdminConsolePage); // assert still logged in (within lifespan)

        waitForTimeout(TIMEOUT / 2 + 2);
        refreshPageAndWaitForLoad();

        log.debug(driver.getCurrentUrl());
        assertCurrentUrlStartsWithLoginUrlOf(testRealmPage); // assert logged out (lifespan exceeded)
    }

    @Test
    public void testLifespanOfVerifyEmailActionTokenPropagated() throws InterruptedException {
        tokenSettingsPage.form().setOperation(VerifyEmailActionToken.TOKEN_TYPE, TIMEOUT, TimeUnit.DAYS);
        tokenSettingsPage.form().save();
        assertAlertSuccess();

        loginToTestRealmConsoleAs(testUser);

        tokenSettingsPage.navigateTo();
        tokenSettingsPage.form().selectOperation(VerifyEmailActionToken.TOKEN_TYPE);

        assertTrue("User action token for verify e-mail expected",
                tokenSettingsPage.form().isOperationEquals(VerifyEmailActionToken.TOKEN_TYPE, TIMEOUT, TimeUnit.DAYS));

    }

    @Test
    public void testLifespanActionTokenPropagatedForVerifyEmailAndResetPassword() throws InterruptedException {
        tokenSettingsPage.form().setOperation(VerifyEmailActionToken.TOKEN_TYPE, TIMEOUT, TimeUnit.DAYS);
        tokenSettingsPage.form().setOperation(ResetCredentialsActionToken.TOKEN_TYPE, TIMEOUT, TimeUnit.HOURS);
        tokenSettingsPage.form().save();
        assertAlertSuccess();

        loginToTestRealmConsoleAs(testUser);

        tokenSettingsPage.navigateTo();
        assertTrue("User action token for verify e-mail expected",
                tokenSettingsPage.form().isOperationEquals(VerifyEmailActionToken.TOKEN_TYPE, TIMEOUT, TimeUnit.DAYS));

        assertTrue("User action token for reset credentials expected",
                tokenSettingsPage.form().isOperationEquals(ResetCredentialsActionToken.TOKEN_TYPE, TIMEOUT, TimeUnit.HOURS));

        //Verify if values were properly propagated
        Map<String, Integer> userActionTokens = getUserActionTokens();

        assertThat("Action Token attributes list should contain 2 items", userActionTokens.entrySet(), Matchers.hasSize(2));
        assertThat(userActionTokens, Matchers.hasEntry(VerifyEmailActionToken.TOKEN_TYPE, (int)(TimeUnit.DAYS.toSeconds(TIMEOUT))));
        assertThat(userActionTokens, Matchers.hasEntry(ResetCredentialsActionToken.TOKEN_TYPE, (int)(TimeUnit.HOURS.toSeconds(TIMEOUT))));

    }

    @Test
    public void testButtonDisabledForEmptyAttributes() throws InterruptedException {
        tokenSettingsPage.form().setOperation(VerifyEmailActionToken.TOKEN_TYPE, TIMEOUT, TimeUnit.DAYS);
        tokenSettingsPage.form().save();
        assertAlertSuccess();

        loginToTestRealmConsoleAs(testUser);

        tokenSettingsPage.navigateTo();
        tokenSettingsPage.form().selectOperation(VerifyEmailActionToken.TOKEN_TYPE);
        tokenSettingsPage.form().selectOperation(ResetCredentialsActionToken.TOKEN_TYPE);

        assertFalse("Save button should be disabled", tokenSettingsPage.form().saveBtn().isEnabled());
        assertFalse("Cancel button should be disabled", tokenSettingsPage.form().cancelBtn().isEnabled());
    }

    @Test
    public void testLifespanActionTokenResetForVerifyEmail() throws InterruptedException {
        tokenSettingsPage.form().setOperation(VerifyEmailActionToken.TOKEN_TYPE, TIMEOUT, TimeUnit.DAYS);
        tokenSettingsPage.form().setOperation(ResetCredentialsActionToken.TOKEN_TYPE, TIMEOUT, TimeUnit.HOURS);
        tokenSettingsPage.form().save();
        assertAlertSuccess();

        loginToTestRealmConsoleAs(testUser);

        tokenSettingsPage.navigateTo();
        assertTrue("User action token for verify e-mail expected",
                tokenSettingsPage.form().isOperationEquals(VerifyEmailActionToken.TOKEN_TYPE, TIMEOUT, TimeUnit.DAYS));

        assertTrue("User action token for reset credentials expected",
                tokenSettingsPage.form().isOperationEquals(ResetCredentialsActionToken.TOKEN_TYPE, TIMEOUT, TimeUnit.HOURS));

        //Remove VerifyEmailActionToken and reset attribute
        tokenSettingsPage.form().resetActionToken(VerifyEmailActionToken.TOKEN_TYPE);
        tokenSettingsPage.form().save();

        //Verify if values were properly propagated
        Map<String, Integer> userActionTokens = getUserActionTokens();

        assertTrue("Action Token attributes list should contain 1 item", userActionTokens.size() == 1);
        assertNull("VerifyEmailActionToken should not exist", userActionTokens.get(VerifyEmailActionToken.TOKEN_TYPE));
        assertEquals("ResetCredentialsActionToken expected to be propagated",
                userActionTokens.get(ResetCredentialsActionToken.TOKEN_TYPE).longValue(), TimeUnit.HOURS.toSeconds(TIMEOUT));

    }

    @Test
    public void testParRequestUriLifespan() {
        int defaultMinutes = (int) TimeUnit.SECONDS.toMinutes(ParConfig.DEFAULT_PAR_REQUEST_URI_LIFESPAN);
        assertThat(tokenSettingsPage.form().getRequestUriLifespanTimeout(), is(defaultMinutes));

        tokenSettingsPage.form().setRequestUriLifespanTimeout(30, TimeUnit.MINUTES);
        tokenSettingsPage.form().save();

        assertAlertSuccess();

        assertThat(tokenSettingsPage.form().getRequestUriLifespanTimeout(), is(30));
        assertThat(tokenSettingsPage.form().getRequestUriLifespanUnits(), is(TimeUnit.MINUTES));

        tokenSettingsPage.form().setRequestUriLifespanTimeout(20,TimeUnit.HOURS);
        tokenSettingsPage.form().save();

        assertAlertSuccess();

        assertThat(tokenSettingsPage.form().getRequestUriLifespanTimeout(), is(20));
        assertThat(tokenSettingsPage.form().getRequestUriLifespanUnits(), is(TimeUnit.HOURS));
    }

    private Map<String, Integer> getUserActionTokens() {
        Map<String, Integer> userActionTokens = new HashMap<>();
        adminClient.realm(testRealmPage.getAuthRealm()).toRepresentation().getAttributes().entrySet().stream()
                .filter(Objects::nonNull)
                .filter(entry -> entry.getKey().startsWith(RealmAttributes.ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN + "."))
                .forEach(entry -> userActionTokens.put(entry.getKey().substring(RealmAttributes.ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN.length() + 1), Integer.valueOf(entry.getValue())));
        return userActionTokens;
    }

    private void waitForTimeout (int timeout) throws InterruptedException {
        log.info("Wait for timeout: " + timeout + " " + TIME_UNIT);
        TIME_UNIT.sleep(timeout);
        log.info("Timeout reached");
    }
}
