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
package org.keycloak.testsuite.account;

import java.io.IOException;
import java.util.List;

import org.keycloak.common.enums.AccountRestApiVersion;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.account.SessionRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.broker.util.SimpleHttpDefault;
import org.keycloak.testsuite.util.TokenUtil;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.common.Profile.Feature.ACCOUNT_API;
import static org.keycloak.testsuite.util.oauth.OAuthClient.APP_ROOT;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public abstract class AbstractRestServiceTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public TokenUtil tokenUtil = new TokenUtil("test-user@localhost", "password");

    @Rule
    public AssertEvents events = new AssertEvents(this);

    protected CloseableHttpClient httpClient;

    protected String inUseClientAppUri = APP_ROOT + "/in-use-client";

    protected String offlineClientAppUri = APP_ROOT + "/offline-client";

    protected String alwaysDisplayClientAppUri = APP_ROOT + "/always-display-client";

    protected String apiVersion;

    @Before
    public void before() {
        httpClient = HttpClientBuilder.create().build();
    }

    @After
    public void after() {
        try {
            httpClient.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        apiVersion = null;
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.getUsers().add(UserBuilder.create().username("no-account-access").password("password").build());
        testRealm.getUsers().add(UserBuilder.create().username("view-account-access").clientRoles("account", "view-profile").password("password").build());
        testRealm.getUsers().add(UserBuilder.create().username("view-applications-access").realmRoles("user", "offline_access").clientRoles("account", "view-applications").clientRoles("account", "manage-consent").password("password").build());
        testRealm.getUsers().add(UserBuilder.create().username("view-consent-access").clientRoles("account", "view-consent").password("password").build());
        testRealm.getUsers().add(UserBuilder.create().username("manage-consent-access").clientRoles("account", "manage-consent").clientRoles("account", "view-profile").password("password").build());
        testRealm.getUsers().add(UserBuilder.create().username("manage-account-access").clientRoles("account", "view-profile").clientRoles("account", "manage-account").realmRoles("user", "offline_access").password("password").build());

        org.keycloak.representations.idm.ClientRepresentation inUseApp = ClientBuilder.create().clientId("in-use-client")
                .id(KeycloakModelUtils.generateId())
                .name("In Use Client")
                .baseUrl(inUseClientAppUri)
                .directAccessGrantsEnabled()
                .secret("secret1").build();
        testRealm.getClients().add(inUseApp);

        org.keycloak.representations.idm.ClientRepresentation offlineApp = ClientBuilder.create().clientId("offline-client")
                .id(KeycloakModelUtils.generateId())
                .name("Offline Client")
                .baseUrl(offlineClientAppUri)
                .directAccessGrantsEnabled()
                .secret("secret1").build();
        testRealm.getClients().add(offlineApp);

        org.keycloak.representations.idm.ClientRepresentation offlineApp2 = ClientBuilder.create().clientId("offline-client-without-base-url")
                .id(KeycloakModelUtils.generateId())
                .name("Offline Client Without Base URL")
                .directAccessGrantsEnabled()
                .secret("secret1").build();
        testRealm.getClients().add(offlineApp2);

        org.keycloak.representations.idm.ClientRepresentation alwaysDisplayApp = ClientBuilder.create().clientId("always-display-client")
                .id(KeycloakModelUtils.generateId())
                .name("Always Display Client")
                .baseUrl(alwaysDisplayClientAppUri)
                .directAccessGrantsEnabled()
                .alwaysDisplayInConsole(true)
                .secret("secret1").build();
        testRealm.getClients().add(alwaysDisplayApp);
    }

    protected String getAccountUrl(String resource) {
        String url = getAccountRootUrl();
        if (apiVersion != null) {
            url += "/" + apiVersion;
        }
        if (resource != null) {
            url += "/" + resource;
        }
        return url;
    }

    @Test
    @DisableFeature(value = ACCOUNT_API, skipRestart = true)
    public void testFeatureDoesntWorkWhenDisabled() {
        checkIfFeatureWorks(false);
    }

    @Test
    @DisableFeature(value = ACCOUNT_API, skipRestart = true)
    public void testVersionedApiDoesntWorkWhenDisabled() {
        apiVersion = AccountRestApiVersion.DEFAULT.getStrVersion();
        assertThat(getAccountUrl(""), containsString(apiVersion));
        checkIfFeatureWorks(false);
    }

    // Check if the feature really works
    private void checkIfFeatureWorks(boolean shouldWorks) {
        try {
            List<SessionRepresentation> sessions = SimpleHttpDefault.doGet(getAccountUrl("sessions"), httpClient).auth(tokenUtil.getToken())
                    .asJson(new TypeReference<List<SessionRepresentation>>() {
                    });
            assertEquals(1, sessions.size());
            if (!shouldWorks)
                fail("Feature is available, but this moment should be disabled");

        } catch (Exception e) {
            if (shouldWorks) {
                e.printStackTrace();
                fail("Feature is not available");
            }
        }
    }
}
