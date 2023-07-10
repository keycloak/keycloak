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

package org.keycloak.models;

import org.junit.Test;
import org.keycloak.models.utils.StripSecretsUtils;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class StripSecretsUtilsTest {

    @Test
    public void checkStrippedRotatedSecret() {
        ClientRepresentation stripped = StripSecretsUtils.stripSecrets(null, createClient("unmasked_secret"));
        assertEquals(ComponentRepresentation.SECRET_VALUE, getRotatedSecret(stripped));
    }

    @Test
    public void checkStrippedRotatedSecretVaultUnaffected() {
        String rotatedSecret = "${vault.key}";
        ClientRepresentation stripped = StripSecretsUtils.stripSecrets(null, createClient(rotatedSecret));
        assertEquals(rotatedSecret, getRotatedSecret(stripped));
    }

    private ClientRepresentation createClient(String rotatedSecret) {
        ClientRepresentation client = new ClientRepresentation();
        Map<String, String> attrs = new HashMap<>();
        attrs.put(ClientSecretConstants.CLIENT_ROTATED_SECRET, rotatedSecret);
        client.setAttributes(attrs);
        return client;
    }

    private String getRotatedSecret(ClientRepresentation clientRepresentation) {
        return clientRepresentation.getAttributes().get(ClientSecretConstants.CLIENT_ROTATED_SECRET);
    }

}
