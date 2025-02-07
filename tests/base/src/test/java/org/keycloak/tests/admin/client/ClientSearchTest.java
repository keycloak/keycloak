/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.admin.client;

import org.junit.jupiter.api.Test;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
@KeycloakIntegrationTest(config = ClientSearchTest.SearchableServer.class)
public class ClientSearchTest extends AbstractClientSearchTest {

    @Test
    public void testQuerySearch() {
        search(String.format("%s:%s", ATTR_ORG_NAME, ATTR_ORG_VAL), CLIENT_ID_1);
        search(String.format("%s:%s", ATTR_URL_NAME, ATTR_URL_VAL), CLIENT_ID_1, CLIENT_ID_2);
        search(String.format("%s:%s %s:%s", ATTR_ORG_NAME, ATTR_ORG_VAL, ATTR_URL_NAME, ATTR_URL_VAL), CLIENT_ID_1);
        search(String.format("%s:%s %s:%s", ATTR_ORG_NAME, "wrong val", ATTR_URL_NAME, ATTR_URL_VAL));
        search(String.format("%s:%s", ATTR_QUOTES_NAME_ESCAPED, ATTR_QUOTES_VAL_ESCAPED), CLIENT_ID_3);

        // "filtered" attribute won't take effect when JPA is used
        String[] expectedRes = new String[]{CLIENT_ID_1, CLIENT_ID_2};
        search(String.format("%s:%s %s:%s", ATTR_URL_NAME, ATTR_URL_VAL, ATTR_FILTERED_NAME, ATTR_FILTERED_VAL), expectedRes);
    }

    public static class SearchableServer implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.option("spi-client-jpa-searchable-attributes", String.join(",", ATTR_URL_NAME, ATTR_ORG_NAME, ATTR_QUOTES_NAME));
        }
    }
}
