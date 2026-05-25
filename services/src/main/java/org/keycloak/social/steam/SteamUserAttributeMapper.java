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

import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.List;

/**
 * User attribute mapper for Steam to split profile realname fields.
 *
 * @author André Rocha
 * @author João Viegas
 */
public class SteamUserAttributeMapper extends AbstractIdentityProviderMapper {

    private static final String[] COMPATIBLE_PROVIDERS = new String[] { SteamIdentityProviderFactory.PROVIDER_ID };
    public static final String PROVIDER_ID = "steam-name-splitter-mapper";

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayCategory() {
        return "Preprocessing Importer";
    }

    @Override
    public String getDisplayType() {
        return "Steam Name Splitter";
    }

    @Override
    public String getHelpText() {
        return "Automatically splits Steam's 'realname' field into dedicated First Name and Last Name fields in Keycloak.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, org.keycloak.models.RealmModel realm, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        JsonNode profile = (JsonNode) context.getContextData().get(AbstractJsonUserAttributeMapper.CONTEXT_JSON_NODE);

        if (profile != null && profile.has("realname")) {
            String fullName = profile.get("realname").asText().trim();
            if (!fullName.isEmpty()) {
                String[] names = splitName(fullName);
                context.setFirstName(names[0]);
                context.setLastName(names[1]);
            }
        }
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, org.keycloak.models.RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        JsonNode profile = (JsonNode) context.getContextData().get(AbstractJsonUserAttributeMapper.CONTEXT_JSON_NODE);

        if (profile != null && profile.has("realname")) {
            String fullName = profile.get("realname").asText().trim();
            if (!fullName.isEmpty()) {
                String[] names = splitName(fullName);
                if (user.getFirstName() == null || user.getFirstName().isEmpty()) {
                    user.setFirstName(names[0]);
                }
                if (user.getLastName() == null || user.getLastName().isEmpty()) {
                    user.setLastName(names[1]);
                }
            }
        }
    }

    private String[] splitName(String fullName) {
        int firstSpace = fullName.indexOf(" ");
        if (firstSpace != -1) {
            return new String[] { fullName.substring(0, firstSpace).trim(), fullName.substring(firstSpace + 1).trim() };
        }
        return new String[] { fullName, "" };
    }
}
