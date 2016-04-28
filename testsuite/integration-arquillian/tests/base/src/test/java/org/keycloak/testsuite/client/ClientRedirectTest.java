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

package org.keycloak.testsuite.client;

import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.TestRealmKeycloakTest;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.RealmBuilder;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class ClientRedirectTest extends TestRealmKeycloakTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        RealmBuilder.edit(testRealm)
                .client(ClientBuilder.create().clientId("launchpad-test").baseUrl("").rootUrl("http://example.org/launchpad"))
                .client(ClientBuilder.create().clientId("dummy-test").baseUrl("/base-path").rootUrl("http://example.org/dummy"));
    }

    /**
     * Integration test for {@link org.keycloak.services.resources.RealmsResource#getRedirect(String, String)}.
     *
     * @throws Exception
     */
    @Test
    public void testClientRedirectEndpoint() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        driver.get(getAuthServerRoot().toString() + "realms/test/clients/launchpad-test/redirect");
        assertEquals("http://example.org/launchpad", driver.getCurrentUrl());

        driver.get(getAuthServerRoot().toString() + "realms/test/clients/dummy-test/redirect");
        assertEquals("http://example.org/dummy/base-path", driver.getCurrentUrl());

        driver.get(getAuthServerRoot().toString() + "realms/test/clients/account/redirect");
        assertEquals(getAuthServerRoot().toString() + "realms/test/account", driver.getCurrentUrl());
    }
}
