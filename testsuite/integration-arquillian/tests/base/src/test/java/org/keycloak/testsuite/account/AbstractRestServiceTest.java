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
import static org.keycloak.testsuite.ProfileAssume.assumeFeatureEnabled;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.representations.account.SessionRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.util.TokenUtil;
import org.keycloak.testsuite.util.UserBuilder;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public abstract class AbstractRestServiceTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public TokenUtil tokenUtil = new TokenUtil();

    @Rule
    public AssertEvents events = new AssertEvents(this);

    protected CloseableHttpClient httpClient;

    @Before
    public void before() {
        httpClient = HttpClientBuilder.create().build();
        try {
            checkIfFeatureWorks(false);
            Response response = testingClient.testing().enableFeature(ACCOUNT_API.toString());
            assertEquals(200, response.getStatus());
            assumeFeatureEnabled(ACCOUNT_API);
            checkIfFeatureWorks(true);
        } catch (Exception e) {
            disableFeature();
            throw e;
        }
    }

    @After
    public void after() {
        try {
            disableFeature();
            httpClient.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void disableFeature() {
        Response response = testingClient.testing().disableFeature(ACCOUNT_API.toString());
        assertEquals(200, response.getStatus());
        checkIfFeatureWorks(false);
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.getUsers().add(UserBuilder.create().username("no-account-access").password("password").build());
        testRealm.getUsers().add(UserBuilder.create().username("view-account-access").role("account", "view-profile").password("password").build());
    }

    protected String getAccountUrl(String resource) {
        return suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/realms/test/account" + (resource != null ? "/" + resource : "");
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
