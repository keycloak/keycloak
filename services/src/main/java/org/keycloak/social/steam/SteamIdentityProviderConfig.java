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
package org.keycloak.social.steam;

import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;

/**
 * Configuration capabilities for the Steam Identity Provider.
 *
 * @author André Rocha
 * @author João Viegas
 */
public class SteamIdentityProviderConfig extends OAuth2IdentityProviderConfig {

    public SteamIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    public SteamIdentityProviderConfig() {
        super();
    }

    public String getSteamApiKey() {
        return getConfig().get("steamApiKey");
    }

    public void setSteamApiKey(String steamApiKey) {
        getConfig().put("steamApiKey", steamApiKey);
    }

    public boolean isRejectVACBanned() {
        return Boolean.parseBoolean(getConfig().get("rejectVACBanned"));
    }

    public boolean isRejectCommunityBanned() {
        return Boolean.parseBoolean(getConfig().get("rejectCommunityBanned"));
    }

    public int getMinDaysSinceLastBan() {
        String val = getConfig().get("minDaysSinceLastBan");
        try {
            return (val == null || val.trim().isEmpty()) ? 0 : Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return 0; // Default fallback to safe setting
        }
    }

    public int getMinSteamLevel() {
        String val = getConfig().get("minSteamLevel");
        try {
            return (val == null || val.trim().isEmpty()) ? 0 : Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return 0; // Default fallback to safe setting
        }
    }

    public boolean isFailClosedOnApiError() {
        String val = getConfig().get("failClosedOnApiError");
        return val != null ? Boolean.parseBoolean(val) : true; // Default to Fail Closed for security
    }

    public void setFailClosedOnApiError(boolean failClosedOnApiError) {
        getConfig().put("failClosedOnApiError", String.valueOf(failClosedOnApiError));
    }
}
