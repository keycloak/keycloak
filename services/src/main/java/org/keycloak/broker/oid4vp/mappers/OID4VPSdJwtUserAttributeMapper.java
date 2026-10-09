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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.utils.StringUtil;

/**
 * Maps a claim of the presented SD JWT credential to a user property or attribute. A single mapper
 * covers the credential value types: string, number and boolean claims become a single text value,
 * arrays become a multivalued attribute, and object values are imported as their JSON
 * representation.
 */
public class OID4VPSdJwtUserAttributeMapper extends AbstractOID4VPClaimMapper {

    public static final String PROVIDER_ID = "oid4vp-sd-jwt-user-attribute-idp-mapper";

    public static final String USER_ATTRIBUTE = "user.attribute";

    public static final String USERNAME = "username";
    public static final String EMAIL = "email";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
            .property(claimProperty())
            .property()
                .name(USER_ATTRIBUTE)
                .label("User Attribute Name")
                .helpText("User attribute name to store the claim. Use username, email, firstName and lastName "
                        + "to map to those predefined user properties.")
                .type(ProviderConfigProperty.USER_PROFILE_ATTRIBUTE_LIST_TYPE)
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
        return "Attribute Importer";
    }

    @Override
    public String getDisplayType() {
        return "SD-JWT Attribute Importer";
    }

    @Override
    public String getHelpText() {
        return "Import the configured claim of the presented SD-JWT credential into the specified user property or "
                + "attribute. String, number and boolean claims are imported as a single value, arrays as a "
                + "multivalued attribute, and object values as their JSON representation.";
    }

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm,
            IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String attribute = attribute(mapperModel);
        if (attribute == null) {
            return;
        }
        List<String> values = claimValues(mapperModel, context);
        switch (attribute) {
            case USERNAME -> setIfPresent(values, context::setModelUsername);
            case EMAIL -> setIfPresent(values, context::setEmail);
            case FIRST_NAME -> setIfPresent(values, context::setFirstName);
            case LAST_NAME -> setIfPresent(values, context::setLastName);
            default -> context.setUserAttribute(attribute, values == null ? new ArrayList<>() : values);
        }
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user,
            IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String attribute = attribute(mapperModel);
        if (attribute == null) {
            return;
        }
        List<String> values = claimValues(mapperModel, context);
        switch (attribute) {
            case USERNAME -> setIfPresent(values, user::setUsername);
            case EMAIL -> setIfPresent(values, user::setEmail);
            case FIRST_NAME -> setIfPresent(values, user::setFirstName);
            case LAST_NAME -> setIfPresent(values, user::setLastName);
            default -> {
                if (values == null || values.isEmpty()) {
                    user.removeAttribute(attribute);
                } else if (!CollectionUtil.collectionEquals(values, user.getAttributeStream(attribute).toList())) {
                    user.setAttribute(attribute, values);
                }
            }
        }
    }

    protected void setIfPresent(List<String> values, Consumer<String> setter) {
        if (values != null && !values.isEmpty() && StringUtil.isNotBlank(values.get(0))) {
            setter.accept(values.get(0));
        }
    }

    protected String attribute(IdentityProviderMapperModel mapperModel) {
        String attribute = mapperModel.getConfig().get(USER_ATTRIBUTE);
        if (StringUtil.isBlank(attribute)) {
            logger.warnf("No user attribute configured for mapper %s", mapperModel.getName());
            return null;
        }
        return attribute.trim();
    }
}
