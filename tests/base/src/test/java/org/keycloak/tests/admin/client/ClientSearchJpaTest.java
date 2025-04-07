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

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
@KeycloakIntegrationTest
public class ClientSearchJpaTest extends AbstractClientSearchTest {

    @Test
    public void testJpaSearchableAttributesUnset() {
        // JPA store removes all attributes by default, i.e. returns all clients
        String[] expectedRes = {CLIENT_ID_1, CLIENT_ID_2, CLIENT_ID_3, "account", "account-console", "admin-cli", "broker", "realm-management", "security-admin-console"};

        search(String.format("%s:%s", ATTR_ORG_NAME, ATTR_ORG_VAL), expectedRes);
    }

}
