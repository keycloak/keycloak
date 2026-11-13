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
package org.keycloak.social.paypal;

import java.util.Arrays;

import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;

/**
 * @author Petter Lysne (petterlysne at hotmail dot com)
 */
public class PayPalIdentityProviderConfig extends OAuth2IdentityProviderConfig {

    public PayPalIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    public PayPalIdentityProviderConfig() {
        
    }

    public boolean targetSandbox() {
        String sandbox = getConfig().get("sandbox");
        return sandbox == null ? false : Boolean.valueOf(sandbox);
    }

    public void setSandbox(boolean sandbox) {
        getConfig().put("sandbox", String.valueOf(sandbox));
    }

    @Override
    protected String[] getClientSecretDestinationConfigKeys() {
        String[] base = super.getClientSecretDestinationConfigKeys();
        String[] keys = Arrays.copyOf(base, base.length + 1);
        keys[base.length] = "sandbox";
        return keys;
    }

    /**
     * {@code sandbox} is a boolean; an absent value is equivalent to {@code "false"}, which is what
     * the admin console submits for existing providers created without the key. Parsing mirrors
     * {@link #targetSandbox()} so the comparison matches the effective token host.
     */
    @Override
    protected boolean isClientSecretDestinationChanged(String key, String stored, String updated) {
        if ("sandbox".equals(key)) {
            return targetSandbox(stored) != targetSandbox(updated);
        }
        return super.isClientSecretDestinationChanged(key, stored, updated);
    }

    private static boolean targetSandbox(String value) {
        return value != null && Boolean.valueOf(value);
    }

}
