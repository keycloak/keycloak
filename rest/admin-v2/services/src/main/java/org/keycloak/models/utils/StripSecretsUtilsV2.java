/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.utils;

import org.keycloak.utils.StringUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Masks secrets in Admin API v2 representations for admin events.
 * <p>
 * Operates on a {@link JsonNode} tree so the original representation object is left unchanged
 * and can still be returned in the HTTP response with secrets intact.
 */
public class StripSecretsUtilsV2 extends StripSecretsUtils {

    private StripSecretsUtilsV2() {
    }

    /**
     * Masks known secret fields on the given JSON tree in place.
     *
     * @param node representation JSON (typically from {@code JsonSerialization.writeValueAsNode})
     * @return the same node instance after masking
     */
    public static JsonNode maskSecrets(JsonNode node) {
        if (node == null || !node.isObject()) {
            return node;
        }

        ObjectNode object = (ObjectNode) node;

        // OIDC: auth.secret
        JsonNode auth = object.get("auth");
        if (auth != null && auth.isObject()) {
            ObjectNode authObject = (ObjectNode) auth;
            JsonNode secret = authObject.get("secret");
            if (secret != null && secret.isTextual() && StringUtil.isNotBlank(secret.asText())) {
                authObject.put("secret", maskNonVaultValue(secret.asText()));
            }
        }

        // SAML: signingCertificate
        JsonNode signingCertificate = object.get("signingCertificate");
        if (signingCertificate != null && signingCertificate.isTextual()
                && StringUtil.isNotBlank(signingCertificate.asText())) {
            object.put("signingCertificate", maskNonVaultValue(signingCertificate.asText()));
        }

        return object;
    }
}
