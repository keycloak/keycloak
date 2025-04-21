/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.sssd;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginUpdateProfilePage;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

/**
 * <p>Abstract base class for SSSD tests.</p>
 *
 * @author rmartinc
 */
public abstract class AbstractBaseSSSDTest extends AbstractTestRealmKeycloakTest {

    @Page
    protected LoginPage loginPage;

    @Page
    protected AppPage appPage;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected LoginUpdateProfilePage updateProfilePage;

    // vars for the configuration of the SSSD installation
    protected static final String PROVIDER_NAME = "sssd";
    private static final String sssdConfigPath = "sssd/sssd.properties";
    private static PropertiesConfiguration sssdConfig;
    protected static final String DISABLED_USER = "disabled";
    protected static final String NO_EMAIL_USER = "noemail";
    protected static final String ADMIN_USER = "admin";

    @BeforeClass
    public static void loadSSSDConfiguration() throws ConfigurationException {
        InputStream is = SSSDTest.class.getClassLoader().getResourceAsStream(sssdConfigPath);
        sssdConfig = new PropertiesConfiguration();
        sssdConfig.load(is);
        sssdConfig.setListDelimiter(',');
    }

    protected void testLoginFailure(String username, String password) {
        loginPage.open();
        loginPage.login(username, password);
        loginPage.assertCurrent();
        Assert.assertEquals("Invalid username or password.", loginPage.getInputError());
        events.expect(EventType.LOGIN_ERROR).user(Matchers.any(String.class)).error(Errors.INVALID_USER_CREDENTIALS).assertEvent();
    }

    protected void testLoginSuccess(String username) {
        oauth.doLogin(username, getPassword(username));
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        EventRepresentation loginEvent = events.expectLogin().user(Matchers.any(String.class))
                .detail(Details.USERNAME, username).assertEvent();
        AccessTokenResponse tokenResponse = sendTokenRequestAndGetResponse(loginEvent);
        appPage.logout(tokenResponse.getIdToken());
        events.expectLogout(loginEvent.getSessionId()).user(loginEvent.getUserId()).assertEvent();
    }

    protected String getUsername() {
        return sssdConfig.getStringArray("usernames")[0];
    }

    protected String getFirstName(String username) {
        return sssdConfig.getString("user." + username + ".firstname");
    }

    protected String getLastName(String username) {
        return sssdConfig.getString("user." + username + ".lastname");
    }

    protected String getEmail(String username) {
        return sssdConfig.getString("user." + username + ".mail");
    }

    protected String getUser(String type) {
        return sssdConfig.getString("user." + type);
    }

    protected List<String> getUsernames() {
        return Arrays.asList(sssdConfig.getStringArray("usernames"));
    }

    protected String getPassword(String username) {
        return sssdConfig.getString("user." + username + ".password");
    }

    protected List<String> getGroups(String username) {
        return Arrays.asList(sssdConfig.getStringArray("user." + username + ".groups"));
    }
}
