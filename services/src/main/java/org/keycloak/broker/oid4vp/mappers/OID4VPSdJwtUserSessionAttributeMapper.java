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

package org.keycloak.broker.oid4vp.mappers;

import java.util.List;

import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.utils.StringUtil;

/**
 * Imports a claim of the presented SD JWT credential into a user session attribute, so it can be
 * surfaced in tokens through the user session note protocol mappers. Session notes hold one string
 * value: multivalued selections are joined with a comma and object values are stored as their JSON
 * representation.
 */
public class OID4VPSdJwtUserSessionAttributeMapper extends AbstractOID4VPClaimMapper {

    public static final String PROVIDER_ID = "oid4vp-sd-jwt-user-session-attribute-idp-mapper";

    public static final String ATTRIBUTE = "attribute";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
            .property(claimProperty())
            .property()
                .name(ATTRIBUTE)
                .label("User Session Attribute")
                .helpText("Name of the user session attribute to store the claim in.")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
            .build();

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public String getDisplayCategory() {
        return "User Session";
    }

    @Override
    public String getDisplayType() {
        return "SD-JWT User Session Attribute Importer";
    }

    @Override
    public String getHelpText() {
        return "Import the configured claim of the presented SD-JWT credential into the specified user session "
                + "attribute. Use together with the 'User Session Note' protocol mapper on your client or client "
                + "scope to make the claim available in tokens.";
    }

    /**
     * The note is per login state, so it is also set during preprocessing, which runs for every
     * login. The user centric hooks below never run for an existing user whose effective sync
     * mode is IMPORT.
     */
    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm,
            IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        addSessionNote(mapperModel, context);
    }

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user,
            IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        addSessionNote(mapperModel, context);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user,
            IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        addSessionNote(mapperModel, context);
    }

    protected void addSessionNote(IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String attribute = mapperModel.getConfig().get(ATTRIBUTE);
        if (StringUtil.isBlank(attribute)) {
            logger.warnf("No user session attribute configured for mapper %s", mapperModel.getName());
            return;
        }
        List<String> values = claimValues(mapperModel, context);
        if (values == null || values.isEmpty()) {
            return;
        }
        context.setSessionNote(attribute.trim(), String.join(",", values));
    }
}
