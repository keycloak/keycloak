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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.keycloak.common.Profile.Feature.ACCOUNT_API;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.representations.account.SessionRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.util.TokenUtil;
import org.keycloak.testsuite.util.UserBuilder;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@EnableFeature(value = ACCOUNT_API, skipRestart = true)
public abstract class AbstractRestServiceTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public TokenUtil tokenUtil = new TokenUtil();

    @Rule
    public AssertEvents events = new AssertEvents(this);

    protected CloseableHttpClient httpClient;

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
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.getUsers().add(UserBuilder.create().username("no-account-access").password("password").build());
        testRealm.getUsers().add(UserBuilder.create().username("view-account-access").role("account", "view-profile").password("password").build());
        testRealm.getUsers().add(UserBuilder.create().username("view-applications-access").role("account", "view-applications").password("password").build());
        testRealm.getUsers().add(UserBuilder.create().username("view-consent-access").role("account", "view-consent").password("password").build());
        testRealm.getUsers().add(UserBuilder.create().username("manage-consent-access").role("account", "manage-consent").password("password").build());
    }

    protected String getAccountUrl(String resource) {
        return suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/realms/test/account" + (resource != null ? "/" + resource : "");
    }

    @Test
    @DisableFeature(value = ACCOUNT_API, skipRestart = true)
    public void testFeatureDoesntWorkWhenDisabled() {
        checkIfFeatureWorks(false);
    }

    // Check if the feature really works
    private void checkIfFeatureWorks(boolean shouldWorks) {
        try {
            List<SessionRepresentation> sessions = SimpleHttp.doGet(getAccountUrl("sessions"), httpClient).auth(tokenUtil.getToken())
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
