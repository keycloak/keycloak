/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.RestartLoginCookie;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.pages.LoginPage;
import org.openqa.selenium.Cookie;

import java.io.IOException;


/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class RestartCookieTest extends AbstractTestRealmKeycloakTest {


    @Page
    protected LoginPage loginPage;


    @Rule
    public AssertEvents events = new AssertEvents(this);

    // KC_RESTART cookie from Keycloak 3.1.0
    private static final String OLD_RESTART_COOKIE_JSON = "{\n" +
            "  \"cs\": \"874a1ea8-5579-4f21-add0-903dd8e3ec1b\",\n" +
            "  \"cid\": \"test-app\",\n" +
            "  \"pty\": \"openid-connect\",\n" +
            "  \"ruri\": \"http://localhost:8081/auth/realms/master/app/auth\",\n" +
            "  \"act\": \"AUTHENTICATE\",\n" +
            "  \"notes\": {\n" +
            "    \"auth_type\": \"code\",\n" +
            "    \"scope\": \"openid\",\n" +
            "    \"iss\": \"http://localhost:8081/auth/realms/master/app/auth\",\n" +
            "    \"response_type\": \"code\",\n" +
            "    \"redirect_uri\": \"http://localhost:8081/auth/realms/master/app/auth/\",\n" +
            "    \"state\": \"6c983e5b-2dc1-411a-9ed1-0f51095949c5\",\n" +
            "    \"code_challenge_method\": \"plain\",\n" +
            "    \"nonce\": \"65639660-99b2-4cdf-bc9f-9978fdce5b03\",\n" +
            "    \"response_mode\": \"fragment\"\n" +
            "  }\n" +
            "}";

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }


    // KEYCLOAK-5440 -- migration from Keycloak 3.1.0
    @Test
    public void testRestartCookieBackwardsCompatible_Keycloak25() throws IOException {
        String oldRestartCookie = testingClient.server().fetchString((KeycloakSession session) -> {
            try {
                String cookieVal = OLD_RESTART_COOKIE_JSON.replace("\n", "").replace(" ", "");
                RealmModel realm = session.realms().getRealmByName("test");

                KeyManager.ActiveHmacKey activeKey = session.keys().getActiveHmacKey(realm);

                String encodedToken = new JWSBuilder()
                        .kid(activeKey.getKid())
                        .content(cookieVal.getBytes("UTF-8"))
                        .hmac256(activeKey.getSecretKey());

                return encodedToken;


            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        });

        oauth.openLoginForm();

        driver.manage().deleteAllCookies();
        driver.manage().addCookie(new Cookie(RestartLoginCookie.KC_RESTART, oldRestartCookie));

        loginPage.login("foo", "bar");
        loginPage.assertCurrent();
        Assert.assertEquals("Your login attempt timed out. Login will start from the beginning.", loginPage.getError());

        events.expectLogin().user((String) null).session((String) null).error(Errors.EXPIRED_CODE).clearDetails()
                .detail(Details.RESTART_AFTER_TIMEOUT, "true")
                .client((String) null)
                .assertEvent();
    }


    // KEYCLOAK-7158 -- migration from Keycloak 1.9.8
    @Test
    public void testRestartCookieBackwardsCompatible_Keycloak19() throws IOException {
        String oldRestartCookie = testingClient.server().fetchString((KeycloakSession session) -> {
            try {
                String cookieVal = OLD_RESTART_COOKIE_JSON.replace("\n", "").replace(" ", "");
                RealmModel realm = session.realms().getRealmByName("test");

                KeyManager.ActiveHmacKey activeKey = session.keys().getActiveHmacKey(realm);

                // There was no KID in the token in Keycloak 1.9.8
                String encodedToken = new JWSBuilder()
                        //.kid(activeKey.getKid())
                        .content(cookieVal.getBytes("UTF-8"))
                        .hmac256(activeKey.getSecretKey());

                return encodedToken;


            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        });

        oauth.openLoginForm();

        driver.manage().deleteAllCookies();
        driver.manage().addCookie(new Cookie(RestartLoginCookie.KC_RESTART, oldRestartCookie));

        loginPage.login("foo", "bar");
        loginPage.assertCurrent();
        Assert.assertEquals("Your login attempt timed out. Login will start from the beginning.", loginPage.getError());

        events.expectLogin().user((String) null).session((String) null).error(Errors.EXPIRED_CODE).clearDetails()
                .detail(Details.RESTART_AFTER_TIMEOUT, "true")
                .client((String) null)
                .assertEvent();
    }
}
