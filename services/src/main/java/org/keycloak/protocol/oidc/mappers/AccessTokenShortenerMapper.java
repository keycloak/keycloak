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

package org.keycloak.protocol.oidc.mappers;

import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.OIDCTokenChangeUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Mapper which is used to shorten an access token. The full token can still be retrieved with token introspection.
 *
 * @author <a href="mailto:daniel.fesenmeyer@bosch.io">Daniel Fesenmeyer</a>
 * @version $Revision: 1 $
 */
public class AccessTokenShortenerMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper {

    public static final String PROVIDER_ID = "oidc-shorten-token";

    private static final String INTROSPECTABLE_CLAIMS_PROPERTY_NAME = "introspectable-claims";
    private static final List<String> DEFAULT_INTROSPECTABLE_CLAIMS =
            Collections.unmodifiableList(Arrays.asList("aud", "realm_access.roles", "resource_access"));
    private static final String DEFAULT_INTROSPECTABLE_CLAIMS_STR = String.join(",", DEFAULT_INTROSPECTABLE_CLAIMS);

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = createConfigProperties();

    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public int getPriority() {
        return ProtocolMapperUtils.PRIORITY_ACCESS_TOKEN_SHORTENER;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Shorten access token";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Shorten the access token by removing configurable claims - full token can be retrieved with introspection.";
    }

    public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel,
            KeycloakSession keycloakSession,
            UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        boolean skip =
                keycloakSession.getAttributeOrDefault(Constants.SKIP_ACCESS_TOKEN_SHORTENER_SESSION_ATTRIBUTE, false);
        if (skip) {
            return token;
        }

        String claimsStr = mappingModel.getConfig().get(INTROSPECTABLE_CLAIMS_PROPERTY_NAME);
        Set<String> claims = stringToClaims(claimsStr);

        if (claims.isEmpty()) {
            return token;
        }

        ObjectNode accessTokenObject;
        try {
            accessTokenObject = JsonSerialization.createObjectNode(token);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        Set<String> removedClaims = OIDCTokenChangeUtils.removeClaims(accessTokenObject, claims);

        if (removedClaims.isEmpty()) {
            return token;
        } else {
            AccessToken resultToken;
            try {
                resultToken = JsonSerialization.mapper.readerFor(AccessToken.class).readValue(accessTokenObject);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            resultToken.setOtherClaims(Constants.INTROSPECTABLE_CLAIMS_CLAIM_NAME, removedClaims);

            return resultToken;
        }
    }

    private static List<ProviderConfigProperty> createConfigProperties() {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(INTROSPECTABLE_CLAIMS_PROPERTY_NAME);
        // TODO@dfr i18n
        property.setLabel("Introspectable claims");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        // TODO@dfr i18n
        property.setHelpText(
                "Comma-separated list of Claims that will be removed from the access token, but can be retrieved via Token Introspection. Claims can be fully qualified names like 'address.street'. To prevent nesting and use dot literally, escape the dot with backslash (\\.).");
        property.setDefaultValue(DEFAULT_INTROSPECTABLE_CLAIMS_STR);

        return Collections.singletonList(property);
    }

    public static ProtocolMapperModel create(final String name) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> config =
                Collections.singletonMap(INTROSPECTABLE_CLAIMS_PROPERTY_NAME, DEFAULT_INTROSPECTABLE_CLAIMS_STR);
        mapper.setConfig(config);
        return mapper;
    }

    private static Set<String> stringToClaims(final String claimsStr) {
        if (claimsStr == null) {
            return Collections.emptySet();
        }

        return Stream.of(claimsStr.split(",")).collect(Collectors.toSet());
    }

}
