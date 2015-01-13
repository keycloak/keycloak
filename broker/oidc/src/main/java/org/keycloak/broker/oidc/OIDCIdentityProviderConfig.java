/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.broker.oidc;

import java.util.Map;

/**
 * @author Pedro Igor
 */
public class OIDCIdentityProviderConfig extends OAuth2IdentityProviderConfig {

    public OIDCIdentityProviderConfig(String providerId, String id, String name, Map<String, String> config) {
        super(providerId, id, name, config);
    }

    public String getPrompt() {
        String prompt = getConfig().get("prompt");

        if (prompt == null || "".equals(prompt)) {
            return "none";
        }

        return prompt;
    }

    @Override
    public String getDefaultScope() {
        String scope = super.getDefaultScope();

        if (scope == null || "".equals(scope)) {
            scope = "openid";
        }

        return scope;
    }

    public String getIssuer() {
        return getConfig().get("issuer");
    }

}
