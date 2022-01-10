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
package org.keycloak.testsuite.admin;

import org.keycloak.Config;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.updaters.Creator;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.RealmBuilder;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AdminConsolePermissionsCalculatedTest extends AbstractKeycloakTest {

    private static final String REALM_NAME = "realm-name";

    private CloseableHttpClient client;

    @Before
    public void before() {
        client = HttpClientBuilder.create().build();
    }

    @After
    public void after() {
        try {
            client.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }

    @Test
    public void changeRealmTokenAlgorithm() throws Exception {
        try (Keycloak adminClient = AdminClientUtil.createAdminClient(suiteContext.isAdapterCompatTesting(), suiteContext.getAuthServerInfo().getContextRoot().toString());
          Creator c = Creator.create(adminClient, RealmBuilder.create().name(REALM_NAME).build())) {
            AccessTokenResponse accessToken = adminClient.tokenManager().getAccessToken();
            assertNotNull(adminClient.realms().findAll());

            String whoAmiUrl = suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/admin/master/console/whoami";

            JsonNode jsonNode = SimpleHttp.doGet(whoAmiUrl, client).auth(accessToken.getToken()).asJson();

            assertTrue("Permissions for " + Config.getAdminRealm() + " realm.", jsonNode.at("/realm_access/" + Config.getAdminRealm()).isArray());
            assertTrue("Permissions for " + REALM_NAME + " realm.", jsonNode.at("/realm_access/" + REALM_NAME).isArray());
        }
    }

}
