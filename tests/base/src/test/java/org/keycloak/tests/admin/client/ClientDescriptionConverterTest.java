/*
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.tests.admin.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

/**
 *
 * @author hmlnarik
 */
@KeycloakIntegrationTest
public class ClientDescriptionConverterTest {

    @InjectRealm
    ManagedRealm realm;

    // https://issues.jboss.org/browse/KEYCLOAK-4040
    @Test
    public void testOrganizationDetailsMetadata() throws IOException {
        try (InputStream is = ClientDescriptionConverterTest.class.getResourceAsStream("KEYCLOAK-4040-sharefile-metadata.xml")) {
            String data = IOUtils.toString(is, StandardCharsets.UTF_8);
            realm.admin().convertClientDescription(data);
        }
    }
}
