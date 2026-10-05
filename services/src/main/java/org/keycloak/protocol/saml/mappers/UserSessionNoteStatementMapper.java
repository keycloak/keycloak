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

package org.keycloak.protocol.saml.mappers;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperContainerModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.provider.ProviderConfigProperty;

import org.jboss.logging.Logger;

/**
 * Maps a user session note to a SAML attribute
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserSessionNoteStatementMapper extends AbstractSAMLProtocolMapper implements SAMLAttributeStatementMapper {
    private static final Logger logger = Logger.getLogger(UserSessionNoteStatementMapper.class);

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    public static final String NOTE_CONFIG_KEY = "note";

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(NOTE_CONFIG_KEY);
        property.setLabel("User Session Note Attribute");
        property.setHelpText("The user session note you want to grab the value from.");
        configProperties.add(property);
        AttributeStatementHelper.setConfigProperties(configProperties);

    }

    public static final String PROVIDER_ID = "saml-user-session-note-mapper";


    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }
    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "User Session Note";
    }

    @Override
    public String getDisplayCategory() {
        return AttributeStatementHelper.ATTRIBUTE_STATEMENT_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Map a user session note to a SAML attribute.";
    }

    @Override
    public void transformAttributeStatement(AttributeStatementType attributeStatement, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, AuthenticatedClientSessionModel clientSession) {
        String note = mappingModel.getConfig().get(NOTE_CONFIG_KEY);
        if (ProtocolMapperUtils.isBrokerCredentialNote(note)) {
            logger.warnf("Protocol mapper '%s' on client '%s' maps the broker credential session note '%s'. The attribute is omitted, remove the mapper.",
                    mappingModel.getName(), clientSession.getClient().getClientId(), note);
            return;
        }
        String value = userSession.getNote(note);
        if (value == null) return;
        AttributeStatementHelper.addAttribute(attributeStatement, mappingModel, value);

    }

    @Override
    public void validateConfig(KeycloakSession session, RealmModel realm, ProtocolMapperContainerModel client, ProtocolMapperModel mapperModel) throws ProtocolMapperConfigException {
        String note = mapperModel.getConfig().get(NOTE_CONFIG_KEY);
        if (ProtocolMapperUtils.isBrokerCredentialNote(note)) {
            throw new ProtocolMapperConfigException(
                    "Session note '" + note + "' is a Keycloak-internal broker credential and cannot be mapped",
                    "protocolMapperBrokerCredentialNote");
        }
    }
}
