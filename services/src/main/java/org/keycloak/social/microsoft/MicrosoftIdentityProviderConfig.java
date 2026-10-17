/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.social.microsoft;

import java.util.Arrays;

import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;

public class MicrosoftIdentityProviderConfig extends OIDCIdentityProviderConfig {

    public MicrosoftIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    public MicrosoftIdentityProviderConfig() {

    }

    public String getTenantId() {
        String tenantId = getConfig().get("tenantId");

        return tenantId == null || tenantId.isEmpty() ? null : tenantId;
    }

    public void setTenantId(final String tenantId) {
        getConfig().put("tenantId", tenantId);
    }

    @Override
    protected String[] getClientSecretDestinationConfigKeys() {
        String[] base = super.getClientSecretDestinationConfigKeys();
        String[] keys = Arrays.copyOf(base, base.length + 1);
        keys[base.length] = "tenantId";
        return keys;
    }

    /**
     * An absent {@code tenantId} resolves to the multi-tenant {@code common} endpoint at runtime,
     * so treat the two as equivalent when deciding whether the token destination changed. The
     * derivation mirrors {@code MicrosoftIdentityProvider}: only {@code null}/empty fall back to
     * {@code common}; any other value is trimmed.
     */
    @Override
    protected boolean isClientSecretDestinationChanged(String key, String stored, String updated) {
        if ("tenantId".equals(key)) {
            return !tenant(stored).equals(tenant(updated));
        }
        return super.isClientSecretDestinationChanged(key, stored, updated);
    }

    private static String tenant(String value) {
        return value == null || value.isEmpty() ? "common" : value.trim();
    }
}
