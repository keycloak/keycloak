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

package org.keycloak.broker.oidc.mappers;

import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.saml.common.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserAttributeMapper extends AbstractClaimMapper {

    public static final String[] COMPATIBLE_PROVIDERS = {KeycloakOIDCIdentityProviderFactory.PROVIDER_ID, OIDCIdentityProviderFactory.PROVIDER_ID};

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    public static final String USER_ATTRIBUTE = "user.attribute";
    public static final String ATTRIBUTE_PREFFIX = "attribute.prefix";
    public static final String EMAIL = "email";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES = new HashSet<>(Arrays.asList(IdentityProviderSyncMode.values()));

    static {
        ProviderConfigProperty property;
        ProviderConfigProperty property1;
        ProviderConfigProperty property2;

        property1 = new ProviderConfigProperty();
        property1.setName(CLAIM);
        property1.setLabel("Claim");
        property1.setHelpText("Name of claim to search for in token. You can reference nested claims using a '.', i.e. 'address.locality'. To use dot (.) literally, escape it with backslash (\\.)");
        property1.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property1);

        property = new ProviderConfigProperty();
        property.setName(USER_ATTRIBUTE);
        property.setLabel("User Attribute Name");
        property.setHelpText("User attribute name to store claim.  Use email, lastName, and firstName to map to those predefined user properties.");
        property.setType(ProviderConfigProperty.USER_PROFILE_ATTRIBUTE_LIST_TYPE);
        configProperties.add(property);

        property2 = new ProviderConfigProperty();
        property2.setName(ATTRIBUTE_PREFFIX);
        property2.setLabel("Attribute Value Prefix");
        property2.setHelpText(
                "Optional prefix to be concatenated in front of every imported attribute value.");
        property2.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property2);
    }

    public static final String PROVIDER_ID = "oidc-user-attribute-idp-mapper";

    private static final Logger LOG = Logger.getLogger(UserAttributeMapper.class);

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return IDENTITY_PROVIDER_SYNC_MODES.contains(syncMode);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayCategory() {
        return "Attribute Importer";
    }

    @Override
    public String getDisplayType() {
        return "Attribute Importer";
    }

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        LOG.debug("executing preprocessFederatedIdentity()");
        
        String attribute = mapperModel.getConfig().get(USER_ATTRIBUTE);
        String prefix = Objects.toString(mapperModel.getConfig().get(ATTRIBUTE_PREFFIX), "");

        if (StringUtil.isNullOrEmpty(attribute)) {
            LOG.debug("Empty attribute, not processing.");
            return;
        }

        Object value = getClaimValue(mapperModel, context);
        List<String> values;

        if (StringUtil.isNullOrEmpty(prefix)) {
            LOG.debug("No attribute prefix configured.");
            values = toList(value);
        }
        else {
            LOG.debug("Retrieved prefix: " + prefix);
            values = toPrefixedList(value, prefix);
        } 

        if (EMAIL.equalsIgnoreCase(attribute)) {
            setIfNotEmpty(context::setEmail, values);
        } else if (FIRST_NAME.equalsIgnoreCase(attribute)) {
            setIfNotEmpty(context::setFirstName, values);
        } else if (LAST_NAME.equalsIgnoreCase(attribute)) {
            setIfNotEmpty(context::setLastName, values);
        } else {
            List<String> valuesToString = values.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.toList());

            context.setUserAttribute(attribute, valuesToString);
        }
    }

    private void setIfNotEmpty(Consumer<String> consumer, List<String> values) {
        if (values != null && !values.isEmpty()) {
            consumer.accept(values.get(0));
        }
    }

    private List<String> toList(Object value) {
        List<Object> values = (value instanceof List)
                ? (List) value
                : Collections.singletonList(value);
        return values.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    private List<String> toPrefixedList(Object value, String prefix) {
        List<Object> values = (value instanceof List)
            ? (List) value
            : Collections.singletonList(value);

        return values.stream()
            .filter(Objects::nonNull)
            .map(item -> prefix.concat(item.toString()))
            .collect(Collectors.toList());
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        LOG.debug("executing updateBrokeredUser()");

        String attribute = mapperModel.getConfig().get(USER_ATTRIBUTE);
        String prefix = Objects.toString(mapperModel.getConfig().get(ATTRIBUTE_PREFFIX), "");

        if (StringUtil.isNullOrEmpty(attribute)) {
            LOG.debug("Empty attribute, not processing.");
            return;
        }

        Object value = getClaimValue(mapperModel, context);
        List<String> values;

        if (StringUtil.isNullOrEmpty(prefix)) {
            LOG.debug("No attribute prefix configured.");
            values = toList(value);
        }
        else {
            LOG.debug("Retrieved prefix: " + prefix);
            values = toPrefixedList(value, prefix);
        } 

        if (EMAIL.equalsIgnoreCase(attribute)) {
            setIfNotEmpty(user::setEmail, values);
        } else if (FIRST_NAME.equalsIgnoreCase(attribute)) {
            setIfNotEmpty(user::setFirstName, values);
        } else if (LAST_NAME.equalsIgnoreCase(attribute)) {
            setIfNotEmpty(user::setLastName, values);
        } else {
            List<String> current = user.getAttributeStream(attribute).collect(Collectors.toList());
            if (!CollectionUtil.collectionEquals(values, current)) {
                user.setAttribute(attribute, values);
            } else if (values.isEmpty()) {
                user.removeAttribute(attribute);
            }
        }
    }

    @Override
    public String getHelpText() {
        return "Import declared claim if it exists in ID, access token or the claim set returned by the user profile endpoint into the specified user property or attribute and optionally prefix each attribute value with the provided prefix.";
    }
}
