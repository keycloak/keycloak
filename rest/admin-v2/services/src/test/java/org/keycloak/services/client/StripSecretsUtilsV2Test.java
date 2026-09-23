/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.services.client;

import org.keycloak.models.utils.StripSecretsUtilsV2;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class StripSecretsUtilsV2Test {

    private static final String SECRET_VALUE = "**********";

    @Test
    void masksOidcSecret() {
        ObjectNode auth = JsonSerialization.createObjectNode();
        auth.put("secret", "super-secret");
        ObjectNode node = JsonSerialization.createObjectNode();
        node.set("auth", auth);

        StripSecretsUtilsV2.maskSecrets(node);

        assertEquals(SECRET_VALUE, node.path("auth").path("secret").asText());
    }

    @Test
    void leavesVaultOidcSecretUnmasked() {
        ObjectNode auth = JsonSerialization.createObjectNode();
        auth.put("secret", "${vault.clientSecret}");
        ObjectNode node = JsonSerialization.createObjectNode();
        node.set("auth", auth);

        StripSecretsUtilsV2.maskSecrets(node);

        assertEquals("${vault.clientSecret}", node.path("auth").path("secret").asText());
    }

    @Test
    void masksSamlSigningCertificate() {
        ObjectNode node = JsonSerialization.createObjectNode();
        node.put("signingCertificate", "PEM-CERT-DATA");

        JsonNode masked = StripSecretsUtilsV2.maskSecrets(node);

        assertEquals(SECRET_VALUE, masked.path("signingCertificate").asText());
        assertNotEquals("PEM-CERT-DATA", masked.path("signingCertificate").asText());
    }
}
