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
package org.keycloak.protocol.oidc.mappers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.ProcessingException;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.*;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.representations.IDToken;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.storage.attributes.AttributeStoreProvider;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.*;

/**
 * @author Ben Cresitello-Dittmar
 *
 * OIDC protocal mapper to add attrributes from instances of {@link AttributeStoreProvider} to claims in tokens.
 *
 * Attributes retrieved from the attribute store provider are cached for the duration of the user session and stored in
 * user session notes so multiple mappers configured for the same store will query the external data store once and cache
 * the results in the user session.
 */
public class AttributeStoreMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper, TokenIntrospectionTokenMapper {

    private static final Logger logger = Logger.getLogger(AttributeStoreMapper.class);

    private static final String PROVIDER_ID = "oidc-attribute-store-mapper";

    // JSON pointer (RFC-6901) used to extract data from the attribute store response
    public static final String CONFIG_ATTRIBUTE_POINTER = "pointer";
    // The attribute store provider to fetch attributes from
    public static final String CONFIG_ATTRIBUTE_STORE_PROVIDER = "provider";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Attribute Store";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Map a value from an external attribute store to a token claim.";
    }

    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> configProperties = ProviderConfigurationBuilder.create()
                .property().name(CONFIG_ATTRIBUTE_POINTER)
                .label("JSON Pointer Expression")
                .type(ProviderConfigProperty.STRING_TYPE)
                .helpText("The JSON pointer expression used to extract attributes from the attribute store response (https://datatracker.ietf.org/doc/html/rfc6901)")
                .add()
                .property().name(CONFIG_ATTRIBUTE_STORE_PROVIDER)
                .type(ProviderConfigProperty.PROVIDER_INSTANCE_TYPE)
                .options(Collections.singletonList("type/" + AttributeStoreProvider.class.getName()))
                .label("Attribute Store Provider")
                .helpText("The external attribute store to get attributes from")
                .required(true)
                .add()
                .build();

        OIDCAttributeMapperHelper.addAttributeConfig(configProperties, AttributeStoreMapper.class);
        return configProperties;
    }

    @Override
    public void validateConfig(KeycloakSession session, RealmModel realm, ProtocolMapperContainerModel client, ProtocolMapperModel mapper) throws ProtocolMapperConfigException {
        // ensure provider id is set
        String providerId = mapper.getConfig().get(CONFIG_ATTRIBUTE_STORE_PROVIDER);
        if (providerId == null || providerId.isEmpty()){
            throw new ProtocolMapperConfigException(String.format("%s cannot be empty", CONFIG_ATTRIBUTE_POINTER));
        }

        // ensure specified provider exists
        AttributeStoreProvider provider = getStoreProvider(session, providerId);
        if (provider == null){
            throw new ProtocolMapperConfigException(String.format("attribute store provider %s could not be found", providerId));
        }
    }

    protected void setClaim(IDToken token, ProtocolMapperModel mapper, UserSessionModel userSession, KeycloakSession keycloakSession, ClientSessionContext clientSessionCtx) {
        String pointerExpr = mapper.getConfig().get(CONFIG_ATTRIBUTE_POINTER);

        Map<String, Object> attributes;
        try {
            // load attributes from store
            logger.debugf("attempting to load attributes from %s", mapper.getConfig().get(CONFIG_ATTRIBUTE_STORE_PROVIDER));
            attributes = getAttributes(keycloakSession, userSession, mapper);
            logger.debugf("loaded attributes for user %s: %s", userSession.getUser().getUsername(), attributes);

            // extract value using pointer expression
            JsonNode parsed = new ObjectMapper().convertValue(attributes, JsonNode.class);
            JsonNode claimValue = parsed.at(pointerExpr);

            // map claim as string unless it is a nested type (ie array or object). prevents extraneous quote for non-nested values
            if (claimValue.isValueNode()){
                OIDCAttributeMapperHelper.mapClaim(token, mapper, claimValue.asText());
            } else {
                OIDCAttributeMapperHelper.mapClaim(token, mapper, claimValue);
            }
        } catch (ProcessingException | ParsingException e){
            logger.warnf("failed to load attributes from provider %s for user %s: %s", mapper.getId(), userSession.getUser().getUsername(), e);
        }
    }

    /**
     * Helper function to fetch cached attributes from user session notes, or query new attributes from the configured attribute
     * store if no cached value is found.
     * @param session The keycloak sesion
     * @param userSession The current user session
     * @param mapper The protocol mapper model
     * @return The JSON-like attributes from the external data store
     * @throws ProcessingException Thrown when attributes cannot be loaded from the external store
     * @throws ParsingException Thrown when the retrieved attributes cannot be parsed as a JSON map
     */
    private Map<String, Object> getAttributes(KeycloakSession session, UserSessionModel userSession, ProtocolMapperModel mapper) throws ProcessingException, ParsingException {
        // get cached attributes from session note or load from store if not cached yet
        String attributes = userSession.getNote(getNoteKey(mapper));
        if (attributes == null){
            attributes = loadAttributes(session, userSession, mapper);
        }

        // parse cached attributes
        Map<String, Object> parsedAttributes;
        try {
            parsedAttributes = JsonSerialization.readValue(attributes, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e){
            throw new ParsingException(String.format("failed to deserialize attributes (%s): %s", attributes, e));
        }

        return parsedAttributes;
    }

    /**
     * Helper function to get the user session note key that will be used to cache the retrieved attributes from the configured
     * attribute store.
     * @param mapper
     * @return
     */
    private String getNoteKey(ProtocolMapperModel mapper){
        return String.format("%s-%s", PROVIDER_ID, mapper.getConfig().get(CONFIG_ATTRIBUTE_STORE_PROVIDER));
    }

    /**
     * Load attributes from the external data store and cache them in a user session note
     * @param keycloakSession The keycloak session
     * @param userSession The user session
     * @param mapper The model for the attribute mapper instance
     * @return The serialized attributes that are cached in the user session note
     * @throws ProcessingException Thrown when attributes cannot be loaded from the external store
     */
    private String loadAttributes(KeycloakSession keycloakSession, UserSessionModel userSession, ProtocolMapperModel mapper) throws ProcessingException {
        // get the configured provider instance
        AttributeStoreProvider provider = getStoreProvider(keycloakSession, mapper.getConfig().get(CONFIG_ATTRIBUTE_STORE_PROVIDER));
        if (provider == null){
            throw new ProcessingException(String.format("could not find provider %s", mapper.getConfig().get(CONFIG_ATTRIBUTE_STORE_PROVIDER)));
        }

        // load and serialize the attributes from the external store
        Map<String, Object> attributes;
        String serializedAttributes;
        try {
            attributes = provider.getAttributes(keycloakSession, keycloakSession.getContext().getRealm(), userSession.getUser());
            serializedAttributes = JsonSerialization.writeValueAsString(attributes);
        } catch (ProcessingException | IOException e){
            throw new ProcessingException(String.format("failed to load attributes: %s", e));
        }

        // cache attributes for other mappers to use in the same user session
        userSession.setNote(getNoteKey(mapper), serializedAttributes);
        logger.debugf("cached attributes for user %s: %s", userSession.getUser().getUsername(), serializedAttributes);

        return serializedAttributes;
    }

    /**
     * Helper function to get an {@link AttributeStoreProvider} instance from the specified provider component ID.
     * @param session The keycloak session
     * @param providerId The ID of the AttributeStoreProvider component instance
     * @return The initialized attribute store provider or null if the specified provider instance cannot be found
     */
    private AttributeStoreProvider getStoreProvider(KeycloakSession session, String providerId){
        // get the specified provider component
        ComponentModel providerComp = session.getContext().getRealm().getComponent(providerId);
        if (providerComp == null){
            logger.warnf("failed to find component %s", providerId);
            return null;
        }

        // get an instance of the configured attribute store provider
        AttributeStoreProvider provider = session.getProvider(AttributeStoreProvider.class, providerComp);
        if (provider == null){
            logger.warnf("failed to find component provider %s", providerId);
            return null;
        }

        return provider;
    }
}
